package com.petruvideo.core.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration11To12Test : MigrationTestBase() {
    @Test
    fun existingAudioClipsOpenWithDuckingOffAtTheDefaults() {
        val name = "migration-11-12"
        helper.createDatabase(name, 11).apply {
            execSQL(
                "INSERT INTO project (id, name, createdAt, modifiedAt, status, playheadMs) " +
                    "VALUES ('p', 'P', 1, 1, 'Draft', 0)",
            )
            execSQL(
                "INSERT INTO media_item (id, projectId, kind, uri, displayName, hasAudio, fpSize, fpLastModified, " +
                    "fpMimeType) VALUES ('m', 'p', 'Audio', 'content://m', 'm', 1, 1, 1, 'audio/mp4')",
            )
            execSQL(
                "INSERT INTO audio_clip (id, projectId, mediaItemId, timelineStartMs, sourceStartMs, sourceEndMs, " +
                    "muted, origin) VALUES ('a', 'p', 'm', 0, 0, 1000, 0, 'Added')",
            )
            close()
        }
        val db = helper.runMigrationsAndValidate(name, 12, true)
        db.query("SELECT duckEnabled, duckAmount, duckSmoothness FROM audio_clip").use {
            it.moveToFirst()
            assertEquals(0, it.getInt(0))
            assertEquals(60, it.getInt(1))
            assertEquals("Medium", it.getString(2))
        }
    }
}
