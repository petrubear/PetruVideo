package com.petruvideo.core.model.engine

import com.petruvideo.core.model.ClipFilter
import com.petruvideo.core.model.ClipTransform
import com.petruvideo.core.model.Fingerprint
import com.petruvideo.core.model.Keyframe
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.Pose
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.Transition
import com.petruvideo.core.model.TransitionKind
import com.petruvideo.core.model.VideoClip
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.Test

class FreezeCommandsTest {
    private val video = MediaItem(
        "m", MediaKind.Video, "content://m", "m.mp4", 20_000, 1920, 1080, 30f, true,
        Fingerprint(1, 1, 20_000, "video/mp4"),
    )
    private val still = MediaItem(
        "s", MediaKind.Photo, "file://s.jpg", "s.jpg", null, 1920, 1080, null, false,
        Fingerprint(1, 1, 0, "image/jpeg"),
    )
    private val clip = VideoClip(
        "a", "m", 0, 6_000, true, transform = ClipTransform(quarterTurns = 1),
        transitionOut = Transition(TransitionKind.Dissolve, 500),
    )
    private val other = VideoClip("b", "m", 0, 4_000, true)
    private val timeline = Timeline("p", media = mapOf("m" to video), videoLane = listOf(clip, other))

    private var next = 0
    private fun ids() = { "n${next++}" }
    private fun freeze(at: Long, length: Long = 2_000, t: Timeline = timeline) =
        FreezeFrame("a", at, length, still, ids()).apply(t)

    private fun applied(r: CommandResult) = (r as CommandResult.Applied).timeline

    @Test
    fun `a freeze in the middle cuts the clip and puts the still between the parts`() {
        val t = applied(freeze(2_000))
        t.videoLane.size shouldBe 4
        val first = t.videoLane[0]
        val frozen = t.videoLane[1]
        val second = t.videoLane[2]
        val last = t.videoLane[3]
        first.sourceEndMs shouldBe 2_000
        second.sourceStartMs shouldBe 2_000
        frozen.mediaItemId shouldBe "s"
        frozen.lengthMs shouldBe 2_000
        frozen.attachedAudio shouldBe false
        last.id shouldBe "b"
        t.media.containsKey("s") shouldBe true
        t.videoLengthMs shouldBe timeline.videoLengthMs + 2_000
        TimelineInvariants.violations(t).shouldBeEmpty()
    }

    @Test
    fun `the still looks like the clip at that moment and the transition stays on the last part`() {
        val t = applied(freeze(2_000))
        val first = t.videoLane[0]
        val frozen = t.videoLane[1]
        val second = t.videoLane[2]
        frozen.transform shouldBe clip.transform
        first.transitionOut shouldBe null
        frozen.transitionOut shouldBe null
        second.transitionOut shouldBe clip.transitionOut
    }

    @Test
    fun `a clip with a filter or keyframes gives the still the matching look`() {
        val filtered = clip.copy(filter = ClipFilter(intensity = 40))
        val animated = filtered.withKeyframes(
            listOf(Keyframe(0, com.petruvideo.core.model.FRAMING_NEUTRAL), Keyframe(4_000, Pose(700, 500, 2_000, 100))),
        )
        val t = applied(freeze(2_000, t = timeline.copy(videoLane = listOf(animated, other))))
        val frozen = t.videoLane[1]
        frozen.filter shouldBe animated.filter
        frozen.keyframes.size shouldBe 1
        frozen.poseAt(0) shouldBe animated.poseAt(2_000)
    }

    @Test
    fun `at the start or end the still goes before or after the clip with no empty part`() {
        val start = applied(freeze(0))
        start.videoLane.map { it.id }.drop(1) shouldBe listOf("a", "b")
        start.videoLane[0].mediaItemId shouldBe "s"
        val end = applied(freeze(6_000))
        end.videoLane.size shouldBe 3
        end.videoLane[1].mediaItemId shouldBe "s"
        end.videoLane[0].transitionOut shouldBe null
        end.videoLane[1].transitionOut shouldBe clip.transitionOut
        TimelineInvariants.violations(end).shouldBeEmpty()
    }

    @Test
    fun `lengths outside 0,5 to 10 seconds, photos and bad times are refused`() {
        freeze(2_000, 400) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
        freeze(2_000, 10_500) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
        freeze(7_000) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
        FreezeFrame("zz", 0, 2_000, still).apply(timeline) shouldBe CommandResult.Rejected(RejectReason.NotFound)
        FreezeFrame("a", 1_000, 2_000, video).apply(timeline) shouldBe CommandResult.Rejected(RejectReason.OutOfRange)
    }

    @Test
    fun `freezing a reversed clip freezes the frame the viewer sees`() {
        val reversed = clip.copy(reversed = true)
        val t = applied(freeze(2_000, t = timeline.copy(videoLane = listOf(reversed, other))))
        val first = t.videoLane[0]
        val second = t.videoLane[2]
        // Played backwards, the first part is the later part of the source.
        first.sourceStartMs shouldBe 4_000
        first.sourceEndMs shouldBe 6_000
        second.sourceEndMs shouldBe 4_000
        first.reversed shouldBe true
    }

    @Test
    fun `one undo step restores the original`() {
        val history = EditHistory(timeline)
        history.apply(freeze(2_000).let { r -> FreezeFrame("a", 2_000, 2_000, still, ids()) })
        history.undo()
        history.current.videoLane.map { it.id } shouldBe listOf("a", "b")
    }
}
