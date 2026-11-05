package com.petruvideo.feature.projects

import android.net.Uri
import app.cash.turbine.test
import com.petruvideo.core.media.probe.MediaProber
import com.petruvideo.core.model.Fingerprint
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.feature.projects.templates.TemplatesEffect
import com.petruvideo.feature.projects.templates.TemplatesViewModel
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.petruvideo.core.designsystem.UiText

@OptIn(ExperimentalCoroutinesApi::class)
class TemplatesViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun video(uri: String) = MediaItem(
        uri, MediaKind.Video, uri, "$uri.mp4", 8_000, 1920, 1080, 30f, true, Fingerprint(1, 1, 8_000, "video/mp4"),
    )

    private val prober = MediaProber { uri: Uri, id: String ->
        if (uri.toString().startsWith("bad")) Result.failure(IllegalStateException("no")) else Result.success(video(id))
    }

    @Test
    fun `using a template asks for as many files as it has slots`() = runTest {
        val vm = TemplatesViewModel(FakeProjectsRepository(), prober)
        vm.effects.test {
            vm.use("memories")
            awaitItem() shouldBe TemplatesEffect.PickMedia(4)
        }
    }

    @Test
    fun `picked files become a named project that opens in the editor and a cancel does nothing`() = runTest {
        val repo = FakeProjectsRepository()
        val vm = TemplatesViewModel(repo, prober)
        vm.effects.test {
            vm.use("memories")
            awaitItem() shouldBe TemplatesEffect.PickMedia(4)
            vm.picked(emptyList(), "Memories", "Memories")
            repo.created shouldBe 0
            vm.picked(listOf("a", "b", "c"), "Memories", "Memories")
            awaitItem() shouldBe TemplatesEffect.OpenEditor("new-1")
        }
        repo.saved shouldHaveSize 1
        repo.saved.single().second.videoLane shouldHaveSize 4
        repo.renamed.single() shouldBe ("new-1" to "Memories")
    }

    @Test
    fun `files that cannot be read give a message and no project`() = runTest {
        val repo = FakeProjectsRepository()
        val vm = TemplatesViewModel(repo, prober)
        vm.use("travel")
        vm.picked(listOf("bad1"), "Travel", "Travel")
        repo.created shouldBe 0
        vm.state.value.message shouldBe UiText.Res(R.string.projects_none_of_those_files_can)
        vm.state.value.busy shouldBe false
    }
}
