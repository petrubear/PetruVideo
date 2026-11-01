package com.petruvideo.core.data.repo

import com.petruvideo.core.data.db.ExportDao
import com.petruvideo.core.data.db.ExportJobEntity
import com.petruvideo.core.data.db.ExportRecordEntity
import com.petruvideo.core.data.db.ExportSettingsEntity
import com.petruvideo.core.model.export.ExportRecord
import com.petruvideo.core.model.export.ExportSettings
import com.petruvideo.core.model.export.ExportState
import com.petruvideo.core.model.export.ExportStateCodec
import com.petruvideo.core.model.export.FrameRate
import com.petruvideo.core.model.export.Quality
import com.petruvideo.core.model.export.Resolution
import com.petruvideo.core.model.export.StoredState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** The latest export of a project, as kept between app runs. */
data class StoredJob(val projectId: String, val jobId: String, val startedAt: Long, val state: ExportState)

interface ExportRepository {
    suspend fun settings(projectId: String): ExportSettings?
    suspend fun saveSettings(projectId: String, settings: ExportSettings)
    suspend fun job(projectId: String): StoredJob?
    suspend fun saveJob(job: StoredJob)

    /** Adds a finished export to the history. */
    suspend fun addRecord(record: ExportRecord) = Unit

    /** The history, newest first. */
    fun records(): Flow<List<ExportRecord>> = flowOf(emptyList())

    /** Takes an entry out of the history; the video file is not touched. */
    suspend fun removeRecord(id: String) = Unit

    /** Marks every job still waiting or rendering as failed (the app was killed); returns how many. */
    suspend fun markInterrupted(): Int
}

class RoomExportRepository(private val dao: ExportDao) : ExportRepository {
    override suspend fun settings(projectId: String): ExportSettings? = dao.settings(projectId)?.let {
        ExportSettings(
            Resolution.entries.firstOrNull { r -> r.name == it.resolution } ?: Resolution.R1080,
            FrameRate.entries.firstOrNull { f -> f.fps == it.frameRate } ?: FrameRate.F30,
            Quality.entries.firstOrNull { q -> q.name == it.quality } ?: Quality.Recommended,
        )
    }

    override suspend fun saveSettings(projectId: String, settings: ExportSettings) {
        dao.putSettings(
            ExportSettingsEntity(projectId, settings.resolution.name, settings.frameRate.fps, settings.quality.name),
        )
    }

    override suspend fun job(projectId: String): StoredJob? = dao.job(projectId)?.let {
        val stored = StoredState(it.state, it.progress, it.reason, it.resultUri, it.bytes, it.durationMs)
        val state = ExportStateCodec.decode(stored)
        StoredJob(it.projectId, it.jobId, it.startedAt, state)
    }

    override suspend fun saveJob(job: StoredJob) {
        val s = ExportStateCodec.encode(job.state)
        dao.putJob(
            ExportJobEntity(
                job.projectId, job.jobId, job.startedAt, s.state, s.progress, s.reason, s.resultUri, s.bytes,
                s.durationMs,
            ),
        )
    }

    override suspend fun addRecord(record: ExportRecord) {
        dao.putRecord(
            ExportRecordEntity(
                record.id, record.projectName, record.uri, record.durationMs, record.bytes,
                record.settings.resolution.name, record.settings.frameRate.fps, record.settings.quality.name,
                record.finishedAt,
            ),
        )
    }

    override fun records(): Flow<List<ExportRecord>> = dao.observeRecords().map { rows ->
        rows.map {
            ExportRecord(
                it.id, it.projectName, it.uri, it.durationMs, it.bytes,
                ExportSettings(
                    Resolution.entries.firstOrNull { r -> r.name == it.resolution } ?: Resolution.R1080,
                    FrameRate.entries.firstOrNull { f -> f.fps == it.frameRate } ?: FrameRate.F30,
                    Quality.entries.firstOrNull { q -> q.name == it.quality } ?: Quality.Recommended,
                ),
                it.finishedAt,
            )
        }
    }

    override suspend fun removeRecord(id: String) = dao.deleteRecord(id)

    override suspend fun markInterrupted(): Int = dao.markInterrupted()
}
