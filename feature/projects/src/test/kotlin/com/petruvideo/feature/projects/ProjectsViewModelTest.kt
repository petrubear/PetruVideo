package com.petruvideo.feature.projects

import app.cash.turbine.test
import com.petruvideo.core.data.repo.ProjectFilter
import com.petruvideo.core.model.ProjectStatus
import io.kotest.matchers.collections.shouldContainExactly
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

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectsViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val sample = listOf(
        summary("a", "Coast road", modifiedAt = 500),
        summary("b", "Dive reel", ProjectStatus.Draft, modifiedAt = 400),
        summary("c", "Night market", ProjectStatus.Exported, modifiedAt = 300),
        summary("d", "Dive school", ProjectStatus.Exported, modifiedAt = 200),
        summary("e", "Hike", ProjectStatus.Draft, modifiedAt = 100),
    )

    private fun viewModel(
        repo: FakeProjectsRepository = FakeProjectsRepository(sample),
        thumbnails: RecordingThumbnails = RecordingThumbnails(),
    ) = ProjectsViewModel(repo, thumbnails)

    @Test
    fun `counts reflect all projects whatever the filter`() {
        val vm = viewModel()
        vm.state.value.counts shouldBe ProjectCounts(all = 5, drafts = 3, exported = 2)

        vm.onEvent(ProjectsEvent.SetFilter(ProjectFilter.Exported))

        vm.state.value.counts shouldBe ProjectCounts(all = 5, drafts = 3, exported = 2)
    }

    @Test
    fun `filter shows only matching projects`() {
        val vm = viewModel()
        vm.onEvent(ProjectsEvent.SetFilter(ProjectFilter.Drafts))
        vm.state.value.projects.map { it.project.id } shouldContainExactly listOf("a", "b", "e")

        vm.onEvent(ProjectsEvent.SetFilter(ProjectFilter.Exported))
        vm.state.value.projects.map { it.project.id } shouldContainExactly listOf("c", "d")
    }

    @Test
    fun `search narrows the list case-insensitively`() {
        val vm = viewModel()
        vm.onEvent(ProjectsEvent.SetQuery("dIVe"))
        vm.state.value.projects.map { it.project.id } shouldContainExactly listOf("b", "d")

        vm.onEvent(ProjectsEvent.SetQuery("zzz"))
        vm.state.value.projects shouldBe emptyList()
        vm.state.value.noResults shouldBe true
    }

    @Test
    fun `continue editing is the most recent project and ignores filter and search`() {
        val vm = viewModel()
        vm.state.value.continueEditing?.project?.id shouldBe "a"

        vm.onEvent(ProjectsEvent.SetFilter(ProjectFilter.Exported))
        vm.onEvent(ProjectsEvent.SetQuery("hike"))

        vm.state.value.continueEditing?.project?.id shouldBe "a"
    }

    @Test
    fun `no projects shows no continue card and is not a no-results state`() {
        val vm = viewModel(FakeProjectsRepository())
        vm.state.value.continueEditing shouldBe null
        vm.state.value.noResults shouldBe false
        vm.state.value.isLoading shouldBe false
    }

    @Test
    fun `rename trims the name and ignores blank names`() {
        val repo = FakeProjectsRepository(sample)
        val vm = viewModel(repo)
        vm.onEvent(ProjectsEvent.Rename("a", "  Coast  "))
        vm.onEvent(ProjectsEvent.Rename("b", "   "))
        repo.renamed shouldContainExactly listOf("a" to "Coast")
    }

    @Test
    fun `duplicate calls the repository`() {
        val repo = FakeProjectsRepository(sample)
        viewModel(repo).onEvent(ProjectsEvent.Duplicate("c"))
        repo.duplicated shouldContainExactly listOf("c")
    }

    @Test
    fun `delete needs confirmation`() {
        val repo = FakeProjectsRepository(sample)
        val vm = viewModel(repo)

        vm.onEvent(ProjectsEvent.RequestDelete("b"))
        vm.state.value.pendingDeleteId shouldBe "b"
        repo.deleted shouldBe emptyList()

        vm.onEvent(ProjectsEvent.CancelDelete)
        vm.state.value.pendingDeleteId shouldBe null
        repo.deleted shouldBe emptyList()

        vm.onEvent(ProjectsEvent.RequestDelete("b"))
        vm.onEvent(ProjectsEvent.ConfirmDelete)
        repo.deleted shouldContainExactly listOf("b")
        vm.state.value.pendingDeleteId shouldBe null
    }

    @Test
    fun `deleting the selected project clears the selection`() {
        val vm = viewModel()
        vm.onEvent(ProjectsEvent.SelectProject("b"))
        vm.onEvent(ProjectsEvent.RequestDelete("b"))
        vm.onEvent(ProjectsEvent.ConfirmDelete)
        vm.state.value.selectedProjectId shouldBe null
    }

    @Test
    fun `new project creates one and navigates to the editor`() = runTest {
        val repo = FakeProjectsRepository(sample)
        val vm = viewModel(repo)
        vm.effects.test {
            vm.onEvent(ProjectsEvent.NewProject)
            awaitItem() shouldBe ProjectsEffect.OpenEditor("new-1")
        }
        repo.created shouldBe 1
    }

    @Test
    fun `opening a project navigates to the editor`() = runTest {
        val vm = viewModel()
        vm.effects.test {
            vm.onEvent(ProjectsEvent.OpenProject("c"))
            awaitItem() shouldBe ProjectsEffect.OpenEditor("c")
        }
    }

    @Test
    fun `selecting a project is remembered`() {
        val vm = viewModel()
        vm.onEvent(ProjectsEvent.SelectProject("d"))
        vm.state.value.selectedProjectId shouldBe "d"
    }

    @Test
    fun `search can be opened and closing it clears the query`() {
        val vm = viewModel()
        vm.onEvent(ProjectsEvent.OpenSearch)
        vm.state.value.searchOpen shouldBe true
        vm.onEvent(ProjectsEvent.SetQuery("hike"))
        vm.onEvent(ProjectsEvent.CloseSearch)
        vm.state.value.searchOpen shouldBe false
        vm.state.value.query shouldBe ""
    }

    private fun clip(id: String) = com.petruvideo.core.model.MediaItem(
        id, com.petruvideo.core.model.MediaKind.Video, "content://x/$id", "$id.mp4", 1_000, 10, 10, 30f, true,
        com.petruvideo.core.model.Fingerprint(1, 1, 1_000, "video/mp4"),
    )

    @Test
    fun `the detail pane loads the media of the selected project, or of the latest one by default`() {
        val media = mapOf("a" to listOf(clip("m1")), "b" to listOf(clip("m2"), clip("m3")))
        val repo = FakeProjectsRepository(sample, media)
        val vm = viewModel(repo)
        vm.state.value.selectedMedia.map { it.id } shouldContainExactly listOf("m1")

        vm.onEvent(ProjectsEvent.SelectProject("b"))

        vm.state.value.selectedMedia.map { it.id } shouldContainExactly listOf("m2", "m3")
        vm.state.value.selectedProjectId shouldBe "b"
    }

    /** The picture pass checks files on the IO dispatcher, so the result arrives a moment later. */
    private fun settled(thumbnails: RecordingThumbnails, count: Int) {
        val end = System.currentTimeMillis() + 2_000
        while (thumbnails.refreshed.size < count && System.currentTimeMillis() < end) Thread.sleep(10)
        Thread.sleep(100)
    }

    private val noMedia = mapOf("a" to emptyList<com.petruvideo.core.model.MediaItem>(), "b" to emptyList(),
        "c" to emptyList(), "no-clips" to emptyList())

    @Test
    fun `a project with clips and no picture is asked for one, in list order, once`() {
        val thumbnails = RecordingThumbnails()
        val picture = java.io.File.createTempFile("picture", ".png").also { it.deleteOnExit() }
        val repo = FakeProjectsRepository(
            listOf(summary("a"), summary("b", thumbnail = picture.path), summary("c"), summary("no-clips", clips = 0)),
            noMedia,
        )
        viewModel(repo, thumbnails)
        settled(thumbnails, 2)

        thumbnails.refreshed shouldContainExactly listOf("a", "c")

        // The list changes again (a project was renamed): nothing is asked twice.
        repo.projects.value = repo.projects.value.map { it }
        settled(thumbnails, 3)
        thumbnails.refreshed shouldContainExactly listOf("a", "c")
    }

    @Test
    fun `a project whose picture file is gone is asked for one again`() {
        val thumbnails = RecordingThumbnails()
        val repo = FakeProjectsRepository(listOf(summary("a", thumbnail = "/does/not/exist.png")), noMedia)
        viewModel(repo, thumbnails)
        settled(thumbnails, 1)

        thumbnails.refreshed shouldContainExactly listOf("a")
    }

    @Test
    fun `a project that fails does not stop the others and is not retried`() {
        val thumbnails = RecordingThumbnails(failing = setOf("a"))
        val repo = FakeProjectsRepository(listOf(summary("a"), summary("b"), summary("c")), noMedia)
        viewModel(repo, thumbnails)
        settled(thumbnails, 3)

        thumbnails.refreshed shouldContainExactly listOf("a", "b", "c")
        repo.projects.value = repo.projects.value.map { it }
        settled(thumbnails, 4)
        thumbnails.refreshed shouldContainExactly listOf("a", "b", "c")
    }
}
