package com.petruvideo.feature.importmedia

import app.cash.turbine.test
import com.petruvideo.core.data.importing.ImportProgress
import com.petruvideo.core.data.importing.ImportResult
import com.petruvideo.core.data.importing.MediaImporter
import com.petruvideo.core.media.library.LibraryAlbum
import com.petruvideo.core.media.library.LibraryItem
import com.petruvideo.core.media.library.MediaLibrary
import com.petruvideo.core.model.Availability
import com.petruvideo.core.model.Fingerprint
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.PHOTO_DEFAULT_MS
import com.petruvideo.core.model.error.AppError
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddMediaViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun lib(kind: MediaKind, name: String, durationMs: Long? = 10_000) =
        LibraryItem("content://lib/$name", kind, name, durationMs, 0)

    private class FakeLibrary : MediaLibrary {
        val data = mutableMapOf<Pair<MediaKind, LibraryAlbum>, List<LibraryItem>>()
        val calls = mutableListOf<Pair<MediaKind, LibraryAlbum>>()
        override suspend fun items(kind: MediaKind, album: LibraryAlbum, limit: Int, offset: Int): List<LibraryItem> {
            calls += kind to album
            return if (offset == 0) data[kind to album].orEmpty() else emptyList()
        }
    }

    private fun library() = FakeLibrary().apply {
        data[MediaKind.Video to LibraryAlbum.Recent] =
            listOf(lib(MediaKind.Video, "a"), lib(MediaKind.Video, "b"), lib(MediaKind.Video, "c"))
        data[MediaKind.Video to LibraryAlbum.Camera] = listOf(lib(MediaKind.Video, "cam"))
        data[MediaKind.Photo to LibraryAlbum.Recent] = listOf(lib(MediaKind.Photo, "p1", null))
        data[MediaKind.Audio to LibraryAlbum.Recent] = listOf(lib(MediaKind.Audio, "song", 30_000))
    }

    private fun imported(uri: String): MediaItem {
        val name = uri.substringAfterLast('/')
        return MediaItem(
            id = "id-$name", kind = MediaKind.Video, uri = uri, displayName = name, durationMs = 10_000,
            width = 10, height = 10, frameRate = 30f, hasAudio = true,
            fingerprint = Fingerprint(1, 1, 10_000, "video/mp4"), availability = Availability.Available,
        )
    }

    private inner class FakeImporter(var failFor: Set<String> = emptySet()) : MediaImporter {
        var gate: CompletableDeferred<Unit>? = null
        var received: List<String> = emptyList()
        override suspend fun import(sources: List<String>, onProgress: (ImportProgress) -> Unit): ImportResult {
            received = sources
            gate?.await()
            val ok = sources.filterNot { s -> failFor.any { s.endsWith("/$it") } }
            val bad = sources.filter { s -> failFor.any { s.endsWith("/$it") } }
            sources.forEachIndexed { i, _ -> onProgress(ImportProgress(i + 1, sources.size)) }
            return ImportResult(
                ok.map(::imported),
                bad.map { AppError.UnsupportedFile(it.substringAfterLast('/')) },
            )
        }
    }

    private fun viewModel(
        lib: FakeLibrary = library(),
        importer: FakeImporter = FakeImporter(),
        access: MediaAccess = MediaAccess.Granted,
    ) = AddMediaViewModel(lib, importer).also { it.onEvent(AddMediaEvent.AccessChanged(access)) }

    @Test
    fun `items load for the first tab once access is granted`() {
        val vm = viewModel()
        vm.state.value.items.map { it.displayName } shouldContainExactly listOf("a", "b", "c")
    }

    @Test
    fun `no items load while access is not granted`() {
        val lib = library()
        val vm = viewModel(lib, access = MediaAccess.NotAsked)
        vm.state.value.items shouldBe emptyList()
        lib.calls shouldBe emptyList()

        vm.onEvent(AddMediaEvent.AccessChanged(MediaAccess.Partial))
        vm.state.value.items.size shouldBe 3
    }

    @Test
    fun `selection is numbered in tap order and renumbered on deselect`() {
        val vm = viewModel()
        vm.onEvent(AddMediaEvent.ToggleItem("content://lib/c"))
        vm.onEvent(AddMediaEvent.ToggleItem("content://lib/a"))
        vm.onEvent(AddMediaEvent.ToggleItem("content://lib/b"))
        vm.state.value.items.associate { it.displayName to it.selectionIndex } shouldBe
            mapOf("a" to 2, "b" to 3, "c" to 1)

        vm.onEvent(AddMediaEvent.ToggleItem("content://lib/c"))
        vm.state.value.items.associate { it.displayName to it.selectionIndex } shouldBe
            mapOf("a" to 1, "b" to 2, "c" to null)
    }

    @Test
    fun `selection persists across tab and album changes`() {
        val vm = viewModel()
        vm.onEvent(AddMediaEvent.ToggleItem("content://lib/a"))

        vm.onEvent(AddMediaEvent.SwitchTab(AddMediaTab.Photos))
        vm.onEvent(AddMediaEvent.ToggleItem("content://lib/p1"))
        vm.onEvent(AddMediaEvent.SwitchTab(AddMediaTab.Videos))
        vm.onEvent(AddMediaEvent.SwitchAlbum(LibraryAlbum.Camera))

        vm.state.value.selection.map { it.displayName } shouldContainExactly listOf("a", "p1")
        vm.state.value.items.map { it.displayName } shouldContainExactly listOf("cam")

        vm.onEvent(AddMediaEvent.SwitchAlbum(LibraryAlbum.Recent))
        vm.state.value.items.first { it.displayName == "a" }.selectionIndex shouldBe 1
    }

    @Test
    fun `totals count items and total duration with photos at their default length`() {
        val vm = viewModel()
        vm.onEvent(AddMediaEvent.ToggleItem("content://lib/a"))
        vm.onEvent(AddMediaEvent.SwitchTab(AddMediaTab.Photos))
        vm.onEvent(AddMediaEvent.ToggleItem("content://lib/p1"))

        vm.state.value.selection.size shouldBe 2
        vm.state.value.totalDurationMs shouldBe 10_000 + PHOTO_DEFAULT_MS
    }

    @Test
    fun `adding to the timeline imports in selection order and reports the items`() = runTest {
        val importer = FakeImporter()
        val vm = viewModel(importer = importer)
        vm.onEvent(AddMediaEvent.ToggleItem("content://lib/b"))
        vm.onEvent(AddMediaEvent.ToggleItem("content://lib/a"))

        vm.effects.test {
            vm.onEvent(AddMediaEvent.AddToTimelineInOrder)
            val done = awaitItem() as AddMediaEffect.Done
            done.items.map { it.displayName } shouldContainExactly listOf("b", "a")
            done.closeScreen shouldBe true
        }
        importer.received shouldContainExactly listOf("content://lib/b", "content://lib/a")
        vm.state.value.selection shouldBe emptyList()
        vm.state.value.importProgress shouldBe null
    }

    @Test
    fun `a partial failure adds the good files, names the bad one, and keeps the screen open`() = runTest {
        val vm = viewModel(importer = FakeImporter(failFor = setOf("b")))
        listOf("a", "b", "c").forEach { vm.onEvent(AddMediaEvent.ToggleItem("content://lib/$it")) }

        vm.effects.test {
            vm.onEvent(AddMediaEvent.AddToTimelineInOrder)
            val done = awaitItem() as AddMediaEffect.Done
            done.items.map { it.displayName } shouldContainExactly listOf("a", "c")
            done.closeScreen shouldBe false
        }
        vm.state.value.errors shouldContainExactly listOf(AppError.UnsupportedFile("b"))

        vm.onEvent(AddMediaEvent.DismissErrors)
        vm.state.value.errors shouldBe emptyList()
    }

    @Test
    fun `cancelling an import keeps the selection and adds nothing`() = runTest {
        val importer = FakeImporter().apply { gate = CompletableDeferred() }
        val vm = viewModel(importer = importer)
        vm.onEvent(AddMediaEvent.ToggleItem("content://lib/a"))

        vm.effects.test {
            vm.onEvent(AddMediaEvent.AddToTimelineInOrder)
            vm.state.value.importProgress shouldBe ImportProgress(0, 1)
            vm.onEvent(AddMediaEvent.CancelImport)
            expectNoEvents()
        }
        vm.state.value.importProgress shouldBe null
        vm.state.value.selection.size shouldBe 1
    }

    @Test
    fun `request access and the system picker are effects for the screen to perform`() = runTest {
        val vm = viewModel(access = MediaAccess.NotAsked)
        vm.effects.test {
            vm.onEvent(AddMediaEvent.RequestAccess)
            awaitItem() shouldBe AddMediaEffect.RequestAccess(MediaKind.Video)
            vm.onEvent(AddMediaEvent.SwitchTab(AddMediaTab.Audio))
            vm.onEvent(AddMediaEvent.OpenSystemPicker)
            awaitItem() shouldBe AddMediaEffect.LaunchSystemPicker(MediaKind.Audio)
        }
    }

    @Test
    fun `files from the system picker join the selection even without library access`() {
        val vm = viewModel(access = MediaAccess.Denied)
        vm.onEvent(AddMediaEvent.PickedFromSystem(listOf("content://picker/x", "content://picker/y")))
        vm.state.value.selection.map { it.displayName } shouldContainExactly listOf("x", "y")
    }

    @Test
    fun `closing asks the screen to close`() = runTest {
        val vm = viewModel()
        vm.effects.test {
            vm.onEvent(AddMediaEvent.Close)
            awaitItem() shouldBe AddMediaEffect.Close
        }
    }
}
