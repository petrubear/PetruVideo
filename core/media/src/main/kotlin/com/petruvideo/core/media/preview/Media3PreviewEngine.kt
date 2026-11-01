package com.petruvideo.core.media.preview

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.annotation.OptIn
import androidx.media3.common.util.ExperimentalApi
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.CompositionPlayer
import com.petruvideo.core.media.composition.BuildSpec
import com.petruvideo.core.media.composition.CompositionBuilder
import com.petruvideo.core.media.pressure.PreviewQuality
import com.petruvideo.core.model.AudioFx
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
    private var voiceFxs = mutableMapOf<String, VoiceFxProcessor>()
    private var voiceFxAllowed = true
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
        val builder = CompositionBuilder(BuildSpec.forPreview(timeline, quality))
        val composition = builder.build(timeline)
        gains = builder.gains
        reverbs = builder.reverbs
        voiceFxs = builder.voiceFxs
        voiceFxAllowed = quality.voiceFx
        reverbAllowed = quality.reverb
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

    override fun setClipFxLive(clipId: String, fx: AudioFx, clipLengthMs: Long) {
        voiceFxs[clipId]?.update(fx, voiceFxAllowed)
        gains[clipId]?.setFade(fx.fade, clipLengthMs)
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

    private companion object {
        const val POLL_MS = 33L
    }
}
