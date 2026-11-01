package com.petruvideo.feature.export

import com.petruvideo.core.data.repo.ExportRepository
import com.petruvideo.core.data.repo.StoredJob
import com.petruvideo.core.model.export.ExportRecord
import com.petruvideo.core.model.export.ExportSettings
import com.petruvideo.core.model.export.FrameRate
import com.petruvideo.core.model.export.Quality
import com.petruvideo.core.model.export.Resolution
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExportsViewModelTest {
    @After
    fun tearDown() = Dispatchers.resetMain()

    private class Repo(initial: List<ExportRecord> = emptyList()) : ExportRepository {
        val all = MutableStateFlow(initial)
        override suspend fun settings(projectId: String) = null
        override suspend fun saveSettings(projectId: String, settings: ExportSettings) = Unit
        override suspend fun job(projectId: String): StoredJob? = null
        override suspend fun saveJob(job: StoredJob) = Unit
        override suspend fun markInterrupted() = 0
        override fun records(): Flow<List<ExportRecord>> = all
        override suspend fun addRecord(record: ExportRecord) {
            all.value = (all.value + record).sortedByDescending { it.finishedAt }
        }
        override suspend fun removeRecord(id: String) {
            all.value = all.value.filterNot { it.id == id }
        }
    }

    private fun record(id: String, at: Long) = ExportRecord(
        id, "Trip $id", "content://media/$id", 10_000, 5_000_000,
        ExportSettings(Resolution.R1080, FrameRate.F30, Quality.Recommended), at,
    )

    private fun vm(repo: Repo, gone: Set<String> = emptySet()): ExportsViewModel {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        return ExportsViewModel(repo) { uri -> gone.none { uri.endsWith("/$it") } }
    }

    @Test
    fun `rows come newest first with their details and the empty state loads`() = runTest {
        val empty = vm(Repo())
        empty.state.value.loaded shouldBe true
        empty.state.value.rows shouldBe emptyList()
        val v = vm(Repo(listOf(record("b", 2_000), record("a", 1_000))))
        v.state.value.rows.map { it.record.id } shouldBe listOf("b", "a")
        v.state.value.rows.first().record.formatLabel shouldBe "1080p, 30 fps, Recommended"
    }

    @Test
    fun `a new export appears while the screen is open`() = runTest {
        val repo = Repo(listOf(record("a", 1_000)))
        val v = vm(repo)
        repo.addRecord(record("b", 3_000))
        v.state.value.rows.map { it.record.id } shouldBe listOf("b", "a")
    }

    @Test
    fun `remove takes the entry out and undo puts it back`() = runTest {
        val repo = Repo(listOf(record("a", 1_000), record("b", 2_000)))
        val v = vm(repo)
        v.onEvent(ExportsEvent.Remove("b"))
        v.state.value.rows.map { it.record.id } shouldBe listOf("a")
        v.state.value.undo?.id shouldBe "b"
        v.onEvent(ExportsEvent.Undo)
        v.state.value.rows.map { it.record.id } shouldBe listOf("b", "a")
        v.state.value.undo shouldBe null
        v.onEvent(ExportsEvent.Remove("a"))
        v.onEvent(ExportsEvent.DismissUndo)
        v.state.value.undo shouldBe null
        v.state.value.rows.map { it.record.id } shouldBe listOf("b")
    }

    @Test
    fun `a video that is gone is marked, cannot be opened or shared, but can be removed`() = runTest {
        val repo = Repo(listOf(record("a", 1_000), record("b", 2_000)))
        val v = vm(repo, gone = setOf("b"))
        v.state.value.rows.first { it.record.id == "b" }.missing shouldBe true
        v.state.value.rows.first { it.record.id == "a" }.missing shouldBe false
        v.onEvent(ExportsEvent.Open("b"))
        v.onEvent(ExportsEvent.Share("b"))
        v.onEvent(ExportsEvent.Open("a"))
        v.effects.first() shouldBe ExportsEffect.OpenVideo("content://media/a")
        v.onEvent(ExportsEvent.Remove("b"))
        v.state.value.rows.map { it.record.id } shouldBe listOf("a")
    }

    @Test
    fun `share sends the video location`() = runTest {
        val v = vm(Repo(listOf(record("a", 1_000))))
        v.onEvent(ExportsEvent.Share("a"))
        v.effects.first() shouldBe ExportsEffect.ShareVideo("content://media/a")
    }
}
