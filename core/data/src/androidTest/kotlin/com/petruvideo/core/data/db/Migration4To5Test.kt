package com.petruvideo.core.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration4To5Test : MigrationTestBase() {
    @Test
    fun existingClipsGetNeutralAudioEffects() {
        val name = "migration-4-5"
        helper.createDatabase(name, 4).apply {
            execSQL(
                "INSERT INTO project (id, name, createdAt, modifiedAt, status, playheadMs) " +
                    "VALUES ('p', 'P', 1, 1, 'Draft', 0)",
            )
            execSQL(
                "INSERT INTO media_item (id, projectId, kind, uri, displayName, hasAudio, fpSize, fpLastModified, " +
                    "fpMimeType) VALUES ('m', 'p', 'Video', 'content://m', 'm', 1, 1, 1, 'video/mp4')",
            )
            execSQL(
                "INSERT INTO video_clip (id, projectId, mediaItemId, orderIndex, sourceStartMs, sourceEndMs, " +
                    "attachedAudio) VALUES ('c', 'p', 'm', 0, 0, 1000, 1)",
            )
            close()
        }
        val db = helper.runMigrationsAndValidate(name, 5, true)
        db.query(
            "SELECT fx_fadeInMs, fx_fadeOutMs, fx_nrEnabled, fx_nrStrength, fx_eqLow, fx_echoDelayMs, fx_pitchSemitones " +
                "FROM video_clip WHERE id = 'c'",
        ).use {
            it.moveToFirst()
            assertEquals(0, it.getInt(0))
            assertEquals(0, it.getInt(1))
            assertEquals(0, it.getInt(2))
            assertEquals("Medium", it.getString(3))
            assertEquals(0, it.getInt(4))
            assertEquals(250, it.getInt(5))
            assertEquals(4, it.getInt(6))
        }
    }
}
