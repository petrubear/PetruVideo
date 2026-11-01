package com.petruvideo.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Embedded
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
    @ColumnInfo(defaultValue = "0") val muted: Boolean = false,
    @Embedded(prefix = "reverb_") val reverb: ReverbColumns = ReverbColumns(),
    @Embedded(prefix = "fx_") val fx: AudioFxColumns = AudioFxColumns(),
    @Embedded(prefix = "flt_") val filter: FilterColumns = FilterColumns(),
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
    @Embedded(prefix = "reverb_") val reverb: ReverbColumns = ReverbColumns(),
    @Embedded(prefix = "fx_") val fx: AudioFxColumns = AudioFxColumns(),
)

/** A clip's reverb stored as columns `reverb_enabled`, `reverb_preset`, and so on. */
data class ReverbColumns(
    @ColumnInfo(defaultValue = "0") val enabled: Boolean = false,
    @ColumnInfo(defaultValue = "'MediumRoom'") val preset: String = "MediumRoom",
    @ColumnInfo(defaultValue = "50") val room: Int = 50,
    @ColumnInfo(defaultValue = "1500") val decayMs: Int = 1500,
    @ColumnInfo(defaultValue = "50") val damping: Int = 50,
    @ColumnInfo(defaultValue = "20") val preDelayMs: Int = 20,
    @ColumnInfo(defaultValue = "30") val mix: Int = 30,
)

/** A clip's filter as columns `flt_look`, `flt_intensity`, and so on. */
data class FilterColumns(
    @ColumnInfo(defaultValue = "'None'") val look: String = "None",
    @ColumnInfo(defaultValue = "100") val intensity: Int = 100,
    @ColumnInfo(defaultValue = "0") val brightness: Int = 0,
    @ColumnInfo(defaultValue = "0") val contrast: Int = 0,
    @ColumnInfo(defaultValue = "0") val saturation: Int = 0,
    @ColumnInfo(defaultValue = "0") val warmth: Int = 0,
    @ColumnInfo(defaultValue = "0") val vignette: Int = 0,
)

/** A clip's fade, noise reduction, equalizer and voice effects as columns `fx_fadeInMs`, `fx_nrEnabled`, and so on. */
data class AudioFxColumns(
    @ColumnInfo(defaultValue = "0") val fadeInMs: Long = 0,
    @ColumnInfo(defaultValue = "0") val fadeOutMs: Long = 0,
    @ColumnInfo(defaultValue = "0") val nrEnabled: Boolean = false,
    @ColumnInfo(defaultValue = "'Medium'") val nrStrength: String = "Medium",
    @ColumnInfo(defaultValue = "0") val eqLow: Int = 0,
    @ColumnInfo(defaultValue = "0") val eqMid: Int = 0,
    @ColumnInfo(defaultValue = "0") val eqHigh: Int = 0,
    @ColumnInfo(defaultValue = "0") val echoEnabled: Boolean = false,
    @ColumnInfo(defaultValue = "250") val echoDelayMs: Int = 250,
    @ColumnInfo(defaultValue = "3") val echoRepeats: Int = 3,
    @ColumnInfo(defaultValue = "0") val robotEnabled: Boolean = false,
    @ColumnInfo(defaultValue = "60") val robotIntensity: Int = 60,
    @ColumnInfo(defaultValue = "0") val pitchEnabled: Boolean = false,
    @ColumnInfo(defaultValue = "4") val pitchSemitones: Int = 4,
    @ColumnInfo(defaultValue = "0") val bassEnabled: Boolean = false,
    @ColumnInfo(defaultValue = "50") val bassAmount: Int = 50,
)

@Entity(
    tableName = "text_clip",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["projectId", "startMs"])],
)
data class TextClipEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val startMs: Long,
    val durationMs: Long,
    val text: String,
    val x: Int,
    val y: Int,
    val size: Int,
    val font: String,
    val color: Int,
    val align: String,
    val bgEnabled: Boolean,
    val bgColor: Int,
    val bgOpacity: Int,
    val olEnabled: Boolean,
    val olColor: Int,
    val olWidth: Int,
    val inKind: String,
    val inMs: Int,
    val outKind: String,
    val outMs: Int,
)

@Entity(tableName = "export_settings")
data class ExportSettingsEntity(
    @PrimaryKey val projectId: String,
    val resolution: String,
    val frameRate: Int,
    val quality: String,
)

/** The latest export of a project; [state] and the result columns are the flat form of an export state. */
@Entity(tableName = "export_job")
data class ExportJobEntity(
    @PrimaryKey val projectId: String,
    val jobId: String,
    val startedAt: Long,
    val state: String,
    val progress: Int,
    val reason: String?,
    val resultUri: String?,
    val bytes: Long,
    val durationMs: Long,
)
