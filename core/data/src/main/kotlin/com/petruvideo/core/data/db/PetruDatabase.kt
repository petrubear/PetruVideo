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
    ],
    version = 3,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
)
abstract class PetruDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun timelineDao(): TimelineDao

    companion object {
        const val NAME = "petruvideo.db"
    }
}
