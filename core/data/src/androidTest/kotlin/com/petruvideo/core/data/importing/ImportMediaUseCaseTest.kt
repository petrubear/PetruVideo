package com.petruvideo.core.data.importing

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.petruvideo.core.media.probe.MediaProbe
import com.petruvideo.core.media.probe.MediaProbeException
import com.petruvideo.core.media.probe.MediaProber
import com.petruvideo.core.model.Availability
import com.petruvideo.core.model.Fingerprint
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.error.AppError
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.Test

class ImportMediaUseCaseTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun item(id: String, uri: Uri) = MediaItem(
        id = id, kind = MediaKind.Video, uri = uri.toString(), displayName = uri.lastPathSegment.orEmpty(),
        durationMs = 1_000, width = 100, height = 100, frameRate = 30f, hasAudio = true,
        fingerprint = Fingerprint(1, 1, 1_000, "video/mp4"), availability = Availability.Available,
    )

    /** Succeeds for every file except those whose name starts with "bad". */
    private val fakeProber = MediaProber { uri, id ->
        if (uri.lastPathSegment.orEmpty().startsWith("bad")) {
            Result.failure(MediaProbeException(AppError.UnsupportedFile(uri.lastPathSegment.orEmpty())))
        } else {
            Result.success(item(id, uri))
        }
    }

    private var counter = 0
    private fun useCase(prober: MediaProber = fakeProber) = ImportMediaUseCase(prober) { "id-${counter++}" }

    private fun uri(name: String) = Uri.parse("content://test/$name")

    @Test
    fun aPerFileErrorDoesNotStopTheBatchAndNamesTheFile() = runBlocking<Unit> {
        val result = useCase()(listOf(uri("a.mp4"), uri("bad.mp4"), uri("c.mp4")))

        result.items.map { it.displayName } shouldContainExactly listOf("a.mp4", "c.mp4")
        result.errors shouldContainExactly listOf(AppError.UnsupportedFile("bad.mp4"))
    }

    @Test
    fun itemsKeepTheSelectionOrder() = runBlocking<Unit> {
        val result = useCase()(listOf(uri("z.mp4"), uri("a.mp4"), uri("m.mp4")))
        result.items.map { it.displayName } shouldContainExactly listOf("z.mp4", "a.mp4", "m.mp4")
    }

    @Test
    fun progressIsReportedAfterEachFile() = runBlocking<Unit> {
        val seen = mutableListOf<ImportProgress>()
        useCase()(listOf(uri("a.mp4"), uri("b.mp4"), uri("c.mp4"))) { seen += it }
        seen.map { it.completed } shouldContainExactly listOf(1, 2, 3)
        seen.all { it.total == 3 } shouldBe true
    }

    @Test
    fun theSameFileTwiceCreatesTwoIndependentItems() = runBlocking<Unit> {
        val result = useCase()(listOf(uri("a.mp4"), uri("a.mp4")))
        result.items.size shouldBe 2
        result.items[0].id shouldNotBe result.items[1].id
        result.items[0].uri shouldBe result.items[1].uri
    }

    @Test
    fun cancellationStopsTheImportBetweenFiles() = runBlocking<Unit> {
        val firstStarted = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val slow = MediaProber { uri, id ->
            firstStarted.complete(Unit)
            release.await()
            Result.success(item(id, uri))
        }
        var probed = 0
        val counting = MediaProber { uri, id -> probed++; slow.probe(uri, id) }

        val job = async(start = CoroutineStart.DEFAULT) { useCase(counting)(List(5) { uri("f$it.mp4") }) }
        firstStarted.await()
        job.cancelAndJoin()

        job.isCancelled shouldBe true
        probed shouldBe 1
    }

    @Test
    fun originalFilesAreNeverModified() = runBlocking<Unit> {
        val ok = File(context.cacheDir, "orig.bin").apply { writeBytes(ByteArray(2048) { it.toByte() }) }
        val unreadable = File(context.cacheDir, "orig2.mp4").apply { writeBytes(ByteArray(512) { 7 }) }
        val before = listOf(ok, unreadable).map { checksum(it) }

        // The real probe fails on these files; either way nothing may be written to them.
        ImportMediaUseCase(MediaProbe(context))(listOf(Uri.fromFile(ok), Uri.fromFile(unreadable)))

        listOf(ok, unreadable).map { checksum(it) } shouldContainExactly before
    }

    private fun checksum(file: File) =
        MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }
}
