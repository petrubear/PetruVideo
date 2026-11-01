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
    ],
    version = 5,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
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
