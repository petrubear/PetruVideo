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
import com.petruvideo.core.model.AudioOrigin
import com.petruvideo.core.model.Availability
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.ProjectFormat
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.VideoClip
import io.kotest.matchers.longs.shouldBeBetween
import io.kotest.matchers.shouldBe
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Plays real timelines through [Media3PreviewEngine]. On an emulator this proves continuity, seeking, gaps and
 * mixing; the 30 fps and A/V sync (40 ms) budgets are measured on the reference device (Spike 1, T068).
 */
@OptIn(UnstableApi::class)
class PreviewEngineTest {
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

    private fun timeline(
        clips: List<Pair<MediaItem, Long>>,
        audio: List<AudioClip> = emptyList(),
        extra: List<MediaItem> = emptyList(),
    ) = Timeline(
        projectId = "p",
        format = ProjectFormat(320, 180, 30),
        media = (clips.map { it.first } + extra).associateBy { it.id },
        videoLane = clips.mapIndexed { i, (m, len) -> VideoClip("c$i", m.id, 0, len, attachedAudio = m.hasAudio) },
        audioLane = audio,
    )

    private fun onMain(block: () -> Unit) = instrumentation.runOnMainSync(block)

    private fun waitFor(timeoutMs: Long = 30_000, condition: () -> Boolean) {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end && !condition()) Thread.sleep(50)
        condition() shouldBe true
    }

    @Test
    fun playsContinuouslyAcrossClipBoundariesToTheEnd() {
        val a = item("video_with_audio.mp4")
        val p = item("video_portrait.mp4")
        onMain { engine.load(timeline(listOf(a to 2_000L, p to 2_000L, a.copy(id = "a2") to 2_000L)), PreviewQuality.Full) }
        engine.state.value.durationMs shouldBe 6_000

        onMain { engine.play() }
        val seen = mutableListOf<Long>()
        waitFor { engine.state.value.also { seen += it.positionMs }.ended }

        engine.state.value.error shouldBe null
        (seen == seen.sorted()) shouldBe true
        (seen.max() > 4_000) shouldBe true
    }

    @Test
    fun seekingLandsNearTheRequestedPosition() {
        val a = item("video_with_audio.mp4")
        onMain { engine.load(timeline(listOf(a to 2_000L, a.copy(id = "a2") to 2_000L)), PreviewQuality.Full) }
        onMain { engine.seekTo(3_000) }
        waitFor { engine.state.value.positionMs in 2_700..3_300 }
        engine.state.value.positionMs.shouldBeBetween(2_700, 3_300)
    }

    @Test
    fun aMissingClipBecomesAGapAndPlaybackStillFinishes() {
        val a = item("video_with_audio.mp4")
        val gone = a.copy(id = "gone", availability = Availability.Missing)
        onMain { engine.load(timeline(listOf(a to 2_000L, gone to 2_000L, a.copy(id = "a3") to 1_000L)), PreviewQuality.Full) }
        engine.state.value.durationMs shouldBe 5_000
        onMain { engine.play() }
        waitFor { engine.state.value.ended }
        engine.state.value.error shouldBe null
    }

    @Test
    fun audioOverlappingAnotherVideosSoundIsMixedWithoutError() {
        val a = item("video_with_audio.mp4")
        val song = item("audio.mp3")
        val clip = AudioClip("m", song.id, 0, 2_000, timelineStartMs = 500, origin = AudioOrigin.Added)
        onMain { engine.load(timeline(listOf(a to 2_000L), listOf(clip), listOf(song)), PreviewQuality.Full) }
        onMain { engine.play() }
        waitFor { engine.state.value.ended }
        engine.state.value.error shouldBe null
    }

    @Test
    fun aPhotoAndAMutedAudioClipPlayThrough() {
        val photo = item("photo.jpg")
        val song = item("audio.mp3")
        val muted = AudioClip("m", song.id, 0, 1_000, 0, muted = true, origin = AudioOrigin.Added)
        val t = Timeline(
            projectId = "p", format = ProjectFormat(320, 180, 30),
            media = mapOf(photo.id to photo, song.id to song),
            videoLane = listOf(VideoClip("ph", photo.id, 0, 2_000, attachedAudio = false)),
            audioLane = listOf(muted),
        )
        onMain { engine.load(t, PreviewQuality.Reduced) }
        onMain { engine.play() }
        waitFor { engine.state.value.ended }
        engine.state.value.error shouldBe null
    }

    @Test
    fun anEmptyTimelineCanBeLoadedSeekedAndPlayedWithoutCrashing() {
        onMain { engine.load(Timeline(projectId = "p"), PreviewQuality.Full) }
        onMain { engine.seekTo(500) }
        onMain { engine.play() }
        onMain { engine.pause() }
        engine.state.value.durationMs shouldBe 0
        engine.state.value.positionMs shouldBe 0
    }

    @Test
    fun reloadingKeepsThePosition() {
        val a = item("video_with_audio.mp4")
        onMain { engine.load(timeline(listOf(a to 2_000L, a.copy(id = "a2") to 2_000L)), PreviewQuality.Full) }
        onMain { engine.seekTo(1_500) }
        waitFor { engine.state.value.positionMs in 1_200..1_800 }
        onMain { engine.load(timeline(listOf(a to 2_000L, a.copy(id = "a2") to 2_000L, a.copy(id = "a3") to 1_000L)), PreviewQuality.Reduced) }
        waitFor { engine.state.value.positionMs in 1_000..2_000 }
    }
}
