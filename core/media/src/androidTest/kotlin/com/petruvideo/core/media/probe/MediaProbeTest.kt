package com.petruvideo.core.media.probe

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.error.AppError
import io.kotest.matchers.ints.shouldBeBetween
import io.kotest.matchers.longs.shouldBeBetween
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Test

class MediaProbeTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val probe = MediaProbe(context)

    private fun asset(name: String): Uri {
        val out = File(context.cacheDir, name)
        InstrumentationRegistry.getInstrumentation().context.assets.open(name).use { input ->
            out.outputStream().use { input.copyTo(it) }
        }
        return Uri.fromFile(out)
    }

    private fun probeOk(name: String): MediaItem = runBlocking { probe.probe(asset(name), id = name).getOrThrow() }

    @Test
    fun videoWithAudioHasDurationSizeFrameRateAndSound() {
        val item = probeOk("video_with_audio.mp4")
        item.kind shouldBe MediaKind.Video
        item.durationMs!!.shouldBeBetween(1_800L, 2_300L)
        item.width shouldBe 320
        item.height shouldBe 180
        item.frameRate!!.toInt().shouldBeBetween(29, 31)
        item.hasAudio shouldBe true
    }

    @Test
    fun silentVideoHasNoAudio() {
        probeOk("video_silent.mp4").hasAudio shouldBe false
    }

    @Test
    fun portraitVideoKeepsPortraitSize() {
        val item = probeOk("video_portrait.mp4")
        item.width shouldBe 180
        item.height shouldBe 320
    }

    @Test
    fun rotatedVideoReportsDisplaySizeAfterRotation() {
        val item = probeOk("video_rotated.mp4")
        item.width shouldBe 180
        item.height shouldBe 320
    }

    @Test
    fun photoHasSizeAndNoDuration() {
        val item = probeOk("photo.jpg")
        item.kind shouldBe MediaKind.Photo
        item.width shouldBe 320
        item.height shouldBe 180
        item.durationMs shouldBe null
        item.hasAudio shouldBe false
    }

    @Test
    fun audioFileHasDurationAndNoSize() {
        val item = probeOk("audio.mp3")
        item.kind shouldBe MediaKind.Audio
        item.durationMs!!.shouldBeBetween(1_800L, 2_400L)
        item.width shouldBe null
    }

    @Test
    fun fingerprintMatchesTheFile() {
        val uri = asset("video_with_audio.mp4")
        val item = runBlocking { probe.probe(uri, id = "x").getOrThrow() }
        item.fingerprint.sizeBytes shouldBe File(uri.path!!).length()
        item.fingerprint.mimeType shouldBe "video/mp4"
        item.fingerprint.durationMs shouldBe item.durationMs
    }

    @Test
    fun unreadableFileFailsWithUnsupportedFileNamingIt() {
        val error = runBlocking { probe.probe(asset("corrupted.mp4"), id = "bad") }.exceptionOrNull()
        error.shouldBeInstanceOf<MediaProbeException>()
        error.error shouldBe AppError.UnsupportedFile("corrupted.mp4")
    }
}
