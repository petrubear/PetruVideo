package com.petruvideo.core.media.preview

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.petruvideo.core.media.pressure.PreviewQuality
import com.petruvideo.core.media.probe.MediaProbe
import com.petruvideo.core.model.AnimKind
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.ProjectFormat
import com.petruvideo.core.model.TextAnimation
import com.petruvideo.core.model.TextClip
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.VideoClip
import io.kotest.matchers.shouldBe
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

/** Plays projects with text clips through the preview engine (feature 006). */
@OptIn(UnstableApi::class)
class TextPlaybackTest {
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
        condition() shouldBe true
    }

    private fun titled(video: MediaItem, vararg clips: TextClip) = Timeline(
        projectId = "p", format = ProjectFormat(320, 180, 30), media = mapOf(video.id to video),
        videoLane = listOf(VideoClip("c", video.id, 0, 2_000, attachedAudio = false)),
        textLane = clips.toList(),
    )

    @Test
    fun aProjectWithAnimatedTextPlaysToTheEnd() {
        val video = item("video_silent.mp4")
        val clip = TextClip(
            "t1", "Hello", 300, 1_200, animIn = TextAnimation(AnimKind.Fade, 300), animOut = TextAnimation(AnimKind.Pop, 300),
        )
        onMain { engine.load(titled(video, clip), PreviewQuality.Full) }
        onMain { engine.play() }
        waitFor { engine.state.value.ended }
        engine.state.value.error shouldBe null
    }

    @Test
    fun textBeyondTheVideoLengthensThePreview() {
        val video = item("video_silent.mp4")
        onMain { engine.load(titled(video, TextClip("t1", "Late", 1_500, 2_000)), PreviewQuality.Reduced) }
        engine.state.value.durationMs shouldBe 3_500
        onMain { engine.play() }
        waitFor { engine.state.value.ended }
        engine.state.value.error shouldBe null
    }

    @Test
    fun liveChangesWhilePausedAndPlayingDoNotBreakPlayback() {
        val video = item("video_silent.mp4")
        val clip = TextClip("t1", "Hello", 0, 2_000)
        onMain { engine.load(titled(video, clip), PreviewQuality.Full) }
        onMain { engine.setTextLive(clip.copy(text = "Changed while paused", sizePermille = 120)) }
        Thread.sleep(300)
        onMain { engine.play() }
        waitFor { engine.state.value.positionMs > 200 }
        onMain { engine.setTextLive(clip.copy(text = "Changed while playing", xPermille = 200)) }
        waitFor { engine.state.value.ended }
        engine.state.value.error shouldBe null
    }

    @Test
    fun aTextOnlyProjectPlays() {
        val t = Timeline(
            projectId = "p", format = ProjectFormat(320, 180, 30),
            textLane = listOf(TextClip("t1", "Only text", 0, 1_500)),
        )
        onMain { engine.load(t, PreviewQuality.Full) }
        engine.state.value.durationMs shouldBe 1_500
        onMain { engine.play() }
        waitFor { engine.state.value.ended }
        engine.state.value.error shouldBe null
    }
}
