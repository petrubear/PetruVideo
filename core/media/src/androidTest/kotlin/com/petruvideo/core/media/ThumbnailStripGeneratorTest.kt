package com.petruvideo.core.media

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.petruvideo.core.media.probe.MediaProbe
import com.petruvideo.core.media.thumbs.ThumbnailStripGenerator
import io.kotest.matchers.shouldBe
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class ThumbnailStripGeneratorTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun clean() {
        File(context.cacheDir, "thumbs").deleteRecursively()
    }

    private fun probe(name: String) = runBlocking {
        val out = File(context.cacheDir, name)
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        assets.open(name).use { input -> out.outputStream().use { input.copyTo(it) } }
        MediaProbe(context).probe(Uri.fromFile(out), name).getOrThrow()
    }

    @Test
    fun videoFramesAreScaledAndCachedPerStep() = runBlocking<Unit> {
        val generator = ThumbnailStripGenerator(context)
        val video = probe("video_with_audio.mp4")

        val first = generator.frame(video, 0, heightPx = 64)!!
        first.height shouldBe 64
        generator.extractionCount shouldBe 1

        generator.frame(video, 400, heightPx = 64) // same one-second step: served from the cache
        generator.extractionCount shouldBe 1

        generator.frame(video, 1_200, heightPx = 64)
        generator.extractionCount shouldBe 2
        ThumbnailStripGenerator(context).frame(video, 0, heightPx = 64)!!.height shouldBe 64
    }

    @Test
    fun photosAndAudio() = runBlocking<Unit> {
        val generator = ThumbnailStripGenerator(context)
        (generator.frame(probe("photo.jpg"), 0, heightPx = 64)!!.height <= 180) shouldBe true
        generator.frame(probe("audio.mp3"), 0) shouldBe null
    }
}
