package com.petruvideo.core.media.preview

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.SpeedParameters
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.SpeedProvider
import androidx.media3.common.Player
import androidx.annotation.OptIn
import androidx.media3.common.util.ExperimentalApi
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.CompositionPlayer
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import com.petruvideo.core.media.pressure.PreviewQuality
import com.petruvideo.core.model.Availability
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.Reverb
import com.petruvideo.core.model.Timeline
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Preview through Media3's [CompositionPlayer] (research 1): the video lane is one sequence (photos as timed
 * images, attached sound kept or removed per clip), the audio lane is a second sequence whose gaps are silent.
 * Missing clips and muted audio clips become gaps, so the timeline keeps its length and playback never fails.
 * All calls must come from the main thread, which is also the player's application thread.
 */
@OptIn(UnstableApi::class, ExperimentalApi::class)
@Singleton
class Media3PreviewEngine @Inject constructor(
    @ApplicationContext private val context: Context,
) : PreviewEngine {
    private val handler = Handler(Looper.getMainLooper())
    private val _state = MutableStateFlow(PreviewState())
    private var compositionPlayer: CompositionPlayer? = null
    private var totalMs = 0L
    private var hasComposition = false
    private var polling = false
    private var gains = mutableMapOf<String, GainAudioProcessor>()
    private var reverbs = mutableMapOf<String, ReverbAudioProcessor>()
    private var reverbAllowed = true

    override val state: StateFlow<PreviewState> = _state
    override val player: Player? get() = compositionPlayer

    private val poll = object : Runnable {
        override fun run() {
            publish()
            if (polling) handler.postDelayed(this, POLL_MS)
        }
    }

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            polling = isPlaying
            if (isPlaying) handler.post(poll) else publish()
        }

        override fun onPlaybackStateChanged(playbackState: Int) = publish()

        override fun onPlayerError(error: PlaybackException) {
            publish(error = "Playback stopped: ${error.errorCodeName}")
        }
    }

    private var playbackStats: PlaybackStats? = null

    private fun isDebuggable() =
        context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0

    override fun load(timeline: Timeline, quality: PreviewQuality) {
        totalMs = timeline.totalDurationMs
        val composition = compositionFor(timeline, quality)
        val player = compositionPlayer ?: CompositionPlayer.Builder(context).build()
            .also {
                it.addListener(listener)
                if (isDebuggable()) PlaybackStats().let { stats ->
                    playbackStats = stats
                    it.setVideoFrameMetadataListener(stats)
                    it.addAnalyticsListener(stats)
                }
                compositionPlayer = it
            }
        hasComposition = composition != null
        if (composition == null) {
            player.pause()
            publish()
            return
        }
        val resume = player.isPlaying
        val position = player.currentPosition.coerceIn(0, totalMs)
        player.setComposition(composition, position)
        player.prepare()
        if (resume) player.play()
        publish()
    }

    // With nothing to play (an empty project) the player has no composition and rejects play and seek.
    override fun play() {
        if (hasComposition) {
            playbackStats?.playRequested()
            compositionPlayer?.play()
        }
    }

    override fun pause() {
        compositionPlayer?.pause()
    }

    override fun seekTo(positionMs: Long) {
        if (hasComposition) compositionPlayer?.seekTo(positionMs.coerceIn(0, totalMs))
        publish()
    }

    override fun setClipReverbLive(clipId: String, reverb: Reverb) {
        reverbs[clipId]?.update(reverb.copy(enabled = reverb.enabled && reverbAllowed))
    }

    override fun setClipVolumeLive(clipId: String, percent: Int) {
        gains[clipId]?.setPercent(percent)
    }

    override fun release() {
        polling = false
        handler.removeCallbacks(poll)
        compositionPlayer?.release()
        compositionPlayer = null
        hasComposition = false
        _state.value = PreviewState()
    }

    private fun publish(error: String? = null) {
        val player = compositionPlayer
        _state.value = PreviewState(
            isPlaying = player?.isPlaying == true,
            positionMs = player?.currentPosition?.coerceIn(0, totalMs) ?: 0,
            durationMs = totalMs,
            ended = player?.playbackState == Player.STATE_ENDED,
            error = error ?: _state.value.error.takeIf { player?.playerError != null },
        )
    }

    private fun compositionFor(timeline: Timeline, quality: PreviewQuality): Composition? {
        gains = mutableMapOf()
        reverbs = mutableMapOf()
        reverbAllowed = quality.reverb
        val sequences = listOfNotNull(videoSequence(timeline, quality), audioSequence(timeline))
        return if (sequences.isEmpty()) null else Composition.Builder(sequences).build()
    }

    private fun videoSequence(timeline: Timeline, quality: PreviewQuality): EditedMediaItemSequence? {
        if (timeline.videoLane.isEmpty()) return null
        val builder = EditedMediaItemSequence.Builder(setOf(C.TRACK_TYPE_VIDEO, C.TRACK_TYPE_AUDIO))
        val fps = minOf(timeline.format?.fps ?: DEFAULT_FPS, quality.previewFps)
        val frame = frameEffects(timeline, quality)
        timeline.videoLane.forEach { clip ->
            val media = timeline.media[clip.mediaItemId]
            when {
                media == null || media.availability == Availability.Missing -> builder.addGap(clip.lengthMs * US_PER_MS)
                media.kind == MediaKind.Photo -> builder.addItem(
                    EditedMediaItem.Builder(MediaItem.fromUri(Uri.parse(media.uri)))
                        .setDurationUs(clip.lengthMs * US_PER_MS)
                        .setFrameRate(fps)
                        .setRemoveAudio(true)
                        .setEffects(frame)
                        .build(),
                )
                else -> builder.addItem(
                    EditedMediaItem.Builder(clipped(media.uri, clip.sourceStartMs, clip.sourceEndMs))
                        .setDurationUs(sourceDurationUs(media.durationMs, clip.sourceEndMs))
                        .setRemoveAudio(!clip.attachedAudio || clip.muted)
                        .setSpeed(speedOf(clip.speedMilli))
                        .setEffects(
                            Effects(
                                soundEffects(
                                    clip.id, clip.reverb, clip.volumePercent, clip.attachedAudio && !clip.muted,
                                ),
                                frame.videoEffects,
                            ),
                        )
                        .build(),
                )
            }
        }
        return builder.build()
    }

    private fun audioSequence(timeline: Timeline): EditedMediaItemSequence? {
        if (timeline.audioLane.isEmpty()) return null
        val builder = EditedMediaItemSequence.Builder(setOf(C.TRACK_TYPE_AUDIO))
        var cursor = 0L
        timeline.audioLane.sortedBy { it.timelineStartMs }.forEach { clip ->
            val gap = clip.timelineStartMs - cursor
            if (gap > 0) builder.addGap(gap * US_PER_MS)
            val media = timeline.media[clip.mediaItemId]
            if (clip.muted || media == null || media.availability == Availability.Missing) {
                builder.addGap(clip.lengthMs * US_PER_MS)
            } else {
                builder.addItem(
                    EditedMediaItem.Builder(clipped(media.uri, clip.sourceStartMs, clip.sourceEndMs))
                        .setDurationUs(sourceDurationUs(media.durationMs, clip.sourceEndMs))
                        .setRemoveVideo(true)
                        .setSpeed(speedOf(clip.speedMilli))
                        .setEffects(Effects(soundEffects(clip.id, clip.reverb, clip.volumePercent, true), emptyList()))
                        .build(),
                )
            }
            cursor = clip.timelineEndMs
        }
        return builder.build()
    }

    /** Reverb first, then volume, so volume 0% silences the tail too. */
    private fun soundEffects(clipId: String, reverb: Reverb, percent: Int, hasAudio: Boolean): List<AudioProcessor> {
        if (!hasAudio) return emptyList()
        val gain = GainAudioProcessor(percent).also { gains[clipId] = it }
        val effective = reverb.copy(enabled = reverb.enabled && reverbAllowed)
        val room = ReverbAudioProcessor(effective).also {
            it.update(effective, applyRoomSize = true)
            reverbs[clipId] = it
        }
        return listOf(room, gain)
    }

    /** A constant speed for the whole item with the pitch kept (research 1; experimental Media3 API). */
    private fun speedOf(speedMilli: Int): SpeedParameters {
        val speed = speedMilli / SPEED_DIVISOR
        val provider = object : SpeedProvider {
            override fun getSpeed(timeUs: Long) = speed
            override fun getNextSpeedChangeTimeUs(timeUs: Long) = C.TIME_UNSET
        }
        return SpeedParameters(provider, true)
    }

    /** Fits every clip inside the project frame (bars, never cropping or stretching); smaller under pressure. */
    private fun frameEffects(timeline: Timeline, quality: PreviewQuality): Effects {
        val format = timeline.format ?: return Effects.EMPTY
        val width = even((format.width * quality.previewScale).toInt())
        val height = even((format.height * quality.previewScale).toInt())
        val fit = Presentation.createForWidthAndHeight(width, height, Presentation.LAYOUT_SCALE_TO_FIT)
        return Effects(emptyList(), listOf(fit))
    }

    private fun even(value: Int) = maxOf(2, value - value % 2)

    /** CompositionPlayer needs each item's full source duration up front (it cannot prepare to find out). */
    private fun sourceDurationUs(durationMs: Long?, fallbackEndMs: Long) = (durationMs ?: fallbackEndMs) * US_PER_MS

    private fun clipped(uri: String, startMs: Long, endMs: Long): MediaItem =
        MediaItem.Builder()
            .setUri(Uri.parse(uri))
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(startMs)
                    .setEndPositionMs(endMs)
                    .build(),
            )
            .build()

    private companion object {
        const val POLL_MS = 33L
        const val DEFAULT_FPS = 30
        const val US_PER_MS = 1_000L
        const val SPEED_DIVISOR = 1000f
    }
}
