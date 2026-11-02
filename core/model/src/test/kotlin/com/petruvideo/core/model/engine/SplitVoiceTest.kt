package com.petruvideo.core.model.engine

import com.petruvideo.core.model.AudioClip
import com.petruvideo.core.model.AudioOrigin
import com.petruvideo.core.model.Fingerprint
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.SpeedCurve
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.VideoClip
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.Test

class SplitVoiceTest {
    private fun media(id: String, kind: MediaKind, ms: Long) = MediaItem(
        id, kind, "content://$id", "$id.mp4", ms, 1280, 720, 30f, true, Fingerprint(1, 1, ms, "video/mp4"),
    )

    private val voice = media("voice", MediaKind.Audio, 4_000)
    private val backing = media("backing", MediaKind.Audio, 4_000)
    private val timeline = Timeline(
        "p",
        media = mapOf("m" to media("m", MediaKind.Video, 20_000)),
        videoLane = listOf(
            VideoClip("a", "m", 1_000, 5_000, true, volumePercent = 80),
            VideoClip("b", "m", 0, 3_000, true),
        ),
    )

    private fun ids(): () -> String {
        var n = 0
        return { "s${n++}" }
    }

    private fun applied(c: Command, t: Timeline = timeline) = (c.apply(t) as CommandResult.Applied).timeline

    @Test
    fun `voice and backing take the clips place length and volume and the video goes silent`() {
        val t = applied(SplitVoice("b", voice, backing, ids()))
        val start = timeline.videoStartOf("b")!!
        t.audioLane.map { it.mediaItemId } shouldBe listOf("voice", "backing")
        t.audioLane.forEach {
            it.timelineStartMs shouldBe start
            it.lengthMs shouldBe 3_000
            it.origin shouldBe AudioOrigin.Detached
        }
        t.videoLane[1].attachedAudio shouldBe false
        t.media.keys shouldBe setOf("m", "voice", "backing")
        TimelineInvariants.violations(t).shouldBeEmpty()
    }

    @Test
    fun `a trimmed clip at another speed keeps its length and the stems cover the trimmed range`() {
        val sped = timeline.copy(videoLane = listOf(VideoClip("a", "m", 1_000, 5_000, true, speedMilli = 2_000)))
        val t = applied(SplitVoice("a", voice, backing, ids()), sped)
        t.audioLane.forEach {
            it.sourceEndMs shouldBe 4_000
            it.speedMilli shouldBe 2_000
            it.lengthMs shouldBe 2_000
        }
        t.audioLane[0].lengthMs shouldBe t.videoLane[0].lengthMs
    }

    @Test
    fun `one undo step and refusals`() {
        SplitVoice("nope", voice, backing).apply(timeline) shouldBe CommandResult.Rejected(RejectReason.NotFound)
        val silent = timeline.copy(videoLane = listOf(VideoClip("a", "m", 0, 1_000, false)))
        SplitVoice("a", voice, backing).apply(silent) shouldBe CommandResult.Rejected(RejectReason.NoAttachedAudio)
        val reversed = timeline.copy(videoLane = listOf(VideoClip("a", "m", 0, 1_000, true, reversed = true)))
        SplitVoice("a", voice, backing).apply(reversed) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
        val curved = timeline.copy(
            videoLane = listOf(
                VideoClip(
                    "a", "m", 0, 1_000, true,
                    speedCurve = SpeedCurve.preset(com.petruvideo.core.model.SpeedPreset.Montage, 1_000),
                ),
            ),
        )
        SplitVoice("a", voice, backing).apply(curved) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
        SplitVoice("b", voice.copy(kind = MediaKind.Video), backing).apply(timeline) shouldBe
            CommandResult.Rejected(RejectReason.MissingMedia)
        val long = timeline.copy(videoLane = listOf(VideoClip("a", "m", 0, VOICE_SPLIT_MAX_MS + 1, true)))
        VoiceSplitRules.reasonNot(long, "a") shouldBe "Works on clips up to 5 minutes"
        VoiceSplitRules.reasonNot(timeline, "a") shouldBe null
    }

    @Test
    fun `it needs two free audio layers where the clip is`() {
        fun music(id: String) = AudioClip(id, "voice", 0, 9_000, timelineStartMs = 0, origin = AudioOrigin.Added)
        val two = timeline.copy(audioLane = listOf(music("x"), music("y")))
        SplitVoice("a", voice, backing).apply(two) shouldBe CommandResult.Rejected(RejectReason.LimitReached)
        val one = timeline.copy(audioLane = listOf(music("x")))
        applied(SplitVoice("a", voice, backing, ids()), one).audioLane.size shouldBe 3
    }
}
