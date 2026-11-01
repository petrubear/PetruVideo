package com.petruvideo.core.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration21To22Test : MigrationTestBase() {
    @Test
    fun theFirstEffectIsKeptAndTheOthersAreEmpty() {
        val name = "migration-21-22"
        helper.createDatabase(name, 21).apply {
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
                    "attachedAudio, fxe_kind, fxe_strength) VALUES ('c', 'p', 'm', 0, 0, 1000, 1, 'Blur', 70)",
            )
            close()
        }
        val db = helper.runMigrationsAndValidate(name, 22, true)
        db.query("SELECT fxe_kind, fxe_strength, fxe2_kind, fxe3_kind FROM video_clip").use {
            it.moveToFirst()
            assertEquals("Blur", it.getString(0))
            assertEquals(70, it.getInt(1))
            assertNull(it.getString(2))
            assertNull(it.getString(3))
        }
    }
}
