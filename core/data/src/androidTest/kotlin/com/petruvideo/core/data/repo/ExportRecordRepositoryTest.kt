package com.petruvideo.core.data.repo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.petruvideo.core.data.db.PetruDatabase
import com.petruvideo.core.model.export.ExportRecord
import com.petruvideo.core.model.export.ExportSettings
import com.petruvideo.core.model.export.FrameRate
import com.petruvideo.core.model.export.Quality
import com.petruvideo.core.model.export.Resolution
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

class ExportRecordRepositoryTest {
    private lateinit var db: PetruDatabase
    private lateinit var repo: RoomExportRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PetruDatabase::class.java).build()
        repo = RoomExportRepository(db.exportDao())
    }

    @After
    fun tearDown() = db.close()

    private fun record(id: String, at: Long, settings: ExportSettings) =
        ExportRecord(id, "Trip $id", "content://media/$id", 12_345, 9_876_543, settings, at)

    @Test
    fun recordsAreKeptNewestFirstWithAllTheirDetailsAndCanBeRemoved() = runBlocking<Unit> {
        repo.records().first() shouldBe emptyList()
        val old = record("a", 1_000, ExportSettings(Resolution.R720, FrameRate.F24, Quality.Smaller))
        val new = record("b", 2_000, ExportSettings(Resolution.R4K, FrameRate.F60, Quality.Higher))
        repo.addRecord(old)
        repo.addRecord(new)
        repo.records().first() shouldBe listOf(new, old)
        repo.removeRecord("b")
        repo.records().first() shouldBe listOf(old)
        repo.addRecord(new)
        repo.records().first() shouldBe listOf(new, old)
    }

    @Test
    fun recordsOutliveTheirProject() = runBlocking<Unit> {
        repo.addRecord(record("a", 1_000, ExportSettings(Resolution.R1080, FrameRate.F30, Quality.Recommended)))
        db.projectDao().delete("no-such-project")
        repo.records().first().size shouldBe 1
    }
}
