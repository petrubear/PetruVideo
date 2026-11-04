package com.petruvideo.core.media.composition

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
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
import org.junit.Test

/** The sound of overlay clips is built as extra audio-only sequences after the picture ones (feature 029). */
@OptIn(UnstableApi::class)
class OverlayAudioSequenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    private fun item(name: String): MediaItem {
        val out = File(context.cacheDir, name)
        instrumentation.context.assets.open(name).use { i -> out.outputStream().use { i.copyTo(it) } }
        return runBlocking { MediaProbe(context).probe(Uri.fromFile(out), name).getOrThrow() }
    }

    private val base = item("video_silent.mp4")
    private val clip = item("video_with_audio.mp4")
    private val photo = item("photo.jpg")

    private fun sequences(vararg overlays: OverlayClip): List<Set<Int>> {
        val timeline = Timeline(
            projectId = "p", format = ProjectFormat(320, 180, 30),
            media = listOf(base, clip, photo).associateBy { it.id },
            videoLane = listOf(VideoClip("c", base.id, 0, 2_000, attachedAudio = false)),
            overlayLane = overlays.toList(),
        )
        val spec = BuildSpec.forPreview(timeline, PreviewQuality.Full)
        val composition = CompositionBuilder(spec).build(timeline)!!
        return composition.sequences.map { sequence ->
            val item = sequence.editedMediaItems.first { it.mediaItem.localConfiguration != null }
            buildSet {
                if (!item.removeVideo) add(C.TRACK_TYPE_VIDEO)
                if (!item.removeAudio) add(C.TRACK_TYPE_AUDIO)
            }
        }
    }

    @Test
    fun aSoundingOverlayAddsOneAudioOnlySequenceAfterThePictureOnes() {
        val result = sequences(OverlayClip("o", clip.id, 0, 1_500, 400, attachedAudio = true))
        result.size shouldBe 3
        result[0] shouldBe setOf(C.TRACK_TYPE_VIDEO)
        result[2] shouldBe setOf(C.TRACK_TYPE_AUDIO)
    }

    @Test
    fun mutedSilentAndPhotoOverlaysAddNoSoundSequence() {
        val result = sequences(
            OverlayClip("a", clip.id, 0, 1_500, 0, attachedAudio = true, muted = true),
            OverlayClip("b", clip.id, 0, 1_500, 0),
            OverlayClip("c", photo.id, 0, 1_500, 0, attachedAudio = true),
        )
        result.size shouldBe 4
        result.none { it == setOf(C.TRACK_TYPE_AUDIO) } shouldBe true
    }
}
