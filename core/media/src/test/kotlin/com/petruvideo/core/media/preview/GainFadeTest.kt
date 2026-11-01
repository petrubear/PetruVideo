package com.petruvideo.core.media.preview

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import com.petruvideo.core.model.Fade
import io.kotest.matchers.shouldBe
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Test
import kotlin.math.abs

class GainFadeTest {
    private val rate = 1_000
    private val level: Short = 10_000

    private fun processor(percent: Int = 100, fade: Fade = Fade(), lengthMs: Long = 1_000) =
        GainAudioProcessor(percent, fade, lengthMs).also {
            it.configure(AudioFormat(rate, 1, C.ENCODING_PCM_16BIT))
            it.flush(AudioProcessor.StreamMetadata.DEFAULT)
        }

    private fun run(p: GainAudioProcessor, frames: Int): ShortArray {
        val input = ByteBuffer.allocateDirect(frames * 2).order(ByteOrder.nativeOrder())
        repeat(frames) { input.putShort(level) }
        input.flip()
        p.queueInput(input)
        val out = p.output.order(ByteOrder.nativeOrder())
        return ShortArray(frames) { out.getShort() }
    }

    @Test
    fun `a fade in starts silent and reaches full level and a fade out ends silent`() {
        val out = run(processor(fade = Fade(200, 200)), 1_000)
        out[0] shouldBe 0.toShort()
        (abs(out[100] - level / 2) < 400) shouldBe true
        out[500] shouldBe level
        (out[999] < 100) shouldBe true
    }

    @Test
    fun `no fade leaves the level alone and volume multiplies the fade`() {
        run(processor(), 100).all { abs(it - level) <= 1 } shouldBe true
        val half = run(processor(percent = 50, fade = Fade(200, 0)), 400)
        (abs(half[300] - level / 2) <= 2) shouldBe true
    }

    @Test
    fun `the stream offset given at a flush moves the clip position`() {
        val p = processor(fade = Fade(200, 0))
        p.flush(AudioProcessor.StreamMetadata(500_000))
        val out = run(p, 100)
        out.all { abs(it - level) <= 1 } shouldBe true
        p.clipPositionMs shouldBe 600L
    }

    @Test
    fun `a flush without metadata restarts at the clip start`() {
        val p = processor(fade = Fade(200, 0))
        run(p, 300)
        p.flush()
        run(p, 10)[0] shouldBe 0.toShort()
    }

    @Test
    fun `volume and fade changes take effect on the next buffer`() {
        val p = processor()
        run(p, 100)[50] shouldBe level
        p.setPercent(0)
        run(p, 100).all { it == 0.toShort() } shouldBe true
        p.setPercent(100)
        p.setFade(Fade(0, 600), 1_000)
        p.flush(AudioProcessor.StreamMetadata(800_000))
        (run(p, 100)[90] < level / 2) shouldBe true
    }
}
