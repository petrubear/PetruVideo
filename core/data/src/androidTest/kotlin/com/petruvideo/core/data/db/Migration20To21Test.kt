package com.petruvideo.core.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration20To21Test : MigrationTestBase() {
    @Test
    fun existingOverlaysAreNotMasked() {
        val name = "migration-20-21"
        helper.createDatabase(name, 20).apply {
            execSQL(
                "INSERT INTO project (id, name, createdAt, modifiedAt, status, playheadMs) " +
                    "VALUES ('p', 'P', 1, 1, 'Draft', 0)",
            )
            execSQL(
                "INSERT INTO media_item (id, projectId, kind, uri, displayName, hasAudio, fpSize, fpLastModified, " +
                    "fpMimeType) VALUES ('m', 'p', 'Video', 'content://m', 'm.mp4', 1, 1, 1, 'video/mp4')",
            )
            execSQL(
                "INSERT INTO overlay_clip (id, projectId, mediaItemId, startMs, sourceStartMs, sourceEndMs, x, y, " +
                    "width, opacity) VALUES ('o', 'p', 'm', 0, 0, 5000, 750, 250, 400, 100)",
            )
            close()
        }
        val db = helper.runMigrationsAndValidate(name, 21, true)
        db.query("SELECT maskKind, maskSize, maskFeather, maskInvert FROM overlay_clip").use {
            it.moveToFirst()
            assertEquals("None", it.getString(0))
            assertEquals(70, it.getInt(1))
            assertEquals(20, it.getInt(2))
            assertEquals(0, it.getInt(3))
        }
    }
}
