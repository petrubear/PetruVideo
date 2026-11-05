package com.petruvideo.core.data.repo

import androidx.room.withTransaction
import com.petruvideo.core.data.db.PetruDatabase
import com.petruvideo.core.data.db.ProjectSummaryRow
import com.petruvideo.core.data.db.canvas
import com.petruvideo.core.data.db.toEntity
import com.petruvideo.core.data.db.toModel
import com.petruvideo.core.data.db.withCanvas
import com.petruvideo.core.model.Project
import com.petruvideo.core.model.ProjectFormat
import com.petruvideo.core.model.ProjectStatus
import com.petruvideo.core.model.Timeline
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomProjectRepository @Inject constructor(
    private val db: PetruDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
    /** Told which files are still used after a project is deleted, so recordings nobody uses can go (FR-010). */
    private val onDeleted: suspend (stillUsedUris: Set<String>) -> Unit = {},
    /** The default name of a new project, from its number, in the language of the moment. */
    private val defaultName: (Int) -> String = { "Untitled $it" },
) : ProjectRepository {
    private val projects get() = db.projectDao()
    private val timelines get() = db.timelineDao()

    override fun observeProjects(filter: ProjectFilter, query: String): Flow<List<ProjectSummary>> {
        val status = when (filter) {
            ProjectFilter.All -> null
            ProjectFilter.Drafts -> ProjectStatus.Draft.name
            ProjectFilter.Exported -> ProjectStatus.Exported.name
        }
        return projects.observeSummaries(status, query.trim()).map { rows -> rows.map { it.toSummary() } }
    }

    override suspend fun loadProject(id: String): LoadedProject? {
        val entity = projects.get(id) ?: return null
        val media = timelines.mediaItems(id).map { it.toModel() }.associateBy { it.id }
        val project = entity.toModel()
        val frames = timelines.keyframes(id).groupBy({ it.ownerId }, { it.toModel() })
        val timeline = Timeline(
            projectId = id,
            format = project.format,
            media = media,
            videoLane = timelines.videoClips(id).map { it.toModel().copy(keyframes = frames[it.id].orEmpty()) },
            audioLane = timelines.audioClips(id).map { it.toModel() },
            textLane = timelines.textClips(id).map { it.toModel().withKeyframes(frames[it.id].orEmpty()) },
            overlayLane = timelines.overlayClips(id).map { it.toModel().withKeyframes(frames[it.id].orEmpty()) },
            playheadMs = entity.playheadMs,
            canvas = entity.canvas(),
        )
        return LoadedProject(project, timeline)
    }

    override suspend fun createProject(): Project {
        val now = clock()
        val project = Project(
            id = newId(),
            name = defaultName(projects.countUntitled() + 1),
            createdAt = now,
            modifiedAt = now,
        )
        projects.upsert(project.toEntity(playheadMs = 0))
        return project
    }

    override suspend fun saveTimeline(projectId: String, timeline: Timeline) {
        db.withTransaction {
            val current = projects.get(projectId) ?: return@withTransaction
            val format: ProjectFormat? = timeline.format
            projects.upsert(
                current.copy(
                    modifiedAt = clock(),
                    formatWidth = format?.width,
                    formatHeight = format?.height,
                    formatFps = format?.fps,
                    playheadMs = timeline.playheadMs,
                    durationMs = timeline.totalDurationMs,
                ).withCanvas(timeline.canvas),
            )
            timelines.deleteVideoClips(projectId)
            timelines.deleteAudioClips(projectId)
            timelines.deleteTextClips(projectId)
            timelines.deleteOverlayClips(projectId)
            timelines.deleteKeyframes(projectId)
            timelines.deleteMedia(projectId)
            val referenced = (
                timeline.videoLane.map { it.mediaItemId } + timeline.audioLane.map { it.mediaItemId } +
                    timeline.overlayLane.map { it.mediaItemId }
                ).toSet()
            timelines.insertMedia(
                timeline.media.values.filter { it.id in referenced }.map { it.toEntity(projectId) },
            )
            timelines.insertVideoClips(timeline.videoLane.mapIndexed { i, c -> c.toEntity(projectId, i) })
            timelines.insertAudioClips(timeline.audioLane.map { it.toEntity(projectId) })
            timelines.insertTextClips(timeline.textLane.map { it.toEntity(projectId) })
            timelines.insertOverlayClips(timeline.overlayLane.map { it.toEntity(projectId) })
            timelines.insertKeyframes(
                timeline.videoLane.flatMap { c -> c.keyframes.map { it.toEntity(projectId, c.id) } } +
                    timeline.overlayLane.flatMap { c -> c.keyframes.map { it.toEntity(projectId, c.id) } } +
                    timeline.textLane.flatMap { c -> c.keyframes.map { it.toEntity(projectId, c.id) } },
            )
        }
    }

    override suspend fun duplicateProject(id: String): Project? = db.withTransaction {
        val source = loadProject(id) ?: return@withTransaction null
        val now = clock()
        val copy = source.project.copy(
            id = newId(),
            name = "${source.project.name} copy",
            createdAt = now,
            modifiedAt = now,
            status = ProjectStatus.Draft,
            // The picture is the original's file; the copy gets its own when it is shown or opened.
            thumbnailRef = null,
        )
        projects.upsert(copy.toEntity(source.timeline.playheadMs))

        val mediaIds = source.timeline.media.keys.associateWith { newId() }
        val clipIds = (
            source.timeline.videoLane.map { it.id } + source.timeline.audioLane.map { it.id } +
                source.timeline.textLane.map { it.id } + source.timeline.overlayLane.map { it.id }
            )
            .associateWith { newId() }
        val timeline = source.timeline.copy(
            projectId = copy.id,
            media = source.timeline.media.values.associate {
                val newMediaId = mediaIds.getValue(it.id)
                newMediaId to it.copy(id = newMediaId)
            },
            videoLane = source.timeline.videoLane.map {
                it.copy(id = clipIds.getValue(it.id), mediaItemId = mediaIds.getValue(it.mediaItemId))
            },
            audioLane = source.timeline.audioLane.map {
                it.copy(id = clipIds.getValue(it.id), mediaItemId = mediaIds.getValue(it.mediaItemId))
            },
            textLane = source.timeline.textLane.map { it.copy(id = clipIds.getValue(it.id)) },
            overlayLane = source.timeline.overlayLane.map {
                it.copy(id = clipIds.getValue(it.id), mediaItemId = mediaIds.getValue(it.mediaItemId))
            },
        )
        saveTimeline(copy.id, timeline)
        copy
    }

    override suspend fun deleteProject(id: String) {
        val picture = projects.get(id)?.thumbnailRef
        projects.delete(id)
        picture?.let { runCatching { File(it).delete() } }
        onDeleted(timelines.allMediaUris().toSet())
    }

    override suspend fun rename(id: String, name: String) = projects.rename(id, name.trim().take(MAX_NAME), clock())

    override suspend fun setStatus(id: String, status: ProjectStatus) = projects.setStatus(id, status.name, clock())

    override suspend fun setThumbnail(id: String, ref: String?) = projects.setThumbnail(id, ref)

    private fun ProjectSummaryRow.toSummary() = ProjectSummary(
        project = project.toModel(),
        clipCount = clipCount,
        durationMs = project.durationMs.takeIf { it > 0 } ?: maxOf(videoLengthMs, audioEndMs),
        playheadMs = project.playheadMs,
    )

    private companion object {
        const val MAX_NAME = 80
    }
}
