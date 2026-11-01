package com.petruvideo.core.media.preview

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.petruvideo.core.media.pressure.PreviewQuality
import com.petruvideo.core.media.probe.MediaProbe
import com.petruvideo.core.model.AudioClip
import com.petruvideo.core.model.AudioFx
import com.petruvideo.core.model.AudioOrigin
import com.petruvideo.core.model.Bass
import com.petruvideo.core.model.Echo
import com.petruvideo.core.model.EqPreset
import com.petruvideo.core.model.EqPresets
import com.petruvideo.core.model.Fade
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.NoiseReduction
import com.petruvideo.core.model.NoiseStrength
import com.petruvideo.core.model.Pitch
import com.petruvideo.core.model.ProjectFormat
import com.petruvideo.core.model.Robot
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.VideoClip
import com.petruvideo.core.model.VoiceFx
import io.kotest.matchers.shouldBe
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

/** Plays clips with fades, noise reduction, equalizer, and voice effects through the preview engine (feature 005). */
@OptIn(UnstableApi::class)
class AudioFxPlaybackTest {
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

    private val everything = AudioFx(
        fade = Fade(500, 500),
        noise = NoiseReduction(true, NoiseStrength.High),
        eq = EqPresets.settings(EqPreset.Telephone),
        voice = VoiceFx(Echo(true, 200, 3), Robot(true, 80), Pitch(true, 5), Bass(true, 70)),
    )

    private fun musicTimeline(song: MediaItem, fx: AudioFx, lengthMs: Long = 2_000) = Timeline(
        projectId = "p",
        format = ProjectFormat(320, 180, 30),
        media = mapOf(song.id to song),
        audioLane = listOf(AudioClip("m", song.id, 0, lengthMs, 0, origin = AudioOrigin.Added, fx = fx)),
    )

    @Test
    fun aClipWithEveryToolOnPlaysToTheEnd() {
        val song = item("audio.mp3")
        onMain { engine.load(musicTimeline(song, everything), PreviewQuality.Full) }
        onMain { engine.play() }
        waitFor { engine.state.value.ended }
        engine.state.value.error shouldBe null
    }

    @Test
    fun aClipWithEveryToolOnAlsoPlaysWithTheLoweredQuality() {
        val song = item("audio.mp3")
        onMain { engine.load(musicTimeline(song, everything), PreviewQuality.Reduced) }
        onMain { engine.play() }
        waitFor { engine.state.value.ended }
        engine.state.value.error shouldBe null
    }

    @Test
    fun aVideoClipWithSoundAndToolsPlays() {
        val video = item("video_with_audio.mp4")
        val t = Timeline(
            projectId = "p", format = ProjectFormat(320, 180, 30), media = mapOf(video.id to video),
            videoLane = listOf(VideoClip("c", video.id, 0, 2_000, attachedAudio = true, fx = everything)),
        )
        onMain { engine.load(t, PreviewQuality.Full) }
        onMain { engine.play() }
        waitFor { engine.state.value.ended }
        engine.state.value.error shouldBe null
    }

    @Test
    fun theFadeKnowsTheClipPositionAfterASeek() {
        val song = item("audio.mp3")
        val clipLength = minOf(song.durationMs ?: 3_000L, 3_000L)
        onMain { engine.load(musicTimeline(song, AudioFx(fade = Fade(800, 0)), clipLength), PreviewQuality.Full) }
        onMain { engine.play() }
        waitFor { engine.state.value.positionMs > 200 }
        onMain { engine.seekTo(clipLength - 1_200) }
        waitFor { engine.state.value.positionMs > clipLength - 1_200 + 150 || engine.state.value.ended }
        val processor = engine.gainOf("m")!!
        // A short clip is processed to its end ahead of playback; with the seek offset honoured the processor's clip
        // time ends at the clip's length, whereas ignoring the offset would leave it near the 1.2 s that remained.
        val clipTime = processor.clipPositionMs
        check(clipTime in (clipLength - 400)..(clipLength + 100)) {
            "processor clip time $clipTime ms for a clip of $clipLength ms"
        }
    }

    @Test
    fun aLiveChangeDoesNotStopPlayback() {
        val song = item("audio.mp3")
        onMain { engine.load(musicTimeline(song, AudioFx(), minOf(song.durationMs ?: 3_000L, 3_000L)), PreviewQuality.Full) }
        onMain { engine.play() }
        waitFor { engine.state.value.positionMs > 200 }
        val before = engine.state.value.positionMs
        onMain { engine.setClipFxLive("m", everything, 2_000) }
        waitFor { engine.state.value.positionMs > before + 250 }
        engine.state.value.error shouldBe null
        engine.state.value.isPlaying shouldBe true
    }
}
