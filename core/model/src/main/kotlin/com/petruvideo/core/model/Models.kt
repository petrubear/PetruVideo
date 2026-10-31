package com.petruvideo.core.model

const val MIN_CLIP_MS = 100L
const val PHOTO_DEFAULT_MS = 5_000L
const val PHOTO_MIN_MS = 500L
const val PHOTO_MAX_MS = 60_000L
const val SPEED_UNIT = 1000
const val SPEED_MIN_MILLI = 250
const val SPEED_MAX_MILLI = 4000
const val VOLUME_DEFAULT_PERCENT = 100
const val VOLUME_MAX_PERCENT = 200

/** Timeline length of [sourceMs] of footage played at [speedMilli] (1000 = 1x), rounded to the millisecond. */
fun timelineLengthMs(sourceMs: Long, speedMilli: Int): Long = (sourceMs * SPEED_UNIT + speedMilli / 2) / speedMilli

enum class MediaKind { Video, Photo, Audio }

enum class Availability { Available, Missing }

enum class ProjectStatus { Draft, Exported }

enum class AudioOrigin { Detached, Added }

data class Fingerprint(
    val sizeBytes: Long,
    val lastModified: Long,
    val durationMs: Long?,
    val mimeType: String,
)

data class ProjectFormat(val width: Int, val height: Int, val fps: Int)

data class Project(
    val id: String,
    val name: String,
    val createdAt: Long,
    val modifiedAt: Long,
    val status: ProjectStatus = ProjectStatus.Draft,
    val format: ProjectFormat? = null,
    val thumbnailRef: String? = null,
    val schemaVersion: Int = 1,
)

data class MediaItem(
    val id: String,
    val kind: MediaKind,
    val uri: String,
    val displayName: String,
    val durationMs: Long?,
    val width: Int?,
    val height: Int?,
    val frameRate: Float?,
    val hasAudio: Boolean,
    val fingerprint: Fingerprint,
    val availability: Availability = Availability.Available,
)

data class VideoClip(
    val id: String,
    val mediaItemId: String,
    val sourceStartMs: Long,
    val sourceEndMs: Long,
    val attachedAudio: Boolean,
    val speedMilli: Int = SPEED_UNIT,
    val volumePercent: Int = VOLUME_DEFAULT_PERCENT,
) {
    val sourceLengthMs: Long get() = sourceEndMs - sourceStartMs
    val lengthMs: Long get() = timelineLengthMs(sourceLengthMs, speedMilli)
}

data class AudioClip(
    val id: String,
    val mediaItemId: String,
    val sourceStartMs: Long,
    val sourceEndMs: Long,
    val timelineStartMs: Long,
    val muted: Boolean = false,
    val origin: AudioOrigin,
    val speedMilli: Int = SPEED_UNIT,
    val volumePercent: Int = VOLUME_DEFAULT_PERCENT,
) {
    val sourceLengthMs: Long get() = sourceEndMs - sourceStartMs
    val lengthMs: Long get() = timelineLengthMs(sourceLengthMs, speedMilli)
    val timelineEndMs: Long get() = timelineStartMs + lengthMs
}

/**
 * Immutable editing state. It carries the project format and the media items its clips refer to so that
 * every command is a pure function of (Timeline, command) and undo snapshots restore everything.
 */
data class Timeline(
    val projectId: String,
    val format: ProjectFormat? = null,
    val media: Map<String, MediaItem> = emptyMap(),
    val videoLane: List<VideoClip> = emptyList(),
    val audioLane: List<AudioClip> = emptyList(),
    val playheadMs: Long = 0,
) {
    val videoLengthMs: Long get() = videoLane.sumOf { it.lengthMs }

    val totalDurationMs: Long
        get() = maxOf(videoLengthMs, audioLane.maxOfOrNull { it.timelineEndMs } ?: 0L)

    /** Start of each video clip on the timeline, derived from order (the lane is gapless). */
    fun videoStartOf(clipId: String): Long? {
        var start = 0L
        for (clip in videoLane) {
            if (clip.id == clipId) return start
            start += clip.lengthMs
        }
        return null
    }
}
