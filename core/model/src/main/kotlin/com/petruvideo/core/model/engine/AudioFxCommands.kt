package com.petruvideo.core.model.engine

import com.petruvideo.core.model.AudioFx
import com.petruvideo.core.model.EqPreset
import com.petruvideo.core.model.EqPresets
import com.petruvideo.core.model.Equalizer
import com.petruvideo.core.model.Fade
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.NoiseReduction
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.VoiceFx

/** Changes one part of a clip's [AudioFx]; only for video, overlay, and audio clips with sound. */
abstract class AudioFxCommand internal constructor(
    private val clipId: String,
    private val label: String,
) : Command {
    internal abstract fun next(current: AudioFx, clipLengthMs: Long): AudioFx

    internal open fun notices(next: AudioFx): List<Notice> = emptyList()

    override fun apply(timeline: Timeline): CommandResult {
        val videoIndex = timeline.videoLane.indexOfFirst { it.id == clipId }
        if (videoIndex >= 0) return applyToVideo(timeline, videoIndex)
        val overlayIndex = timeline.overlayLane.indexOfFirst { it.id == clipId }
        if (overlayIndex >= 0) return applyToOverlay(timeline, overlayIndex)
        val audioIndex = timeline.audioLane.indexOfFirst { it.id == clipId }
        if (audioIndex < 0) return CommandResult.Rejected(RejectReason.NotFound)
        return applyToAudio(timeline, audioIndex)
    }

    private fun applyToVideo(timeline: Timeline, index: Int): CommandResult {
        val clip = timeline.videoLane[index]
        val photo = timeline.media[clip.mediaItemId]?.kind == MediaKind.Photo
        if (photo || !clip.attachedAudio) return CommandResult.Rejected(RejectReason.NoAttachedAudio)
        val fx = next(clip.fx, clip.lengthMs)
        if (fx == clip.fx) return CommandResult.Rejected(RejectReason.OutOfRange)
        val lane = timeline.videoLane.toMutableList().also { it[index] = clip.copy(fx = fx) }
        return CommandResult.Applied(timeline.copy(videoLane = lane), label, notices(fx))
    }

    private fun applyToAudio(timeline: Timeline, index: Int): CommandResult {
        val clip = timeline.audioLane[index]
        val fx = next(clip.fx, clip.lengthMs)
        if (fx == clip.fx) return CommandResult.Rejected(RejectReason.OutOfRange)
        val lane = timeline.audioLane.toMutableList().also { it[index] = clip.copy(fx = fx) }
        return CommandResult.Applied(timeline.copy(audioLane = lane), label, notices(fx))
    }

    private fun applyToOverlay(timeline: Timeline, index: Int): CommandResult {
        val clip = timeline.overlayLane[index]
        if (!clip.attachedAudio) return CommandResult.Rejected(RejectReason.NoAttachedAudio)
        val fx = next(clip.fx, clip.durationMs)
        if (fx == clip.fx) return CommandResult.Rejected(RejectReason.OutOfRange)
        val lane = timeline.overlayLane.toMutableList().also { it[index] = clip.copy(fx = fx) }
        return CommandResult.Applied(timeline.copy(overlayLane = lane), label, notices(fx))
    }
}

/** Sets the fade lengths; each is limited to half the clip so the two never overlap. */
class SetFade(clipId: String, private val fade: Fade) : AudioFxCommand(clipId, "Fade") {
    override fun next(current: AudioFx, clipLengthMs: Long) =
        current.copy(fade = fade.copy(inMs = fade.inMs.coerceAtLeast(0), outMs = fade.outMs.coerceAtLeast(0))
            .limitedTo(clipLengthMs))

    override fun notices(next: AudioFx) = if (next.fade != fade) listOf(Notice.FadeLimited) else emptyList()
}

class SetNoiseReduction(clipId: String, private val noise: NoiseReduction) : AudioFxCommand(clipId, "Noise reduction") {
    override fun next(current: AudioFx, clipLengthMs: Long) = current.copy(noise = noise)
}

/** Sets the three bands (clamped); the preset is recomputed. */
class SetEqualizer(clipId: String, private val eq: Equalizer) : AudioFxCommand(clipId, "Equalizer") {
    override fun next(current: AudioFx, clipLengthMs: Long) = current.copy(eq = eq.normalized())
}

/** Sets all bands from a preset. Custom is ignored. */
class ApplyEqPreset(clipId: String, private val preset: EqPreset) : AudioFxCommand(clipId, "Equalizer preset") {
    override fun next(current: AudioFx, clipLengthMs: Long) =
        if (preset == EqPreset.Custom) current else current.copy(eq = EqPresets.settings(preset))
}

/** Sets all four voice effects (clamped). */
class SetVoiceFx(clipId: String, private val voice: VoiceFx) : AudioFxCommand(clipId, "Voice effects") {
    override fun next(current: AudioFx, clipLengthMs: Long) = current.copy(voice = voice.normalized())
}
