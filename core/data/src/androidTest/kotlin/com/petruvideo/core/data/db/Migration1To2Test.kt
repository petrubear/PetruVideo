package com.petruvideo.core.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration1To2Test : MigrationTestBase() {
    @Test
    fun existingClipsGetDefaultSpeedAndVolume() {
        val name = "migration-1-2"
        helper.createDatabase(name, 1).apply {
            execSQL(
                "INSERT INTO project (id, name, createdAt, modifiedAt, status, playheadMs) " +
                    "VALUES ('p', 'P', 1, 1, 'Draft', 0)",
            )
            execSQL(
                "INSERT INTO media_item (id, projectId, kind, uri, displayName, hasAudio, fpSize, fpLastModified, " +
                    "fpMimeType) VALUES ('m', 'p', 'Video', 'content://m', 'm.mp4', 1, 1, 1, 'video/mp4')",
            )
            execSQL(
                "INSERT INTO video_clip (id, projectId, mediaItemId, orderIndex, sourceStartMs, sourceEndMs, " +
                    "attachedAudio) VALUES ('c', 'p', 'm', 0, 0, 5000, 1)",
            )
            execSQL(
                "INSERT INTO audio_clip (id, projectId, mediaItemId, timelineStartMs, sourceStartMs, sourceEndMs, " +
                    "muted, origin) VALUES ('a', 'p', 'm', 0, 0, 2000, 0, 'Added')",
            )
            close()
        }
        val db = helper.runMigrationsAndValidate(name, 2, true)
        for (table in listOf("video_clip", "audio_clip")) {
            db.query("SELECT speedMilli, volumePercent FROM $table").use {
                it.moveToFirst()
                assertEquals(1000, it.getInt(0))
                assertEquals(100, it.getInt(1))
            }
        }
    }
}
