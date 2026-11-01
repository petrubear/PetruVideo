package com.petruvideo.core.media.freeze

import android.content.Context
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import com.petruvideo.core.media.probe.MediaProbe
import com.petruvideo.core.model.MediaKind
import io.kotest.matchers.shouldBe
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Test

class FreezeStillsTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext

    @Test
    fun aFrameBecomesAPhotoOfTheSameSizeInTheAppsStorage() = runBlocking<Unit> {
        val file = File(context.cacheDir, "freeze-source.mp4")
        instrumentation.context.assets.open("video_silent.mp4").use { i -> file.outputStream().use { i.copyTo(it) } }
        val probe = MediaProbe(context)
        val video = probe.probe(Uri.fromFile(file), "v").getOrThrow()
        val still = FreezeStills(context, probe).make(video, 500)!!
        still.kind shouldBe MediaKind.Photo
        still.hasAudio shouldBe false
        still.width shouldBe video.width
        still.height shouldBe video.height
        Uri.parse(still.uri).path!!.startsWith(File(context.filesDir, "freezes").path) shouldBe true
        File(Uri.parse(still.uri).path!!).delete()
    }

    @Test
    fun anUnreadableSourceGivesNoStillAndLeavesNoFile() = runBlocking<Unit> {
        val probe = MediaProbe(context)
        val before = File(context.filesDir, "freezes").listFiles()?.size ?: 0
        val broken = com.petruvideo.core.model.MediaItem(
            "x", MediaKind.Video, "file:///does/not/exist.mp4", "x.mp4", 1_000, 100, 100, 30f, false,
            com.petruvideo.core.model.Fingerprint(0, 0, 0, "video/mp4"),
        )
        FreezeStills(context, probe).make(broken, 0) shouldBe null
        (File(context.filesDir, "freezes").listFiles()?.size ?: 0) shouldBe before
    }
}
