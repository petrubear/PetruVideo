package com.petruvideo.core.model.engine

import com.petruvideo.core.model.AudioClip
import com.petruvideo.core.model.AudioOrigin
import com.petruvideo.core.model.Availability
import com.petruvideo.core.model.Fingerprint
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.VOICE_OVER_MAX_MS
import com.petruvideo.core.model.VOICE_OVER_MIN_FREE_BYTES
import com.petruvideo.core.model.VoiceOverLimit
import com.petruvideo.core.model.VoiceOverStop
import io.kotest.matchers.shouldBe
import org.junit.Test

class AddVoiceOverTest {
    private fun take(ms: Long, kind: MediaKind = MediaKind.Audio) = MediaItem(
        "vo", kind, "file:///vo.aac", "Voice-over", ms, null, null, null, true, Fingerprint(1, 1, ms, "audio/aac"),
    )

    private val music = AudioClip("m", "mu", 0, 4_000, 10_000, false, AudioOrigin.Added)
    private val timeline = Timeline("p", audioLane = listOf(music))

    private fun applied(c: Command, t: Timeline = timeline) = (c.apply(t) as CommandResult.Applied).timeline

    @Test
    fun `the clip starts exactly where the take started and keeps the lane sorted`() {
        val t = applied(AddVoiceOver(take(3_000), 2_000, { "c" }))
        t.audioLane.map { it.id } shouldBe listOf("c", "m")
        t.audioLane.first().timelineStartMs shouldBe 2_000
        t.audioLane.first().lengthMs shouldBe 3_000
        t.media["vo"]?.durationMs shouldBe 3_000
    }

    @Test
    fun `short takes, overlaps, missing files and other kinds are rejected`() {
        AddVoiceOver(take(400), 0).apply(timeline) shouldBe CommandResult.Rejected(RejectReason.BelowMinimumLength)
        // A take may start over another audio clip: up to three layers play together.
        (AddVoiceOver(take(5_000), 8_000).apply(timeline) is CommandResult.Applied) shouldBe true
        (AddVoiceOver(take(2_000), 11_000).apply(timeline) is CommandResult.Applied) shouldBe true
        AddVoiceOver(take(2_000, MediaKind.Video), 0).apply(timeline) shouldBe
            CommandResult.Rejected(RejectReason.OutOfRange)
        AddVoiceOver(take(2_000).copy(availability = Availability.Missing), 0).apply(timeline) shouldBe
            CommandResult.Rejected(RejectReason.MissingMedia)
        AddVoiceOver(take(2_000), -1).apply(timeline) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
    }

    @Test
    fun `a take ending exactly at the next clip fits`() {
        applied(AddVoiceOver(take(2_000), 8_000)).audioLane.size shouldBe 2
    }

    @Test
    fun `the longest take stops at the next clip, 30 minutes, or the storage reserve`() {
        val plenty = 10L * 1024 * 1024 * 1024
        VoiceOverLimit.maxDurationMs(2_000, listOf(music), plenty) shouldBe 8_000
        VoiceOverLimit.stopReason(2_000, listOf(music), plenty) shouldBe VoiceOverStop.NextClip
        VoiceOverLimit.maxDurationMs(20_000, listOf(music), plenty) shouldBe VOICE_OVER_MAX_MS
        VoiceOverLimit.stopReason(20_000, listOf(music), plenty) shouldBe VoiceOverStop.TimeLimit
        // 12 000 bytes is one second at 96 kbps.
        VoiceOverLimit.maxDurationMs(20_000, emptyList(), VOICE_OVER_MIN_FREE_BYTES + 120_000) shouldBe 10_000
        VoiceOverLimit.stopReason(20_000, emptyList(), VOICE_OVER_MIN_FREE_BYTES + 120_000) shouldBe
            VoiceOverStop.Storage
        VoiceOverLimit.maxDurationMs(20_000, emptyList(), VOICE_OVER_MIN_FREE_BYTES - 1) shouldBe 0
    }

    @Test
    fun `starting inside an existing clip leaves no room`() {
        VoiceOverLimit.maxDurationMs(11_000, listOf(music), 10L * 1024 * 1024 * 1024) shouldBe 0
        VoiceOverLimit.durationOfBytes(12_000) shouldBe 1_000
    }
}
