package com.petruvideo.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "project")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
    val modifiedAt: Long,
    val status: String,
    val formatWidth: Int?,
    val formatHeight: Int?,
    val formatFps: Int?,
    val thumbnailRef: String?,
    val playheadMs: Long,
)

@Entity(
    tableName = "media_item",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("projectId")],
)
data class MediaItemEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val kind: String,
    val uri: String,
    val displayName: String,
    val durationMs: Long?,
    val width: Int?,
    val height: Int?,
    val frameRate: Float?,
    val hasAudio: Boolean,
    val fpSize: Long,
    val fpLastModified: Long,
    val fpMimeType: String,
)

@Entity(
    tableName = "video_clip",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = MediaItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["mediaItemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["projectId", "orderIndex"], unique = true),
        Index("mediaItemId"),
    ],
)
data class VideoClipEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val mediaItemId: String,
    val orderIndex: Int,
    val sourceStartMs: Long,
    val sourceEndMs: Long,
    val attachedAudio: Boolean,
    @ColumnInfo(defaultValue = "1000") val speedMilli: Int = 1000,
    @ColumnInfo(defaultValue = "100") val volumePercent: Int = 100,
)

@Entity(
    tableName = "audio_clip",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = MediaItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["mediaItemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["projectId", "timelineStartMs"]),
        Index("mediaItemId"),
    ],
)
data class AudioClipEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val mediaItemId: String,
    val timelineStartMs: Long,
    val sourceStartMs: Long,
    val sourceEndMs: Long,
    val muted: Boolean,
    val origin: String,
    @ColumnInfo(defaultValue = "1000") val speedMilli: Int = 1000,
    @ColumnInfo(defaultValue = "100") val volumePercent: Int = 100,
)
