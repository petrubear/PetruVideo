package com.petruvideo.core.model.engine

import com.petruvideo.core.model.AnimKind
import com.petruvideo.core.model.TextAnimation
import com.petruvideo.core.model.TextClip
import com.petruvideo.core.model.TextFont
import com.petruvideo.core.model.TextStyle
import com.petruvideo.core.model.Timeline
import io.kotest.matchers.shouldBe
import org.junit.Test

class TextCommandsTest {
    private val empty = Timeline("p")

    private fun applied(c: Command, t: Timeline) = c.apply(t) as CommandResult.Applied

    private fun withText(start: Long = 1_000, duration: Long = 4_000) =
        applied(AddText(start, { "t1" }), empty).timeline.let {
            it.copy(textLane = listOf(it.textLane.single().copy(startMs = start, durationMs = duration)))
        }

    @Test
    fun `add gives five seconds and the placeholder in a project without video`() {
        val clip = applied(AddText(3_000, { "t1" }), empty).timeline.textLane.single()
        clip.startMs shouldBe 3_000
        clip.durationMs shouldBe 5_000
        clip.text shouldBe "Your text"
    }

    @Test
    fun `add is shortened when the project ends sooner but not below half a second`() {
        val base = empty.copy(textLane = listOf(TextClip("x", "a", 0, 10_000)))
        applied(AddText(8_000, { "n" }), base).timeline.textLane.last { it.id == "n" }.durationMs shouldBe 2_000
        applied(AddText(9_900, { "n" }), base).timeline.textLane.last { it.id == "n" }.durationMs shouldBe 500
    }

    @Test
    fun `content style placement and animation commands change one clip each`() {
        val t = withText()
        applied(SetTextContent("t1", "Day one\nline two"), t)
            .timeline.textLane.single().text shouldBe "Day one\nline two"
        applied(SetTextStyle("t1", TextStyle(font = TextFont.Mono)), t)
            .timeline.textLane.single().style.font shouldBe TextFont.Mono
        applied(SetTextPlacement("t1", 9_999, -4, 5), t).timeline.textLane.single().let {
            it.xPermille shouldBe 1000
            it.yPermille shouldBe 0
            it.sizePermille shouldBe 20
        }
        applied(SetTextAnimation("t1", TextAnimation(AnimKind.Fade, 800), TextAnimation()), t)
            .timeline.textLane.single().animIn.kind shouldBe AnimKind.Fade
        SetTextContent("t1", t.textLane.single().text).apply(t) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
        SetTextContent("nope", "x").apply(t) shouldBe CommandResult.Rejected(RejectReason.NotFound)
    }

    @Test
    fun `move trim and limits`() {
        val t = withText(1_000, 4_000)
        applied(MoveText("t1", -50), t).timeline.textLane.single().startMs shouldBe 0
        applied(TrimText("t1", 500, 2_500), t).timeline.textLane.single().let {
            it.startMs shouldBe 500
            it.durationMs shouldBe 2_000
        }
        TrimText("t1", 1_000, 1_100).apply(t) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
        TrimText("t1", 0, 700_000).apply(t) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
    }

    @Test
    fun `split keeps text and style on both halves and clears the inner animations`() {
        val t = withText(1_000, 4_000).let {
            val c = it.textLane.single().copy(
                animIn = TextAnimation(AnimKind.Fade), animOut = TextAnimation(AnimKind.Pop),
                style = TextStyle(font = TextFont.Serif),
            )
            it.copy(textLane = listOf(c))
        }
        val ids = sequenceOf("x", "y").iterator()
        val (a, b) = applied(Split("t1", 3_000, ids::next), t).timeline.textLane
        a.durationMs shouldBe 2_000
        b.startMs shouldBe 3_000
        b.durationMs shouldBe 2_000
        a.animIn.kind shouldBe AnimKind.Fade
        a.animOut.kind shouldBe AnimKind.None
        b.animIn.kind shouldBe AnimKind.None
        b.animOut.kind shouldBe AnimKind.Pop
        a.style.font shouldBe TextFont.Serif
        Split("t1", 1_050, ids::next).apply(t) shouldBe CommandResult.Rejected(RejectReason.BelowMinimumLength)
    }

    @Test
    fun `duplicate goes right after and delete removes and the length follows`() {
        val t = withText(1_000, 4_000)
        val dup = applied(Duplicate("t1") { "t2" }, t).timeline
        dup.textLane.map { it.startMs } shouldBe listOf(1_000L, 5_000L)
        dup.totalDurationMs shouldBe 9_000
        applied(Delete("t2"), dup).timeline.totalDurationMs shouldBe 5_000
    }
}
