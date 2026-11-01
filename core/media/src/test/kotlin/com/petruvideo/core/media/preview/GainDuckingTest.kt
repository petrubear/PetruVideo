package com.petruvideo.core.media.preview

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import com.petruvideo.core.model.DuckEnvelope
import com.petruvideo.core.model.Fade
import io.kotest.matchers.shouldBe
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import org.junit.Test

class GainDuckingTest {
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

    // Full level for the first 100 ms, then half level.
    private val envelope = DuckEnvelope(100, floatArrayOf(1f, 1f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f))

    @Test
    fun `the envelope lowers the level where it says and leaves the rest`() {
        val p = processor().also { it.setEnvelope(envelope) }
        val out = run(p, 500)
        (abs(out[50] - level) <= 2) shouldBe true
        (abs(out[400] - level / 2) <= 2) shouldBe true
    }

    @Test
    fun `ducking multiplies volume and fade and never raises the level`() {
        val p = processor(percent = 50, fade = Fade(0, 0)).also { it.setEnvelope(envelope) }
        val out = run(p, 500)
        (abs(out[400] - level / 4) <= 2) shouldBe true
        out.all { it <= level / 2 + 2 } shouldBe true
    }

    @Test
    fun `after a seek the envelope is read at the new clip position`() {
        val p = processor().also { it.setEnvelope(envelope) }
        p.flush(AudioProcessor.StreamMetadata(400_000))
        val out = run(p, 100)
        out.all { abs(it - level / 2) <= 2 } shouldBe true
    }

    @Test
    fun `without an envelope the level is untouched`() {
        run(processor(), 100).all { abs(it - level) <= 1 } shouldBe true
    }
}
