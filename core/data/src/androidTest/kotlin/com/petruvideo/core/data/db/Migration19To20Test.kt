package com.petruvideo.core.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration19To20Test : MigrationTestBase() {
    @Test
    fun existingClipsKeepNeutralAdjustments() {
        val name = "migration-19-20"
        helper.createDatabase(name, 19).apply {
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
                    "attachedAudio) VALUES ('c', 'p', 'm', 0, 0, 1000, 1)",
            )
            close()
        }
        val db = helper.runMigrationsAndValidate(name, 20, true)
        db.query(
            "SELECT flt_exposure, flt_gamma, flt_highlights, flt_shadows, flt_tint, flt_hue, flt_sharpen FROM video_clip",
        ).use {
            it.moveToFirst()
            for (i in 0..6) assertEquals(0, it.getInt(i))
        }
    }
}
