package com.petruvideo.core.media.voicesplit

import com.petruvideo.core.media.reverse.Pcm
import io.kotest.matchers.shouldBe
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import org.junit.Test

class VoiceSplitDspTest {
    private fun tone(length: Int, hz: Double = 440.0, rate: Int = 44_100) =
        FloatArray(length) { (0.6 * sin(2.0 * PI * hz * it / rate)).toFloat() }

    @Test
    fun `a signal comes back from the transform and back`() {
        val stft = Stft()
        val signal = tone(30_000) + FloatArray(2_000) { ((it * 37) % 100 - 50) / 100f }
        val spectrum = stft.forward(signal)
        spectrum.frames shouldBe stft.frameCount(signal.size)
        val back = stft.inverse(spectrum, signal.size)
        val worst = signal.indices.maxOf { abs(signal[it] - back[it]) }
        (worst < 1e-3f) shouldBe true
    }

    @Test
    fun `a pure tone puts its energy at its own bin`() {
        val stft = Stft()
        val spectrum = stft.forward(tone(20_000, hz = 1_000.0))
        val frame = spectrum.frames / 2
        val magnitudes = FloatArray(stft.bins) { kotlin.math.hypot(spectrum.re[frame][it], spectrum.im[frame][it]) }
        val peak = magnitudes.indices.maxBy { magnitudes[it] }
        val expected = (1_000.0 * stft.nFft / 44_100).toInt()
        (abs(peak - expected) <= 1) shouldBe true
    }

    @Test
    fun `very short signals still transform`() {
        val stft = Stft()
        stft.inverse(stft.forward(FloatArray(10) { 0.1f }), 10).size shouldBe 10
        stft.frameCount(0) shouldBe 1
    }

    @Test
    fun `the wav file has a correct header and the samples`() {
        val file = File.createTempFile("split", ".wav")
        WavWriter(file, 44_100, 2).use { it.write(shortArrayOf(1, -2, 300, -400)) }
        val bytes = file.readBytes()
        String(bytes, 0, 4) shouldBe "RIFF"
        String(bytes, 8, 4) shouldBe "WAVE"
        bytes.size shouldBe 44 + 8
        (bytes[40].toInt() and 0xFF) shouldBe 8
        bytes[44].toInt() shouldBe 1
        bytes[46].toInt() shouldBe 0xFE.toByte().toInt()
        file.delete()
    }

    @Test
    fun `mono and other rates become stereo at 44100`() {
        val mono = toStereo44k(Pcm(ShortArray(4_410) { 1_000 }, 44_100, 1))
        mono.size shouldBe 8_820
        (mono[0] == mono[1] && mono[0].toInt() == 1_000) shouldBe true
        val stereo48 = toStereo44k(Pcm(ShortArray(2 * 4_800) { if (it % 2 == 0) 500 else -500 }, 48_000, 2))
        stereo48.size shouldBe 2 * 4_410
        (stereo48[100] > 0 && stereo48[101] < 0) shouldBe true
    }
}
