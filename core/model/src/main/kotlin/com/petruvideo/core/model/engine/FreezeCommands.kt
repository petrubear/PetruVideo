package com.petruvideo.core.model.engine

import com.petruvideo.core.model.FREEZE_MAX_MS
import com.petruvideo.core.model.FREEZE_MIN_MS
import com.petruvideo.core.model.Keyframe
import com.petruvideo.core.model.MIN_CLIP_MS
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.VideoClip
import java.util.UUID

/**
 * Holds the frame shown at timeline time [atMs] of the main-track clip [clipId] for [lengthMs]: the clip is cut there
 * and the [still] (a photo made from that frame) goes in between as its own clip. At the very start or end of the clip
 * the still goes before or after it without an empty part. The still has no sound and copies the clip's transform,
 * filter, effect, and the pose its keyframes show at that moment.
 */
class FreezeFrame(
    private val clipId: String,
    private val atMs: Long,
    private val lengthMs: Long,
    private val still: MediaItem,
    private val newClipId: () -> String = { UUID.randomUUID().toString() },
) : Command {
    override fun apply(timeline: Timeline): CommandResult {
        val index = timeline.videoLane.indexOfFirst { it.id == clipId }
        if (index < 0) return CommandResult.Rejected(RejectReason.NotFound)
        val clip = timeline.videoLane[index]
        val start = timeline.videoStartOf(clipId) ?: return CommandResult.Rejected(RejectReason.NotFound)
        val offset = atMs - start
        val valid = lengthMs in FREEZE_MIN_MS..FREEZE_MAX_MS && still.kind == MediaKind.Photo &&
            offset in 0..clip.lengthMs && timeline.media[clip.mediaItemId]?.kind == MediaKind.Video
        if (!valid) return CommandResult.Rejected(RejectReason.OutOfRange)
        val frozen = stillClip(clip, offset)
        val lane = timeline.videoLane.toMutableList()
        when {
            offset < MIN_CLIP_MS -> lane.add(index, frozen)
            clip.lengthMs - offset < MIN_CLIP_MS -> {
                // The transition into the next clip now leaves the still instead.
                lane[index] = clip.copy(transitionOut = null)
                lane.add(index + 1, frozen.copy(transitionOut = clip.transitionOut))
            }
            else -> {
                val split = Split(clipId, atMs, newClipId).apply(timeline) as? CommandResult.Applied
                    ?: return CommandResult.Rejected(RejectReason.OutOfRange)
                val parts = split.timeline.videoLane
                return CommandResult.Applied(
                    timeline.copy(
                        media = timeline.media + (still.id to still),
                        videoLane = parts.take(index + 1) + frozen + parts.drop(index + 1),
                    ),
                    "Freeze frame",
                )
            }
        }
        return CommandResult.Applied(
            timeline.copy(media = timeline.media + (still.id to still), videoLane = lane),
            "Freeze frame",
        )
    }

    private fun stillClip(clip: VideoClip, offset: Long): VideoClip {
        val frozen = VideoClip(
            id = newClipId(),
            mediaItemId = still.id,
            sourceStartMs = 0,
            sourceEndMs = lengthMs,
            attachedAudio = false,
            filter = clip.filter,
            effects = clip.effects,
            transform = clip.transform,
        )
        if (clip.keyframes.isEmpty()) return frozen
        return frozen.withKeyframes(listOf(Keyframe(0, clip.poseAt(offset))))
    }
}
