package com.petruvideo.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProjectEntity::class,
        MediaItemEntity::class,
        VideoClipEntity::class,
        AudioClipEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class PetruDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun timelineDao(): TimelineDao

    companion object {
        const val NAME = "petruvideo.db"
    }
}
