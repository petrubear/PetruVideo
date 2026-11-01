package com.petruvideo.core.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProjectEntity::class,
        MediaItemEntity::class,
        VideoClipEntity::class,
        AudioClipEntity::class,
        ExportSettingsEntity::class,
        ExportJobEntity::class,
        TextClipEntity::class,
        ExportRecordEntity::class,
        OverlayClipEntity::class,
        KeyframeEntity::class,
    ],
    version = 15,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 8, to = 9),
        AutoMigration(from = 9, to = 10),
        AutoMigration(from = 10, to = 11),
        AutoMigration(from = 11, to = 12),
        AutoMigration(from = 12, to = 13),
        AutoMigration(from = 13, to = 14),
        AutoMigration(from = 14, to = 15),
    ],
)
abstract class PetruDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun timelineDao(): TimelineDao
    abstract fun exportDao(): ExportDao

    companion object {
        const val NAME = "petruvideo.db"
    }
}
