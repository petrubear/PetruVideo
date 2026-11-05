package com.petruvideo.core.data.db

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class ProjectSummaryRow(
    @Embedded val project: ProjectEntity,
    val clipCount: Int,
    val videoLengthMs: Long,
    val audioEndMs: Long,
)

@Dao
interface ProjectDao {
    @Query(
        """
        SELECT p.*,
          (SELECT COUNT(*) FROM video_clip v WHERE v.projectId = p.id) AS clipCount,
          (SELECT COALESCE(SUM(((v.sourceEndMs - v.sourceStartMs) * 1000 + v.speedMilli / 2) / v.speedMilli), 0) FROM video_clip v WHERE v.projectId = p.id)
            AS videoLengthMs,
          (SELECT COALESCE(MAX(a.timelineStartMs + ((a.sourceEndMs - a.sourceStartMs) * 1000 + a.speedMilli / 2) / a.speedMilli), 0)
             FROM audio_clip a WHERE a.projectId = p.id) AS audioEndMs
        FROM project p
        WHERE (:status IS NULL OR p.status = :status)
          AND (:query = '' OR p.name LIKE '%' || :query || '%')
        ORDER BY p.modifiedAt DESC
        """,
    )
    fun observeSummaries(status: String?, query: String): Flow<List<ProjectSummaryRow>>

    @Query("SELECT * FROM project WHERE id = :id")
    suspend fun get(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(project: ProjectEntity)

    @Query("DELETE FROM project WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE project SET name = :name, modifiedAt = :now WHERE id = :id")
    suspend fun rename(id: String, name: String, now: Long)

    @Query("UPDATE project SET status = :status, modifiedAt = :now WHERE id = :id")
    suspend fun setStatus(id: String, status: String, now: Long)

    @Query("UPDATE project SET thumbnailRef = :ref WHERE id = :id")
    suspend fun setThumbnail(id: String, ref: String?)

    // The default name is made in the language of the moment, so both languages count toward the number.
    @Query("SELECT COUNT(*) FROM project WHERE name LIKE 'Untitled %' OR name LIKE 'Sin título %'")
    suspend fun countUntitled(): Int
}

@Dao
interface TimelineDao {
    @Query("SELECT * FROM media_item WHERE projectId = :projectId")
    suspend fun mediaItems(projectId: String): List<MediaItemEntity>

    @Query("SELECT * FROM video_clip WHERE projectId = :projectId ORDER BY orderIndex")
    suspend fun videoClips(projectId: String): List<VideoClipEntity>

    @Query("SELECT * FROM audio_clip WHERE projectId = :projectId ORDER BY timelineStartMs")
    suspend fun audioClips(projectId: String): List<AudioClipEntity>

    @Query("SELECT uri FROM media_item")
    suspend fun allMediaUris(): List<String>

    @Query("SELECT * FROM video_clip WHERE reversed = 1")
    suspend fun reversedClips(): List<VideoClipEntity>

    @Query("SELECT * FROM media_item")
    suspend fun allMediaItems(): List<MediaItemEntity>

    @Query("SELECT * FROM keyframe WHERE projectId = :projectId ORDER BY ownerId, timeMs")
    suspend fun keyframes(projectId: String): List<KeyframeEntity>

    @Insert
    suspend fun insertKeyframes(frames: List<KeyframeEntity>)

    @Query("DELETE FROM keyframe WHERE projectId = :projectId")
    suspend fun deleteKeyframes(projectId: String)

    @Query("SELECT * FROM overlay_clip WHERE projectId = :projectId ORDER BY startMs")
    suspend fun overlayClips(projectId: String): List<OverlayClipEntity>

    @Insert
    suspend fun insertOverlayClips(clips: List<OverlayClipEntity>)

    @Query("DELETE FROM overlay_clip WHERE projectId = :projectId")
    suspend fun deleteOverlayClips(projectId: String)

    @Query("SELECT * FROM text_clip WHERE projectId = :projectId ORDER BY startMs")
    suspend fun textClips(projectId: String): List<TextClipEntity>

    @Insert
    suspend fun insertTextClips(clips: List<TextClipEntity>)

    @Query("DELETE FROM text_clip WHERE projectId = :projectId")
    suspend fun deleteTextClips(projectId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(items: List<MediaItemEntity>)

    @Insert
    suspend fun insertVideoClips(clips: List<VideoClipEntity>)

    @Insert
    suspend fun insertAudioClips(clips: List<AudioClipEntity>)

    @Query("DELETE FROM video_clip WHERE projectId = :projectId")
    suspend fun deleteVideoClips(projectId: String)

    @Query("DELETE FROM audio_clip WHERE projectId = :projectId")
    suspend fun deleteAudioClips(projectId: String)

    @Query("DELETE FROM media_item WHERE projectId = :projectId")
    suspend fun deleteMedia(projectId: String)
}

@Dao
interface ExportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putSettings(settings: ExportSettingsEntity)

    @Query("SELECT * FROM export_settings WHERE projectId = :projectId")
    suspend fun settings(projectId: String): ExportSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putJob(job: ExportJobEntity)

    @Query("SELECT * FROM export_job WHERE projectId = :projectId")
    suspend fun job(projectId: String): ExportJobEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putRecord(record: ExportRecordEntity)

    @Query("SELECT * FROM export_record ORDER BY finishedAt DESC")
    fun observeRecords(): kotlinx.coroutines.flow.Flow<List<ExportRecordEntity>>

    @Query("DELETE FROM export_record WHERE id = :id")
    suspend fun deleteRecord(id: String)

    @Query("UPDATE export_job SET state = 'Failed', reason = 'Interrupted' WHERE state IN ('Waiting', 'Rendering')")
    suspend fun markInterrupted(): Int
}
