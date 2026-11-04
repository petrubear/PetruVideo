package com.petruvideo.core.model.engine

import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.Reverb
import com.petruvideo.core.model.ReverbPreset
import com.petruvideo.core.model.ReverbPresets
import com.petruvideo.core.model.Timeline

/** Mutes or unmutes the attached sound of a video clip; volume and reverb are untouched. */
class SetVideoMuted(private val clipId: String, private val muted: Boolean) : Command {
    override fun apply(timeline: Timeline): CommandResult {
        val index = timeline.videoLane.indexOfFirst { it.id == clipId }
        if (index < 0) return CommandResult.Rejected(RejectReason.NotFound)
        val clip = timeline.videoLane[index]
        if (!clip.attachedAudio) return CommandResult.Rejected(RejectReason.NoAttachedAudio)
        if (clip.muted == muted) return CommandResult.Rejected(RejectReason.OutOfRange)
        val lane = timeline.videoLane.toMutableList().also { it[index] = clip.copy(muted = muted) }
        return CommandResult.Applied(timeline.copy(videoLane = lane), if (muted) "Mute clip" else "Unmute clip")
    }
}

abstract class ReverbCommand internal constructor(
    private val clipId: String,
    private val label: String,
) : Command {
    internal abstract fun next(current: Reverb): Reverb

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
        val reverb = next(clip.reverb)
        if (reverb == clip.reverb) return CommandResult.Rejected(RejectReason.OutOfRange)
        val lane = timeline.videoLane.toMutableList().also { it[index] = clip.copy(reverb = reverb) }
        return CommandResult.Applied(timeline.copy(videoLane = lane), label)
    }

    private fun applyToAudio(timeline: Timeline, index: Int): CommandResult {
        val clip = timeline.audioLane[index]
        val reverb = next(clip.reverb)
        if (reverb == clip.reverb) return CommandResult.Rejected(RejectReason.OutOfRange)
        val lane = timeline.audioLane.toMutableList().also { it[index] = clip.copy(reverb = reverb) }
        return CommandResult.Applied(timeline.copy(audioLane = lane), label)
    }

    private fun applyToOverlay(timeline: Timeline, index: Int): CommandResult {
        val clip = timeline.overlayLane[index]
        if (!clip.attachedAudio) return CommandResult.Rejected(RejectReason.NoAttachedAudio)
        val reverb = next(clip.reverb)
        if (reverb == clip.reverb) return CommandResult.Rejected(RejectReason.OutOfRange)
        val lane = timeline.overlayLane.toMutableList().also { it[index] = clip.copy(reverb = reverb) }
        return CommandResult.Applied(timeline.copy(overlayLane = lane), label)
    }
}

/** Turns reverb on or off; the settings are kept. */
class SetReverbEnabled(clipId: String, private val enabled: Boolean) :
    ReverbCommand(clipId, if (enabled) "Reverb on" else "Reverb off") {
    override fun next(current: Reverb) = current.copy(enabled = enabled)
}

/** Sets all five settings from a preset and turns reverb on. Custom is rejected. */
class ApplyReverbPreset(clipId: String, private val preset: ReverbPreset) : ReverbCommand(clipId, "Reverb preset") {
    override fun next(current: Reverb) =
        if (preset == ReverbPreset.Custom) current else ReverbPresets.settings(preset, enabled = true)
}

/** Sets the five numbers (clamped); the preset is recomputed and the on/off state kept. */
class SetReverbSettings(clipId: String, private val settings: Reverb) : ReverbCommand(clipId, "Reverb settings") {
    override fun next(current: Reverb) = settings.copy(enabled = current.enabled).normalized()
}
