package com.petruvideo.feature.editor

import com.petruvideo.core.model.AudioClip
import com.petruvideo.core.model.AudioOrigin
import com.petruvideo.core.model.Availability
import com.petruvideo.core.model.Fingerprint
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.VideoClip
import io.kotest.matchers.shouldBe
import org.junit.Test

class TrimLimitsTest {
    private fun media(kind: MediaKind, duration: Long?) = MediaItem(
        "m", kind, "content://m", "m", duration, null, null, null, true,
        Fingerprint(1, 1, duration, "x"), Availability.Available,
    )

    private val video = VideoClip("c", "m", 2_000, 6_000, attachedAudio = true)
    private val videoMedia = media(MediaKind.Video, 10_000)

    @Test
    fun `start handle stops at zero and at the minimum length`() {
        TrimLimits.video(video, videoMedia, true, -5_000) shouldBe TrimTarget(0, 6_000)
        TrimLimits.video(video, videoMedia, true, 9_000) shouldBe TrimTarget(5_900, 100)
        TrimLimits.video(video, videoMedia, true, 500) shouldBe TrimTarget(2_500, 3_500)
    }

    @Test
    fun `end handle stops at the source end and at the minimum length`() {
        TrimLimits.video(video, videoMedia, false, 9_000) shouldBe TrimTarget(10_000, 8_000)
        TrimLimits.video(video, videoMedia, false, -9_000) shouldBe TrimTarget(2_100, 100)
    }

    @Test
    fun `photos stretch between half a second and a minute`() {
        val photo = VideoClip("p", "m", 0, 5_000, attachedAudio = false)
        val photoMedia = media(MediaKind.Photo, null)
        TrimLimits.video(photo, photoMedia, false, 100_000) shouldBe TrimTarget(60_000, 60_000)
        TrimLimits.video(photo, photoMedia, false, -100_000) shouldBe TrimTarget(500, 500)
        TrimLimits.video(photo, photoMedia, true, -3_000) shouldBe TrimTarget(-3_000, 8_000)
        TrimLimits.video(photo, photoMedia, true, 100_000) shouldBe TrimTarget(4_500, 500)
    }

    @Test
    fun `audio handles respect source bounds`() {
        val clip = AudioClip("a", "m", 1_000, 4_000, 0, origin = AudioOrigin.Added)
        val audio = media(MediaKind.Audio, 8_000)
        TrimLimits.audio(clip, audio, true, -2_000) shouldBe TrimTarget(0, 4_000)
        TrimLimits.audio(clip, audio, false, 9_000) shouldBe TrimTarget(8_000, 7_000)
        TrimLimits.audio(clip, audio, false, -9_000) shouldBe TrimTarget(1_100, 100)
    }

    @Test
    fun `handles on a 2x clip move by what the user sees`() {
        val fast = video.copy(speedMilli = 2000) // 4 s of source plays in 2 s
        TrimLimits.video(fast, videoMedia, false, 500) shouldBe TrimTarget(7_000, 2_500)
        TrimLimits.video(fast, videoMedia, true, 500) shouldBe TrimTarget(3_000, 1_500)
        // the minimum is 100 ms of timeline, which is 200 ms of source
        TrimLimits.video(fast, videoMedia, true, 9_000) shouldBe TrimTarget(5_800, 100)
    }

    @Test
    fun `handles on a half speed audio clip`() {
        val slow = AudioClip("a", "m", 0, 2_000, timelineStartMs = 0, origin = AudioOrigin.Added, speedMilli = 500)
        TrimLimits.audio(slow, media(MediaKind.Audio, 10_000), false, 1_000) shouldBe TrimTarget(2_500, 5_000)
    }
}
