package com.petruvideo.core.model.engine

import com.petruvideo.core.model.Availability
import com.petruvideo.core.model.AudioClip
import com.petruvideo.core.model.AudioOrigin
import com.petruvideo.core.model.Fingerprint
import com.petruvideo.core.model.MIN_CLIP_MS
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.ProjectFormat
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.VideoClip
import kotlin.random.Random

fun testVideo(id: String, durationMs: Long = 10_000, hasAudio: Boolean = true) = MediaItem(
    id = id,
    kind = MediaKind.Video,
    uri = "content://media/$id",
    displayName = "$id.mp4",
    durationMs = durationMs,
    width = 1920,
    height = 1080,
    frameRate = 30f,
    hasAudio = hasAudio,
    fingerprint = Fingerprint(1_000, 1, durationMs, "video/mp4"),
    availability = Availability.Available,
)

fun testAudio(id: String, durationMs: Long = 10_000) = MediaItem(
    id = id,
    kind = MediaKind.Audio,
    uri = "content://media/$id",
    displayName = "$id.mp3",
    durationMs = durationMs,
    width = null,
    height = null,
    frameRate = null,
    hasAudio = true,
    fingerprint = Fingerprint(500, 1, durationMs, "audio/mpeg"),
    availability = Availability.Available,
)

fun testPhoto(id: String) = MediaItem(
    id = id,
    kind = MediaKind.Photo,
    uri = "content://media/$id",
    displayName = "$id.jpg",
    durationMs = null,
    width = 4000,
    height = 3000,
    frameRate = null,
    hasAudio = false,
    fingerprint = Fingerprint(300, 1, null, "image/jpeg"),
    availability = Availability.Available,
)

/** Builds a random but valid timeline (audio clips are laid out without overlap). */
fun randomTimeline(random: Random): Timeline {
    val media = mutableMapOf<String, MediaItem>()
    val video = (0 until random.nextInt(0, 8)).map { i ->
        val sourceLength = random.nextLong(2_000, 20_000)
        val item = testVideo("v$i", sourceLength)
        media[item.id] = item
        val start = random.nextLong(0, sourceLength - MIN_CLIP_MS)
        val end = random.nextLong(start + MIN_CLIP_MS, sourceLength + 1)
        VideoClip("vc$i", item.id, start, end, attachedAudio = random.nextBoolean())
    }
    var cursor = random.nextLong(0, 2_000)
    val audio = (0 until random.nextInt(0, 6)).map { i ->
        val sourceLength = random.nextLong(2_000, 20_000)
        val item = testAudio("a$i", sourceLength)
        media[item.id] = item
        val start = random.nextLong(0, sourceLength - MIN_CLIP_MS)
        val end = random.nextLong(start + MIN_CLIP_MS, sourceLength + 1)
        val clip = AudioClip("ac$i", item.id, start, end, cursor, random.nextBoolean(), AudioOrigin.Added)
        cursor = clip.timelineEndMs + random.nextLong(0, 3_000)
        clip
    }
    return Timeline(
        projectId = "p",
        format = ProjectFormat(1920, 1080, 30),
        media = media,
        videoLane = video,
        audioLane = audio,
        playheadMs = random.nextLong(0, 5_000),
    )
}
