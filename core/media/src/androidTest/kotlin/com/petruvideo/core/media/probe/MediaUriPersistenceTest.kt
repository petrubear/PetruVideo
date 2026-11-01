package com.petruvideo.core.media.probe

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import io.kotest.matchers.shouldBe
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

/**
 * Spike 2 (research 6): a media URI stored in a project must still open after the process is killed and the app
 * restarts. Run the two steps as separate instrumentations with `am force-stop` in between (see research.md).
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class MediaUriPersistenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val remembered = File(context.filesDir, "remembered-uri.txt")

    @Test
    fun step1_insertMediaStoreVideoAndRememberItsUri() {
        val values = ContentValues().apply {
            // A name of its own, so a file an earlier run left behind never blocks the insert.
            put(MediaStore.Video.Media.DISPLAY_NAME, "spike-${System.nanoTime()}.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/PetruSpike")
        }
        val uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)!!
        context.contentResolver.openOutputStream(uri)!!.use { out ->
            InstrumentationRegistry.getInstrumentation().context.assets.open("video_with_audio.mp4").use { it.copyTo(out) }
        }
        remembered.writeText(uri.toString())
        runBlocking { MediaProbe(context).probe(uri, "x").isSuccess } shouldBe true
    }

    @Test
    fun step2_theRememberedUriStillOpensAndProbesAfterARestart() {
        val uri = Uri.parse(remembered.readText())
        context.contentResolver.openInputStream(uri)!!.use { it.read() >= 0 } shouldBe true
        runBlocking { MediaProbe(context).probe(uri, "x").isSuccess } shouldBe true
    }
}
