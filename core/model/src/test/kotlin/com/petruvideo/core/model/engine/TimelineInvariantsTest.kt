package com.petruvideo.core.model.engine

import com.petruvideo.core.model.AudioClip
import com.petruvideo.core.model.AudioOrigin
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.VideoClip
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.random.Random
import org.junit.Test

class TimelineInvariantsTest {
    @Test
    fun `random valid timelines have no violations`() {
        repeat(500) { seed ->
            val timeline = randomTimeline(Random(seed))
            TimelineInvariants.violations(timeline).shouldBeEmpty()
        }
    }

    @Test
    fun `video start positions equal the running sum of clip lengths`() {
        repeat(200) { seed ->
            val t = randomTimeline(Random(seed))
            var sum = 0L
            for (clip in t.videoLane) {
                t.videoStartOf(clip.id) shouldBe sum
                sum += clip.lengthMs
            }
            t.videoLengthMs shouldBe sum
        }
    }

    @Test
    fun `total duration is the video length extended by a later audio end`() {
        repeat(200) { seed ->
            val t = randomTimeline(Random(seed))
            val expected = maxOf(t.videoLane.sumOf { it.lengthMs }, t.audioLane.maxOfOrNull { it.timelineEndMs } ?: 0L)
            t.totalDurationMs shouldBe expected
        }
    }

    @Test
    fun `a clip shorter than the minimum is reported`() {
        val t = randomTimeline(Random(1)).let {
            it.copy(videoLane = it.videoLane + VideoClip("short", it.media.keys.first(), 0, 50, true))
        }
        TimelineInvariants.violations(t) shouldHaveSize 1
    }

    @Test
    fun `a trim outside the source is reported`() {
        val base = randomTimeline(Random(2)).let { it.copy(videoLane = emptyList(), audioLane = emptyList()) }
        val media = testVideo("m", durationMs = 1_000)
        val t = base.copy(
            media = base.media + (media.id to media),
            videoLane = listOf(VideoClip("c", "m", 0, 2_000, true)),
        )
        TimelineInvariants.violations(t) shouldHaveSize 1
    }

    @Test
    fun `more than three audio clips at once are reported and up to three are fine`() {
        val media = testAudio("a", 10_000)
        fun lane(count: Int) = Timeline(
            projectId = "p",
            media = mapOf(media.id to media),
            audioLane = (0 until count).map { AudioClip("c$it", "a", 0, 5_000, it * 1_000L, false, AudioOrigin.Added) },
        )
        TimelineInvariants.violations(lane(3)) shouldHaveSize 0
        TimelineInvariants.violations(lane(4)) shouldHaveSize 1
    }

    @Test
    fun `speed and volume out of range, a sped photo, and a too-short derived length are reported`() {
        val t = Timeline(
            projectId = "p",
            media = mapOf("v" to testVideo("v"), "ph" to testPhoto("ph")),
            videoLane = listOf(
                VideoClip("a", "v", 0, 5_000, attachedAudio = true, speedMilli = 100),
                VideoClip("b", "v", 0, 5_000, attachedAudio = true, volumePercent = 250),
                VideoClip("c", "ph", 0, 5_000, attachedAudio = false, speedMilli = 2000),
                VideoClip("d", "v", 0, 300, attachedAudio = true, speedMilli = 4000),
            ),
        )
        val violations = TimelineInvariants.violations(t)
        violations.any { "a speed" in it } shouldBe true
        violations.any { "b volume" in it } shouldBe true
        violations.any { "photo clip c" in it } shouldBe true
        violations.any { "d is shorter" in it } shouldBe true
    }
}
