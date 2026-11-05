package com.petruvideo.core.media.composition

import com.petruvideo.core.model.EffectKind
import com.petruvideo.core.model.TransitionKind
import io.kotest.matchers.shouldBe
import org.junit.Test
import kotlin.math.abs

class TransitionPoseTest {
    private val steps = listOf(0f, 0.1f, 0.25f, 0.5f, 0.75f, 0.9f, 1f)
    private val original = listOf(
        TransitionKind.Dissolve, TransitionKind.FadeBlack, TransitionKind.Slide, TransitionKind.Cover,
        TransitionKind.Zoom,
    )
    private val added = TransitionKind.entries - original.toSet()

    /** Every progress step with both clips and both drawing orders. */
    private val combos = steps.flatMap { p ->
        listOf(true, false).flatMap { outgoing -> listOf(true, false).map { onTop -> Triple(p, outgoing, onTop) } }
    }

    /** The values the five original kinds produced before this feature, written out again from their definition. */
    private fun expectedOriginal(kind: TransitionKind, p: Float, outgoing: Boolean, onTop: Boolean): TransitionPose {
        val own = if (outgoing) 1f - p else p
        val side = if (outgoing) -2f * p else 2f * (1f - p)
        return when (kind) {
            TransitionKind.Dissolve -> TransitionPose(if (onTop) own else 1f)
            TransitionKind.FadeBlack ->
                TransitionPose((if (outgoing) 1f - 2f * p else 2f * p - 1f).coerceAtLeast(0f))
            TransitionKind.Slide -> TransitionPose(1f, x = side)
            TransitionKind.Cover -> TransitionPose(1f, x = if (!onTop) 0f else side)
            TransitionKind.Zoom ->
                TransitionPose(if (onTop) own else 1f, zoomX = 1f + 0.3f * (if (outgoing) p else 1f - p))
            else -> error("not original")
        }
    }

    @Test
    fun `the five original transitions keep the first positions and their poses`() {
        TransitionKind.entries.take(5) shouldBe original
        for (kind in original) for ((p, outgoing, onTop) in combos) {
            TransitionPoses.of(kind, p, outgoing, onTop) shouldBe expectedOriginal(kind, p, outgoing, onTop)
        }
    }

    @Test
    fun `the nine original effects keep the first positions`() {
        EffectKind.entries.take(9) shouldBe listOf(
            EffectKind.Blur, EffectKind.Pixelate, EffectKind.Mirror, EffectKind.Shake, EffectKind.Glitch,
            EffectKind.RgbSplit, EffectKind.Glow, EffectKind.Vhs, EffectKind.Grain,
        )
        EffectKind.entries.size shouldBe 19
        TransitionKind.entries.size shouldBe 15
    }

    private fun isIdentity(pose: TransitionPose) =
        pose.alpha == 1f && pose.x == 0f && pose.y == 0f && pose.zoomX == 1f && pose.zoomY == 1f &&
            pose.rotationDegrees == 0f

    private fun hidden(pose: TransitionPose) =
        pose.alpha == 0f || abs(pose.x) >= 2f || abs(pose.y) >= 2f || pose.zoomX <= 0.01f || pose.zoomY <= 0.01f

    @Test
    fun `each new transition starts on the first clip and ends on the second`() {
        for (kind in added) for (onTop in listOf(true, false)) {
            val start = TransitionPoses.of(kind, 0f, outgoing = true, onTop = onTop)
            val end = TransitionPoses.of(kind, 1f, outgoing = false, onTop = onTop)
            isIdentity(start) shouldBe true
            isIdentity(end) shouldBe true
        }
    }

    @Test
    fun `the second clip is hidden or out of the frame at the start when it is drawn on top`() {
        for (kind in added) {
            hidden(TransitionPoses.of(kind, 0f, outgoing = false, onTop = true)) shouldBe true
        }
    }

    @Test
    fun `the first clip is gone at the end when it is drawn on top`() {
        for (kind in added) {
            hidden(TransitionPoses.of(kind, 1f, outgoing = true, onTop = true)) shouldBe true
        }
    }

    @Test
    fun `poses stay in sane bounds at every step`() {
        for (kind in TransitionKind.entries) for ((p, outgoing, onTop) in combos) {
            val pose = TransitionPoses.of(kind, p, outgoing, onTop)
            (pose.alpha in 0f..1f) shouldBe true
            (pose.zoomX > 0f && pose.zoomX <= 2f) shouldBe true
            (pose.zoomY > 0f && pose.zoomY <= 2f) shouldBe true
            (abs(pose.x) <= 2f && abs(pose.y) <= 2f) shouldBe true
            (abs(pose.rotationDegrees) <= 360f) shouldBe true
        }
    }

    @Test
    fun `the new transitions move to the directions they are named for`() {
        (TransitionPoses.of(TransitionKind.PushUp, 0.5f, true, true).y > 0f) shouldBe true
        (TransitionPoses.of(TransitionKind.PushDown, 0.5f, true, true).y < 0f) shouldBe true
        (TransitionPoses.of(TransitionKind.PushRight, 0.5f, true, true).x > 0f) shouldBe true
        (TransitionPoses.of(TransitionKind.Slide, 0.5f, true, true).x < 0f) shouldBe true
    }

    @Test
    fun `the whip pan is quicker in the middle than the plain slide`() {
        val whip = abs(TransitionPoses.of(TransitionKind.WhipPan, 0.25f, true, true).x)
        val slide = abs(TransitionPoses.of(TransitionKind.Slide, 0.25f, true, true).x)
        (whip < slide) shouldBe true
        val midWhip = abs(TransitionPoses.of(TransitionKind.WhipPan, 0.5f, true, true).x)
        midWhip shouldBe 1f
    }
}
