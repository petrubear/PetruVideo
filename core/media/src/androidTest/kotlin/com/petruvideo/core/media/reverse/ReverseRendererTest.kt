package com.petruvideo.core.media.reverse

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import io.kotest.matchers.shouldBe
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test

class ReverseRendererTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext

    private fun asset(name: String): File {
        val out = File(context.cacheDir, name)
        instrumentation.context.assets.open(name).use { i -> out.outputStream().use { i.copyTo(it) } }
        return out
    }

    private fun frame(file: File, ms: Long): Bitmap {
        val r = MediaMetadataRetriever().apply { setDataSource(file.absolutePath) }
        return r.getFrameAtTime(ms * 1000, MediaMetadataRetriever.OPTION_CLOSEST)!!.also { r.release() }
    }

    private fun distance(a: Bitmap, b: Bitmap): Long {
        var sum = 0L
        for (y in 0 until minOf(a.height, b.height) step 4) for (x in 0 until minOf(a.width, b.width) step 4) {
            val p = a.getPixel(x, y)
            val q = b.getPixel(x, y)
            sum += kotlin.math.abs(android.graphics.Color.red(p) - android.graphics.Color.red(q)) +
                kotlin.math.abs(android.graphics.Color.green(p) - android.graphics.Color.green(q)) +
                kotlin.math.abs(android.graphics.Color.blue(p) - android.graphics.Color.blue(q))
        }
        return sum
    }

    private fun durationMs(file: File): Long {
        val r = MediaMetadataRetriever().apply { setDataSource(file.absolutePath) }
        return r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toLong().also { r.release() }
    }

    @Test
    fun theCopyPlaysTheRangeBackwardsWithItsSound() = runBlocking<Unit> {
        val source = asset("video_with_audio.mp4")
        val length = durationMs(source)
        val out = File(context.cacheDir, "reversed-test.mp4").also { it.delete() }
        val progress = mutableListOf<Int>()
        val ok = ReverseRenderer(context).render(Uri.fromFile(source).toString(), 0, length, true, out) { progress += it }
        ok shouldBe true
        progress.last() shouldBe 100
        (kotlin.math.abs(durationMs(out) - length) < 400) shouldBe true
        val r = MediaMetadataRetriever().apply { setDataSource(out.absolutePath) }
        r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) shouldBe "yes"
        r.release()

        val early = frame(source, 150)
        val late = frame(source, length - 250)
        check(distance(early, late) > 0) { "the source never changes, the test cannot tell the order" }
        val revEarly = frame(out, 150)
        val revLate = frame(out, length - 250)
        check(distance(revEarly, late) < distance(revEarly, early)) { "the reversed start is not the source end" }
        check(distance(revLate, early) < distance(revLate, late)) { "the reversed end is not the source start" }
    }

    @Test
    fun aRangeWithoutSoundKeepsItSilent() = runBlocking<Unit> {
        val source = asset("video_silent.mp4")
        val out = File(context.cacheDir, "reversed-silent.mp4").also { it.delete() }
        ReverseRenderer(context).render(Uri.fromFile(source).toString(), 0, durationMs(source), false, out) { } shouldBe true
        val r = MediaMetadataRetriever().apply { setDataSource(out.absolutePath) }
        r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) shouldBe null
        r.release()
    }

    @Test
    fun cancellingLeavesNoFileBehind() = runBlocking<Unit> {
        val source = asset("video_with_audio.mp4")
        val out = File(context.cacheDir, "reversed-cancel.mp4").also { it.delete() }
        val job = launch {
            ReverseRenderer(context).render(Uri.fromFile(source).toString(), 0, durationMs(source), true, out) { }
        }
        job.cancel(CancellationException("test"))
        withTimeout(30_000) { job.join() }
        out.exists() shouldBe false
        File(out.parentFile, "${out.name}.work").exists() shouldBe false
    }
}
