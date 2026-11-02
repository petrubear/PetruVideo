package com.petruvideo.core.media.voicesplit

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.petruvideo.core.media.probe.MediaProbe
import com.petruvideo.core.model.Fingerprint
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import io.kotest.matchers.shouldBe
import java.io.File
import java.security.MessageDigest
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test

class VoiceSplitServiceTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    /** A steady two-note chord, like a plucked instrument: no voice in it. */
    private fun chordWav(seconds: Int): MediaItem {
        val rate = 44_100
        val file = File(context.cacheDir, "chord-$seconds.wav")
        WavWriter(file, rate, 2).use { writer ->
            val samples = ShortArray(rate * 2)
            repeat(seconds) { second ->
                for (i in 0 until rate) {
                    val t = (second * rate + i).toDouble() / rate
                    val v = (0.25 * sin(2 * PI * 220 * t) + 0.2 * sin(2 * PI * 330 * t)) * Short.MAX_VALUE
                    samples[2 * i] = v.toInt().toShort()
                    samples[2 * i + 1] = v.toInt().toShort()
                }
                writer.write(samples)
            }
        }
        return MediaItem(
            "chord", MediaKind.Audio, Uri.fromFile(file).toString(), "chord.wav", seconds * 1_000L, null, null, null,
            true, Fingerprint(file.length(), file.lastModified(), seconds * 1_000L, "audio/wav"),
        )
    }

    private fun read(file: File): ShortArray {
        val bytes = file.readBytes()
        return ShortArray((bytes.size - 44) / 2) { i ->
            ((bytes[44 + 2 * i].toInt() and 0xFF) or (bytes[45 + 2 * i].toInt() shl 8)).toShort()
        }
    }

    private fun energy(samples: ShortArray) = samples.sumOf { it.toDouble() * it } / samples.size

    @Test
    fun theBundledModelIsTheOneThePlanNames() {
        val bytes = context.assets.open(UmxVocalsModel.ASSET).use { it.readBytes() }
        bytes.size shouldBe 17_820_856
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) } shouldBe
            "3d05709ff7197bbd4a33aee759e4e82002acb491f41c94770227ab57507b6ccb"
    }

    @Test
    fun twoBlocksOfAChordSplitIntoStemsThatAddUpToTheOriginal() = runBlocking {
        val store = VoiceSplitStore(File(context.cacheDir, "voicesplit-test").apply { deleteRecursively() })
        val service = VoiceSplitService(context, store, MediaProbe(context))
        val media = chordWav(20)
        val progress = mutableListOf<Int>()
        val started = System.nanoTime()
        val files = withTimeout(300_000) { service.splitFiles(media, 0, 20_000) { progress += it } }!!
        val seconds = (System.nanoTime() - started) / 1e9
        Log.i("VoiceSplitTest", "20 s of audio split in $seconds s")

        val voice = read(files.voice)
        val backing = read(files.backing)
        val original = read(File(Uri.parse(media.uri).path!!))
        voice.size shouldBe original.size
        backing.size shouldBe original.size
        // Voice plus Backing is the original (the rounding of each stem to 16 bits is the only difference).
        val worst = original.indices.maxOf { abs(voice[it] + backing[it] - original[it]) }
        check(worst <= 1) { "stems differ from the original by $worst" }
        // A chord has no voice: the voice stem must carry far less than the rest.
        check(energy(voice) < energy(backing) * 0.5) { "voice ${energy(voice)} against backing ${energy(backing)}" }
        check(progress.isNotEmpty() && progress.last() == 100 && progress == progress.sorted()) { "progress $progress" }

        // The same range again is reused.
        val again = System.nanoTime()
        service.splitFiles(media, 0, 20_000) { }!!.voice.absolutePath shouldBe files.voice.absolutePath
        check((System.nanoTime() - again) / 1e9 < 1.0) { "the second split was not instant" }
    }

    @Test
    fun cancellingLeavesNoFilesBehind() = runBlocking {
        val root = File(context.cacheDir, "voicesplit-cancel").apply { deleteRecursively() }
        val service = VoiceSplitService(context, VoiceSplitStore(root), MediaProbe(context))
        val media = chordWav(20)
        val job = launch { service.splitFiles(media, 0, 20_000) { } }
        kotlinx.coroutines.delay(300)
        job.cancel()
        job.join()
        ((root.listFiles() ?: emptyArray()).toList().map { it.name } shouldBe emptyList())
        Unit
    }
}
