package com.petruvideo.core.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration9To10Test : MigrationTestBase() {
    @Test
    fun existingClipsHaveNoTransitionOrEffect() {
        val name = "migration-9-10"
        helper.createDatabase(name, 9).apply {
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
        val db = helper.runMigrationsAndValidate(name, 10, true)
        db.query("SELECT trn_kind, trn_lengthMs, fxe_kind, fxe_strength FROM video_clip WHERE id = 'c'").use {
            it.moveToFirst()
            assertTrue(it.isNull(0))
            assertEquals(0, it.getInt(1))
            assertTrue(it.isNull(2))
            assertEquals(50, it.getInt(3))
        }
    }
}
