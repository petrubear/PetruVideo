package com.petruvideo.core.media.preview

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.petruvideo.core.media.pressure.PreviewQuality
import com.petruvideo.core.media.probe.MediaProbe
import com.petruvideo.core.model.ClipEffect
import com.petruvideo.core.model.EffectKind
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.OverlayClip
import com.petruvideo.core.model.ProjectFormat
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.Transition
import com.petruvideo.core.model.TransitionKind
import com.petruvideo.core.model.VideoClip
import io.kotest.matchers.shouldBe
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

/** Plays projects with transitions and clip effects through the preview engine (feature 011). */
@OptIn(UnstableApi::class)
class TransitionPlaybackTest {
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

    private fun project(a: MediaItem, b: MediaItem, kind: TransitionKind, vararg overlays: OverlayClip, extra: List<MediaItem> = emptyList()) =
        Timeline(
            projectId = "p", format = ProjectFormat(320, 180, 30),
            media = (listOf(a, b) + extra).associateBy { it.id },
            videoLane = listOf(
                VideoClip("c1", a.id, 0, 1_500, true, transitionOut = Transition(kind, 500)),
                VideoClip("c2", b.id, 0, 1_500, false, effect = ClipEffect(EffectKind.Blur, 40)),
            ),
            overlayLane = overlays.toList(),
        )

    @Test
    fun aTransitionPlaysToTheEndAndTheProjectIsShorterByItsLength() {
        val a = item("video_with_audio.mp4")
        val b = item("video_silent.mp4")
        onMain { engine.load(project(a, b, TransitionKind.Dissolve), PreviewQuality.Full) }
        engine.state.value.durationMs shouldBe 2_500
        onMain { engine.play() }
        waitFor { engine.state.value.positionMs > 400 }
        onMain { engine.setClipEffectLive("c2", ClipEffect(EffectKind.Blur, 90)) }
        waitFor { engine.state.value.ended }
        engine.state.value.error shouldBe null
    }

    @Test
    fun everyKindPlaysAndATransitionWorksTogetherWithAnOverlay() {
        val a = item("video_with_audio.mp4")
        val b = item("video_silent.mp4")
        val photo = item("photo.jpg")
        fun timeline(kind: TransitionKind) = project(
            a, b, kind,
            *(if (kind == TransitionKind.Slide) arrayOf(OverlayClip("o1", photo.id, 0, 1_000, 300)) else emptyArray()),
            extra = listOf(photo),
        )
        onMain { engine.load(timeline(TransitionKind.Dissolve), PreviewQuality.Full) }
        onMain { engine.play() }
        // Each change of kind rebuilds the composition while the preview plays.
        for (kind in TransitionKind.entries.drop(1)) {
            waitFor { engine.state.value.isPlaying }
            onMain { engine.load(timeline(kind), PreviewQuality.Full) }
            Thread.sleep(300)
            engine.state.value.error shouldBe null
        }
        waitFor { engine.state.value.ended }
        engine.state.value.error shouldBe null
    }
}
