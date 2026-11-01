package com.petruvideo.core.media.preview

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.petruvideo.core.media.pressure.PreviewQuality
import com.petruvideo.core.media.probe.MediaProbe
import com.petruvideo.core.model.Keyframe
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.OverlayClip
import com.petruvideo.core.model.Pose
import com.petruvideo.core.model.ProjectFormat
import com.petruvideo.core.model.TextClip
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.VideoClip
import io.kotest.matchers.shouldBe
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

/** Plays animated overlay and text clips through the preview engine (feature 012). */
@OptIn(UnstableApi::class)
class KeyframePlaybackTest {
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

    @Test
    fun animatedClipsPlayToTheEndAndLiveKeyframeChangesAreSafe() {
        val base = item("video_silent.mp4")
        val photo = item("photo.jpg")
        val frames = listOf(
            Keyframe(0, Pose(250, 300, 300, 100), ease = true),
            Keyframe(1_500, Pose(750, 700, 500, 40)),
        )
        val overlay = OverlayClip("o1", photo.id, 0, 2_000, 200).withKeyframes(frames)
        val text = TextClip("t1", "Hi", 0, 2_000).withKeyframes(
            listOf(Keyframe(0, Pose(200, 500, 60, 100)), Keyframe(1_800, Pose(800, 500, 120, 100))),
        )
        val timeline = Timeline(
            projectId = "p", format = ProjectFormat(320, 180, 30),
            media = mapOf(base.id to base, photo.id to photo),
            videoLane = listOf(VideoClip("c", base.id, 0, 2_000, attachedAudio = false)),
            overlayLane = listOf(overlay), textLane = listOf(text),
        )
        onMain { engine.load(timeline, PreviewQuality.Full) }
        onMain { engine.play() }
        waitFor { engine.state.value.positionMs > 300 }
        val moved = overlay.withKeyframes(frames + Keyframe(1_900, Pose(500, 500, 400, 100)))
        onMain { engine.setOverlayLive(moved) }
        onMain { engine.setTextLive(text.withKeyframes(text.keyframes.map { it.copy(ease = true) })) }
        waitFor { engine.state.value.ended }
        engine.state.value.error shouldBe null
    }
}
