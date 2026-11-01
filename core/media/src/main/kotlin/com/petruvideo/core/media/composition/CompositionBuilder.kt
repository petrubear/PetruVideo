package com.petruvideo.core.media.composition

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.SpeedParameters
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.SpeedProvider
import androidx.media3.common.util.ExperimentalApi
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.FrameDropEffect
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import com.petruvideo.core.media.effects.ClipEffectHandle
import com.petruvideo.core.media.effects.ClipFramingHandle
import com.petruvideo.core.media.effects.transformEffects
import com.petruvideo.core.media.filter.ClipFilterEffects
import com.petruvideo.core.media.pressure.PreviewQuality
import com.petruvideo.core.media.preview.GainAudioProcessor
import com.petruvideo.core.media.preview.ReverbAudioProcessor
import com.petruvideo.core.media.preview.VoiceFxProcessor
import com.petruvideo.core.media.text.TextOverlaySet
import com.petruvideo.core.model.AudioFx
import com.petruvideo.core.model.DuckEnvelope
import com.petruvideo.core.model.Fade
import com.petruvideo.core.model.Availability
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.Reverb
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.VideoClip

/**
 * How a composition is sized and timed: output [width] and [height] (null keeps each clip's own size), the [fps]
 * photos are given, an optional [frameDropFps] that drops frames to a lower rate, and whether reverb is applied.
 */
data class BuildSpec(
    val width: Int?,
    val height: Int?,
    val fps: Int,
    val reverbAllowed: Boolean = true,
    val voiceFxAllowed: Boolean = true,
    val frameDropFps: Int? = null,
    val sourceFps: Int = 30,
    val toneMapHdr: Boolean = false,
) {
    companion object {
        private const val DEFAULT_FPS = 30

        private fun even(value: Int) = maxOf(2, value - value % 2)

        /** Preview: the project frame scaled by the quality, at most the quality's frame rate. */
        fun forPreview(timeline: Timeline, quality: PreviewQuality): BuildSpec {
            val format = timeline.format
            return BuildSpec(
                width = format?.let { even((it.width * quality.previewScale).toInt()) },
                height = format?.let { even((it.height * quality.previewScale).toInt()) },
                fps = minOf(format?.fps ?: DEFAULT_FPS, quality.previewFps),
                reverbAllowed = quality.reverb,
                voiceFxAllowed = quality.voiceFx,
            )
        }

        /**
         * Export: the project's aspect ratio with [shortSide] pixels on the short side, at [fps]; frames are dropped in
         * whole steps when [fps] is below the footage's highest rate [sourceFps] (it must divide it evenly).
         */
        fun forExport(timeline: Timeline, shortSide: Int, fps: Int, sourceFps: Int): BuildSpec {
            val format = timeline.format
            val short = minOf(format?.width ?: 1, format?.height ?: 1).coerceAtLeast(1)
            val scale = shortSide.toDouble() / short
            return BuildSpec(
                width = format?.let { even((it.width * scale).toInt()) },
                height = format?.let { even((it.height * scale).toInt()) },
                fps = fps,
                frameDropFps = fps.takeIf { it < sourceFps },
                sourceFps = sourceFps,
                toneMapHdr = true,
            )
        }
    }
}

/**
 * Builds the Media3 [Composition] for a timeline. The preview and the export both use it, so speed, volume, mute,
 * reverb, photos, gaps, and the fitting of clips to the frame are identical by construction. The processors created
 * for each clip are kept in [gains] and [reverbs] so the preview can change them live.
 */
@OptIn(UnstableApi::class, ExperimentalApi::class)
class CompositionBuilder(
    private val spec: BuildSpec,
    private val framingIds: Set<String> = emptySet(),
    private val duckEnvelopes: Map<String, DuckEnvelope> = emptyMap(),
) {
    val gains = mutableMapOf<String, GainAudioProcessor>()
    val reverbs = mutableMapOf<String, ReverbAudioProcessor>()
    val voiceFxs = mutableMapOf<String, VoiceFxProcessor>()

    /** The filter effects of each filtered clip after [build], so the preview can change them live. */
    val filters = mutableMapOf<String, ClipFilterEffects>()

    /** The picture effect of each clip that has one after [build], so the preview can change its strength live. */
    val clipEffects = mutableMapOf<String, ClipEffectHandle>()

    /** The framing (zoom, pan, fade) of each clip with keyframes after [build], so the preview can change it live. */
    val framings = mutableMapOf<String, ClipFramingHandle>()

    /** The placement of the overlay clips after [build], so the preview can change one live; null without overlays. */
    var overlayLayout: OverlayLayout? = null
        private set

    /** The text lane's overlays after [build], so the preview can change a clip live; null without text clips. */
    var textOverlays: TextOverlaySet? = null
        private set

    /** The composition for [timeline], or null when there is nothing to play or render. */
    fun build(timeline: Timeline): Composition? {
        val overlays = overlaySequences(timeline)
        val transitions = hasTransitions(timeline)
        val main = if (transitions) transitionSequences(timeline) else listOfNotNull(videoSequence(timeline))
        val sequences = overlays + main + listOfNotNull(audioSequence(timeline))
        if (sequences.isEmpty()) return null
        val builder = Composition.Builder(sequences)
        textEffects(timeline)?.let { builder.setEffects(it) }
        if (usesMultipleInputs(timeline)) {
            val width = spec.width ?: timeline.format?.width ?: DEFAULT_WIDTH
            val height = spec.height ?: timeline.format?.height ?: DEFAULT_HEIGHT
            val plan = if (transitions) TransitionPlan.of(timeline, overlays.size) else null
            builder.setVideoCompositorSettings(
                OverlayLayout(timeline.overlayLane, width, height, plan).also { overlayLayout = it },
            )
        }
        if (spec.toneMapHdr) builder.setHdrMode(Composition.HDR_MODE_TONE_MAP_HDR_TO_SDR_USING_MEDIACODEC)
        return builder.build()
    }

    /**
     * Clips alternate between two sequences so a transition can show the end of one and the start of the next at the
     * same time; [OverlayLayout] blends them. Each sequence runs on to the end of the project (an ended input would
     * keep its last frame).
     */
    private fun transitionSequences(timeline: Timeline): List<EditedMediaItemSequence> {
        val frame = frameEffects()
        val builders = (0..1).map { parity ->
            val clips = timeline.videoLane.filterIndexed { i, _ -> i % 2 == parity }
            EditedMediaItemSequence.Builder(videoTrackTypes(timeline, clips))
        }
        val cursors = longArrayOf(0, 0)
        var start = 0L
        timeline.videoLane.forEachIndexed { i, clip ->
            val parity = i % 2
            val gap = start - cursors[parity]
            if (gap > 0) builders[parity].addGap(gap * US_PER_MS)
            val fadeIn = if (i > 0) timeline.transitionLengthAfter(i - 1) else 0L
            val fadeOut = timeline.transitionLengthAfter(i)
            addVideoClip(builders[parity], timeline, clip, frame, fadeIn, fadeOut)
            cursors[parity] = start + clip.lengthMs
            start += clip.lengthMs - fadeOut
        }
        return builders.mapIndexed { parity, builder ->
            val tail = timeline.totalDurationMs - cursors[parity]
            if (tail > 0) builder.addGap(tail * US_PER_MS)
            builder.build()
        }
    }

    private fun videoSequence(timeline: Timeline): EditedMediaItemSequence? {
        if (timeline.videoLane.isEmpty() && timeline.textLane.isEmpty() && timeline.overlayLane.isEmpty()) return null
        val builder = EditedMediaItemSequence.Builder(videoTrackTypes(timeline, timeline.videoLane))
        val frame = frameEffects()
        timeline.videoLane.forEach { clip -> addVideoClip(builder, timeline, clip, frame) }
        // Text or an overlay that ends after the video plays over black, so the sequence runs on as a gap.
        val extraEndMs = maxOf(
            timeline.textLane.maxOfOrNull { it.endMs } ?: 0L,
            timeline.overlayLane.maxOfOrNull { it.endMs } ?: 0L,
        )
        if (extraEndMs > timeline.videoLengthMs) builder.addGap((extraEndMs - timeline.videoLengthMs) * US_PER_MS)
        return builder.build()
    }

    /**
     * A sequence with no picture-clip sound (photos only, silent or muted clips) would wait for an audio track that
     * never comes, which fails the export, so it only asks for video; sound comes from the audio lane.
     */
    private fun videoTrackTypes(timeline: Timeline, clips: List<VideoClip>): Set<Int> {
        val hasSound = clips.any {
            it.attachedAudio && !it.muted && timeline.media[it.mediaItemId]?.kind == MediaKind.Video
        }
        return if (hasSound) setOf(C.TRACK_TYPE_VIDEO, C.TRACK_TYPE_AUDIO) else setOf(C.TRACK_TYPE_VIDEO)
    }

    private fun addVideoClip(
        builder: EditedMediaItemSequence.Builder,
        timeline: Timeline,
        clip: VideoClip,
        frame: Effects,
        fadeInMs: Long = 0,
        fadeOutMs: Long = 0,
    ) {
        val media = timeline.media[clip.mediaItemId]
        when {
            media == null || media.availability == Availability.Missing -> builder.addGap(clip.lengthMs * US_PER_MS)
            media.kind == MediaKind.Photo -> builder.addItem(
                EditedMediaItem.Builder(
                    // Transformer needs the image duration on the media item itself, not only on the edited item.
                    MediaItem.Builder().setUri(Uri.parse(media.uri)).setImageDurationMs(clip.lengthMs).build(),
                )
                    .setDurationUs(clip.lengthMs * US_PER_MS)
                    .setFrameRate(spec.fps)
                    .setRemoveAudio(true)
                    .setEffects(withFilter(clip, frame))
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
                                crossfaded(clip.fx, fadeInMs, fadeOutMs), clip.lengthMs,
                            ),
                            withFilter(clip, frame).videoEffects,
                        ),
                    )
                    .build(),
            )
        }
    }

    /** The clip's own fades, raised to the length of the transitions on its sides so the sound crosses over. */
    private fun crossfaded(fx: AudioFx, fadeInMs: Long, fadeOutMs: Long): AudioFx =
        if (fadeInMs == 0L && fadeOutMs == 0L) {
            fx
        } else {
            fx.copy(fade = Fade(maxOf(fx.fade.inMs, fadeInMs), maxOf(fx.fade.outMs, fadeOutMs)))
        }

    /**
     * One video-only sequence per overlay clip: a gap up to its start, then the clip. The compositor draws input 0
     * (the main video) first and each overlay over it, placed by [OverlayLayout]. Overlays have no sound.
     */
    private fun overlaySequences(timeline: Timeline): List<EditedMediaItemSequence> =
        timeline.overlayLane.mapNotNull { clip ->
            val media = timeline.media[clip.mediaItemId]
            if (media == null || media.availability == Availability.Missing) return@mapNotNull null
            val builder = EditedMediaItemSequence.Builder(setOf(C.TRACK_TYPE_VIDEO))
            if (clip.startMs > 0) builder.addGap(clip.startMs * US_PER_MS)
            val item = if (media.kind == MediaKind.Photo) {
                EditedMediaItem.Builder(
                    MediaItem.Builder().setUri(Uri.parse(media.uri)).setImageDurationMs(clip.durationMs).build(),
                )
                    .setDurationUs(clip.durationMs * US_PER_MS)
                    .setFrameRate(spec.fps)
            } else {
                EditedMediaItem.Builder(clipped(media.uri, clip.sourceStartMs, clip.sourceEndMs))
                    .setDurationUs(sourceDurationUs(media.durationMs, clip.sourceEndMs))
            }
            builder.addItem(item.setRemoveAudio(true).build())
            // An ended input would keep showing its last frame, so a gap runs on to the end of the project.
            val tailMs = timeline.totalDurationMs - clip.endMs
            if (tailMs > 0) builder.addGap(tailMs * US_PER_MS)
            builder.build()
        }

    /** [frame] plus the clip's color transform and vignette when it has a filter on. */
    private fun withFilter(clip: VideoClip, frame: Effects): Effects {
        val effect = clip.effect?.let { ClipEffectHandle(it).also { handle -> clipEffects[clip.id] = handle } }
        val framing = clip.takeIf { it.keyframes.isNotEmpty() || it.id in framingIds }
            ?.let { ClipFramingHandle(clip).also { handle -> framings[clip.id] = handle } }
        val turn = transformEffects(clip.transform)
        val plain = effect == null && framing == null && turn.isEmpty()
        if (clip.filter.isNeutral && plain) return frame
        val filter = if (clip.filter.isNeutral) {
            emptyList()
        } else {
            ClipFilterEffects(clip.filter).also { filters[clip.id] = it }.effects
        }
        val picture = listOfNotNull(effect?.glEffect, framing?.glEffect)
        return Effects(frame.audioProcessors, turn + frame.videoEffects + filter + picture)
    }

    /** One overlay effect for every text clip, at the composition level so times are the project's. */
    private fun textEffects(timeline: Timeline): Effects? {
        if (timeline.textLane.isEmpty()) return null
        val width = spec.width ?: timeline.format?.width ?: DEFAULT_WIDTH
        val height = spec.height ?: timeline.format?.height ?: DEFAULT_HEIGHT
        val set = TextOverlaySet(timeline.textLane, width, height).also { textOverlays = it }
        val effect = set.effect ?: return null
        // The output frame is fixed first, so text sizes follow it even when the video lane is only gaps.
        val frame = Presentation.createForWidthAndHeight(width, height, Presentation.LAYOUT_SCALE_TO_FIT)
        return Effects(emptyList(), listOf(frame, effect))
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
                        .setEffects(Effects(
                            soundEffects(clip.id, clip.reverb, clip.volumePercent, true, clip.fx, clip.lengthMs),
                            emptyList(),
                        ))
                        .build(),
                )
            }
            cursor = clip.timelineEndMs
        }
        return builder.build()
    }

    /**
     * Noise reduction, equalizer, and voice effects first, then reverb, then fade and volume, so volume 0% or a
     * fade silences the effect tails too.
     */
    private fun soundEffects(
        clipId: String,
        reverb: Reverb,
        percent: Int,
        hasAudio: Boolean,
        fx: AudioFx,
        clipLengthMs: Long,
    ): List<AudioProcessor> {
        if (!hasAudio) return emptyList()
        val voice = VoiceFxProcessor(fx, spec.voiceFxAllowed).also { voiceFxs[clipId] = it }
        val gain = GainAudioProcessor(percent, fx.fade, clipLengthMs).also {
            gains[clipId] = it
            it.setEnvelope(duckEnvelopes[clipId])
        }
        val effective = reverb.copy(enabled = reverb.enabled && spec.reverbAllowed)
        val room = ReverbAudioProcessor(effective).also {
            it.update(effective, applyRoomSize = true)
            reverbs[clipId] = it
        }
        return listOf(voice, room, gain)
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

    /** Fits every clip inside the output frame (bars, never cropping or stretching). */
    private fun frameEffects(): Effects {
        val width = spec.width ?: return Effects.EMPTY
        val height = spec.height ?: return Effects.EMPTY
        val fit = Presentation.createForWidthAndHeight(width, height, Presentation.LAYOUT_SCALE_TO_FIT)
        val drop = spec.frameDropFps?.let {
            FrameDropEffect.createSimpleFrameDropEffect(spec.sourceFps.toFloat(), it.toFloat())
        }
        return Effects(emptyList(), listOfNotNull(fit, drop))
    }

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
        const val US_PER_MS = 1_000L
        const val DEFAULT_WIDTH = 1920
        const val DEFAULT_HEIGHT = 1080
        const val SPEED_DIVISOR = 1000f
    }
}
