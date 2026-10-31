package com.petruvideo.core.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration2To3Test : MigrationTestBase() {
    @Test
    fun existingClipsAreNotMutedAndHaveReverbOff() {
        val name = "migration-2-3"
        helper.createDatabase(name, 2).apply {
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
                    "attachedAudio, speedMilli, volumePercent) VALUES ('c', 'p', 'm', 0, 0, 5000, 1, 2000, 40)",
            )
            execSQL(
                "INSERT INTO audio_clip (id, projectId, mediaItemId, timelineStartMs, sourceStartMs, sourceEndMs, " +
                    "muted, origin, speedMilli, volumePercent) VALUES ('a', 'p', 'm', 0, 0, 2000, 0, 'Added', 1000, 100)",
            )
            close()
        }
        val db = helper.runMigrationsAndValidate(name, 3, true)
        db.query("SELECT muted, reverb_enabled, reverb_preset, reverb_room, reverb_decayMs, speedMilli FROM video_clip")
            .use {
                it.moveToFirst()
                assertEquals(0, it.getInt(0))
                assertEquals(0, it.getInt(1))
                assertEquals("MediumRoom", it.getString(2))
                assertEquals(50, it.getInt(3))
                assertEquals(1500, it.getInt(4))
                assertEquals(2000, it.getInt(5))
            }
        db.query("SELECT reverb_enabled, reverb_mix FROM audio_clip").use {
            it.moveToFirst()
            assertEquals(0, it.getInt(0))
            assertEquals(30, it.getInt(1))
        }
    }
}
