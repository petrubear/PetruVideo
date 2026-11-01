package com.petruvideo.core.media.preview

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.petruvideo.core.media.pressure.PreviewQuality
import com.petruvideo.core.media.probe.MediaProbe
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.OverlayClip
import com.petruvideo.core.model.ProjectFormat
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.VideoClip
import io.kotest.matchers.shouldBe
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

/** Plays projects with overlay clips through the preview engine (feature 010). */
@OptIn(UnstableApi::class)
class OverlayPlaybackTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var engine: Media3PreviewEngine

    @Before
    fun setUp() {
        instrumentation.runOnMainSync { engine = Media3PreviewEngine(context) }
    }

    @After
    fun tearDown() = instrumentation.runOnMainSync { engine.release() }

    private fun item(name: String): MediaItem {
        val out = File(context.cacheDir, name)
        instrumentation.context.assets.open(name).use { i -> out.outputStream().use { i.copyTo(it) } }
        return runBlocking { MediaProbe(context).probe(Uri.fromFile(out), name).getOrThrow() }
    }

    private fun onMain(block: () -> Unit) = instrumentation.runOnMainSync(block)

    private fun waitFor(timeoutMs: Long = 30_000, condition: () -> Boolean) {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end && !condition()) Thread.sleep(50)
        check(condition()) { "state ${engine.state.value}" }
    }

    private fun project(base: MediaItem, extra: List<MediaItem>, vararg overlays: OverlayClip) = Timeline(
        projectId = "p", format = ProjectFormat(320, 180, 30),
        media = (listOf(base) + extra).associateBy { it.id },
        videoLane = listOf(VideoClip("c", base.id, 0, 2_000, attachedAudio = false)),
        overlayLane = overlays.toList(),
    )

    @Test
    fun aVideoOverlayAndAPhotoOverlayPlayOverTheMainVideoToTheEnd() {
        val base = item("video_silent.mp4")
        val photo = item("photo.jpg")
        val clip = item("video_with_audio.mp4")
        val timeline = project(
            base, listOf(photo, clip),
            OverlayClip("o1", photo.id, 0, 1_500, 200),
            OverlayClip("o2", clip.id, 0, 1_500, 400, xPermille = 300, widthPermille = 300),
        )
        onMain { engine.load(timeline, PreviewQuality.Full) }
        onMain { engine.play() }
        waitFor { engine.state.value.ended }
        engine.state.value.error shouldBe null
    }

    @Test
    fun anOverlayBeyondTheMainVideoLengthensThePreviewAndLiveChangesAreSafe() {
        val base = item("video_silent.mp4")
        val photo = item("photo.jpg")
        val overlay = OverlayClip("o1", photo.id, 0, 2_000, 1_500)
        onMain { engine.load(project(base, listOf(photo), overlay), PreviewQuality.Reduced) }
        engine.state.value.durationMs shouldBe 3_500
        onMain { engine.setOverlayLive(overlay.copy(xPermille = 100, opacityPercent = 40)) }
        Thread.sleep(300)
        onMain { engine.play() }
        waitFor { engine.state.value.positionMs > 300 }
        onMain { engine.setOverlayLive(overlay.copy(widthPermille = 600)) }
        waitFor { engine.state.value.ended }
        engine.state.value.error shouldBe null
    }

    @Test
    fun aVideoOnlyOverlayPlaysToTheEnd() {
        val base = item("video_silent.mp4")
        val clip = item("video_with_audio.mp4")
        onMain { engine.load(project(base, listOf(clip), OverlayClip("o2", clip.id, 0, 1_500, 400)), PreviewQuality.Full) }
        onMain { engine.play() }
        waitFor { engine.state.value.ended }
        engine.state.value.error shouldBe null
    }

    @Test
    fun aPhotoOnlyOverlayPlaysToTheEnd() {
        val base = item("video_silent.mp4")
        val photo = item("photo.jpg")
        onMain { engine.load(project(base, listOf(photo), OverlayClip("o1", photo.id, 0, 1_500, 200)), PreviewQuality.Full) }
        onMain { engine.play() }
        waitFor { engine.state.value.ended }
        engine.state.value.error shouldBe null
    }
}
