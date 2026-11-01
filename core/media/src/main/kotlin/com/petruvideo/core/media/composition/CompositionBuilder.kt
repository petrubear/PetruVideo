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
import com.petruvideo.core.media.filter.ClipFilterEffects
import com.petruvideo.core.media.pressure.PreviewQuality
import com.petruvideo.core.media.preview.GainAudioProcessor
import com.petruvideo.core.media.preview.ReverbAudioProcessor
import com.petruvideo.core.media.preview.VoiceFxProcessor
import com.petruvideo.core.media.text.TextOverlaySet
import com.petruvideo.core.model.AudioFx
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
class CompositionBuilder(private val spec: BuildSpec) {
    val gains = mutableMapOf<String, GainAudioProcessor>()
    val reverbs = mutableMapOf<String, ReverbAudioProcessor>()
    val voiceFxs = mutableMapOf<String, VoiceFxProcessor>()

    /** The filter effects of each filtered clip after [build], so the preview can change them live. */
    val filters = mutableMapOf<String, ClipFilterEffects>()

    /** The text lane's overlays after [build], so the preview can change a clip live; null without text clips. */
    var textOverlays: TextOverlaySet? = null
        private set

    /** The composition for [timeline], or null when there is nothing to play or render. */
    fun build(timeline: Timeline): Composition? {
        val sequences = listOfNotNull(videoSequence(timeline), audioSequence(timeline))
        if (sequences.isEmpty()) return null
        val builder = Composition.Builder(sequences)
        textEffects(timeline)?.let { builder.setEffects(it) }
        if (spec.toneMapHdr) builder.setHdrMode(Composition.HDR_MODE_TONE_MAP_HDR_TO_SDR_USING_MEDIACODEC)
        return builder.build()
    }

    private fun videoSequence(timeline: Timeline): EditedMediaItemSequence? {
        if (timeline.videoLane.isEmpty() && timeline.textLane.isEmpty()) return null
        // A sequence with no picture-clip sound (photos only, silent or muted clips) would wait for an audio track
        // that never comes, which fails the export, so it only asks for video; sound comes from the audio lane.
        val hasSound = timeline.videoLane.any {
            it.attachedAudio && !it.muted && timeline.media[it.mediaItemId]?.kind == MediaKind.Video
        }
        val tracks = if (hasSound) setOf(C.TRACK_TYPE_VIDEO, C.TRACK_TYPE_AUDIO) else setOf(C.TRACK_TYPE_VIDEO)
        val builder = EditedMediaItemSequence.Builder(tracks)
        val fps = spec.fps
        val frame = frameEffects()
        timeline.videoLane.forEach { clip ->
            val media = timeline.media[clip.mediaItemId]
            when {
                media == null || media.availability == Availability.Missing -> builder.addGap(clip.lengthMs * US_PER_MS)
                media.kind == MediaKind.Photo -> builder.addItem(
                    EditedMediaItem.Builder(
                        // Transformer needs the image duration on the media item itself, not only on the edited item.
                        MediaItem.Builder().setUri(Uri.parse(media.uri)).setImageDurationMs(clip.lengthMs).build(),
                    )
                        .setDurationUs(clip.lengthMs * US_PER_MS)
                        .setFrameRate(fps)
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
                                    clip.fx, clip.lengthMs,
                                ),
                                withFilter(clip, frame).videoEffects,
                            ),
                        )
                        .build(),
                )
            }
        }
        // Text that ends after the video plays over black, so the video sequence runs on as a gap.
        val textEndMs = timeline.textLane.maxOfOrNull { it.endMs } ?: 0L
        if (textEndMs > timeline.videoLengthMs) builder.addGap((textEndMs - timeline.videoLengthMs) * US_PER_MS)
        return builder.build()
    }

    /** [frame] plus the clip's color transform and vignette when it has a filter on. */
    private fun withFilter(clip: VideoClip, frame: Effects): Effects {
        if (clip.filter.isNeutral) return frame
        val handles = ClipFilterEffects(clip.filter).also { filters[clip.id] = it }
        return Effects(frame.audioProcessors, frame.videoEffects + handles.effects)
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
        val gain = GainAudioProcessor(percent, fx.fade, clipLengthMs).also { gains[clipId] = it }
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
