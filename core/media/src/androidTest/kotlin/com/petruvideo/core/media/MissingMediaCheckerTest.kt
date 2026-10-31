package com.petruvideo.core.media

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.petruvideo.core.model.Availability
import com.petruvideo.core.model.Fingerprint
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import io.kotest.matchers.shouldBe
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Test

class MissingMediaCheckerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val checker = MissingMediaChecker(context)

    private fun item(id: String, file: File, fingerprintSize: Long = file.length()) = MediaItem(
        id = id, kind = MediaKind.Video, uri = Uri.fromFile(file).toString(), displayName = file.name,
        durationMs = 1_000, width = 10, height = 10, frameRate = 30f, hasAudio = true,
        fingerprint = Fingerprint(fingerprintSize, file.lastModified(), 1_000, "video/mp4"),
    )

    private fun file(name: String, bytes: Int) = File(context.cacheDir, name).apply { writeBytes(ByteArray(bytes)) }

    @Test
    fun existingUnchangedFilesAreAvailable() = runBlocking<Unit> {
        val f = file("present.mp4", 100)
        checker.check(listOf(item("a", f))) shouldBe mapOf("a" to Availability.Available)
    }

    @Test
    fun deletedFilesAreMissing() = runBlocking<Unit> {
        val f = file("gone.mp4", 100)
        val item = item("a", f)
        f.delete()
        checker.check(listOf(item)) shouldBe mapOf("a" to Availability.Missing)
    }

    @Test
    fun aFileWhoseSizeChangedIsMissing() = runBlocking<Unit> {
        val f = file("replaced.mp4", 100)
        val item = item("a", f, fingerprintSize = 50)
        checker.check(listOf(item)) shouldBe mapOf("a" to Availability.Missing)
    }

    @Test
    fun eachItemIsJudgedOnItsOwn() = runBlocking<Unit> {
        val ok = file("ok.mp4", 10)
        val bad = file("bad.mp4", 10).also { }
        val result = checker.check(listOf(item("ok", ok), item("bad", bad, fingerprintSize = 11)))
        result shouldBe mapOf("ok" to Availability.Available, "bad" to Availability.Missing)
    }
}
