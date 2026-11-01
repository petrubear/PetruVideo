package com.petruvideo.core.model

import com.petruvideo.core.model.engine.CommandResult
import com.petruvideo.core.model.engine.RejectReason
import com.petruvideo.core.model.engine.SetDucking
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlin.math.abs
import org.junit.Test

class DuckingTest {
    private fun media(id: String, kind: MediaKind = MediaKind.Audio, audio: Boolean = true) = MediaItem(
        id, kind, "content://$id", "$id.m4a", 20_000, null, null, null, audio,
        Fingerprint(1, 1, 20_000, "audio/mp4"),
    )

    /** Peaks with sound (0.5) from [from] to [to] ms and silence elsewhere, 20 per second, 20 s long. */
    private fun speech(from: Long, to: Long) =
        FloatArray(400) { i -> if (i * 50L in from until to) 0.5f else 0f }

    private val music = AudioClip("music", "mu", 0, 10_000, 0, origin = AudioOrigin.Added,
        ducking = Ducking(enabled = true, amountPercent = 100, smoothness = DuckSmoothness.Quick))
    private val voice = AudioClip("voice", "vo", 0, 4_000, 3_000, origin = AudioOrigin.Added)
    private val timeline = Timeline(
        "p", media = mapOf("mu" to media("mu"), "vo" to media("vo")),
        audioLane = listOf(music, voice),
    )
    private val peaks = mapOf("mu" to FloatArray(400) { 0.6f }, "vo" to speech(0, 4_000))

    private fun env(t: Timeline = timeline) = DuckingPlanner.envelopeFor(t, "music") { peaks[it] }!!

    @Test
    fun `music is full level before the voice and low under it`() {
        val e = env()
        e.gainAt(1_000) shouldBe 1f
        e.gainAt(5_000) shouldBeLessThan 0.15f
        e.gainAt(9_000) shouldBeGreaterThan 0.9f
    }

    @Test
    fun `the music is already low when the voice starts`() {
        env().gainAt(3_000) shouldBeLessThan 0.7f
    }

    @Test
    fun `amount zero changes nothing and amount 100 never reaches silence`() {
        val none = timeline.copy(audioLane = listOf(music.copy(ducking = music.ducking.copy(amountPercent = 0)), voice))
        env(none).gains.all { it == 1f } shouldBe true
        env().gains.min() shouldBeGreaterThan 0.09f
    }

    @Test
    fun `a muted or silent trigger does not duck`() {
        val muted = timeline.copy(audioLane = listOf(music, voice.copy(muted = true)))
        env(muted).gains.all { it == 1f } shouldBe true
        val zero = timeline.copy(audioLane = listOf(music, voice.copy(volumePercent = 0)))
        env(zero).gains.all { it == 1f } shouldBe true
    }

    @Test
    fun `a trigger that is ducking itself does not duck the other clip`() {
        val both = timeline.copy(audioLane = listOf(music, voice.copy(ducking = Ducking(enabled = true))))
        env(both).gains.all { it == 1f } shouldBe true
    }

    @Test
    fun `a trigger at the very start lowers from the first moment`() {
        val early = timeline.copy(audioLane = listOf(music, voice.copy(timelineStartMs = 0)))
        env(early).gainAt(0) shouldBeLessThan 0.15f
    }

    @Test
    fun `video sound ducks the clip and follows the trim and speed of the trigger`() {
        val video = VideoClip("v", "vi", 2_000, 6_000, true, speedMilli = 2_000)
        val t = Timeline(
            "p", media = mapOf("mu" to media("mu"), "vi" to media("vi", MediaKind.Video)),
            videoLane = listOf(video), audioLane = listOf(music),
        )
        // The video plays source 2 000..6 000 ms at 2x: timeline 0..2 000 ms, sound in source 4 000..6 000.
        val p = mapOf("mu" to FloatArray(400) { 0.6f }, "vi" to speech(4_000, 6_000))
        val e = DuckingPlanner.envelopeFor(t, "music") { p[it] }!!
        e.gainAt(1_500) shouldBeLessThan 0.2f
        e.gainAt(5_000) shouldBeGreaterThan 0.95f
    }

    @Test
    fun `the gain never jumps by more than 3 dB in 5 ms`() {
        val e = env()
        var t = 0L
        while (t < 9_990) {
            val a = e.gainAt(t)
            val b = e.gainAt(t + 5)
            val db = abs(20 * kotlin.math.log10(b / a))
            (db < 3f) shouldBe true
            t += 5
        }
    }

    @Test
    fun `a clip that is not ducking has no envelope`() {
        val off = timeline.copy(audioLane = listOf(music.copy(ducking = Ducking()), voice))
        DuckingPlanner.envelopeFor(off, "music") { peaks[it] } shouldBe null
    }

    @Test
    fun `SetDucking changes only audio clips and clamps the amount`() {
        val result = SetDucking("voice", Ducking(true, 150)).apply(timeline) as CommandResult.Applied
        result.timeline.audioLane.first { it.id == "voice" }.ducking.amountPercent shouldBe 100
        SetDucking("voice", Ducking()).apply(timeline) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
        SetDucking("zz", Ducking(true)).apply(timeline) shouldBe CommandResult.Rejected(RejectReason.NotFound)
    }

    @Test
    fun `a reversed or curved video trigger ducks where its sound is actually heard`() {
        // Source 0..6 s with speech only in 4..6 s. Played backwards, that speech is heard in the first 2 s.
        val video = VideoClip("v", "vi", 0, 6_000, true, reversed = true)
        val t = Timeline(
            "p", media = mapOf("mu" to media("mu"), "vi" to media("vi", MediaKind.Video)),
            videoLane = listOf(video), audioLane = listOf(music),
        )
        val p = mapOf("mu" to FloatArray(400) { 0.6f }, "vi" to speech(4_000, 6_000))
        val e = DuckingPlanner.envelopeFor(t, "music") { p[it] }!!
        e.gainAt(1_000) shouldBeLessThan 0.2f
        e.gainAt(4_000) shouldBeGreaterThan 0.95f
    }
}
