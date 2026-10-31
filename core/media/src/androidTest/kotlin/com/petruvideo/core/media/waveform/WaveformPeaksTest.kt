package com.petruvideo.core.media.waveform

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.petruvideo.core.media.probe.MediaProbe
import com.petruvideo.core.model.MediaItem
import io.kotest.matchers.shouldBe
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class WaveformPeaksTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var generator: WaveformPeaksGenerator

    @Before
    fun setUp() {
        File(context.cacheDir, "waveforms").deleteRecursively()
        generator = WaveformPeaksGenerator(context)
    }

    private fun probe(name: String): MediaItem {
        val out = File(context.cacheDir, name)
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        assets.open(name).use { input -> out.outputStream().use { input.copyTo(it) } }
        return runBlocking { MediaProbe(context).probe(Uri.fromFile(out), name).getOrThrow() }
    }

    @Test
    fun peaksAreNormalisedAndMatchTheDuration() = runBlocking<Unit> {
        val item = probe("audio.mp3")
        val peaks = generator.peaks(item)!!
        val expected = item.durationMs!! / 1000.0 * WaveformPeaksGenerator.PEAKS_PER_SECOND
        (peaks.size in (expected * 0.9).toInt()..(expected * 1.1).toInt() + 1) shouldBe true
        (peaks.all { it in 0f..1f }) shouldBe true
        peaks.max().let { it > 0.1f } shouldBe true
    }

    @Test
    fun peaksAreComputedOnceAndThenCachedByFingerprint() = runBlocking<Unit> {
        val item = probe("video_with_audio.mp4")
        val first = generator.peaks(item)!!
        generator.decodeCount shouldBe 1

        val second = WaveformPeaksGenerator(context).peaks(item)!! // new instance: must come from the cache file
        second.toList() shouldBe first.toList()
        generator.peaks(item)
        generator.decodeCount shouldBe 1
    }

    @Test
    fun aSilentVideoHasNoWaveform() = runBlocking<Unit> {
        generator.peaks(probe("video_silent.mp4")) shouldBe null
        generator.decodeCount shouldBe 0
    }

    @Test
    fun anUnreadableFileGivesNoPeaksInsteadOfCrashing() = runBlocking<Unit> {
        val item = probe("audio.mp3").copy(uri = Uri.fromFile(File(context.cacheDir, "missing.mp3")).toString())
        generator.peaks(item.copy(fingerprint = item.fingerprint.copy(sizeBytes = 1))) shouldBe null
    }
}
