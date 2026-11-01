package com.petruvideo.core.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration12To13Test : MigrationTestBase() {
    @Test
    fun existingVideoClipsOpenWithNoTransform() {
        val name = "migration-12-13"
        helper.createDatabase(name, 12).apply {
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
        val db = helper.runMigrationsAndValidate(name, 13, true)
        db.query("SELECT xf_turns, xf_flipH, xf_flipV, xf_cropLeft, xf_cropTop, xf_cropRight, xf_cropBottom FROM video_clip").use {
            it.moveToFirst()
            assertEquals(listOf(0, 0, 0, 0, 0, 1000, 1000), (0..6).map { i -> it.getInt(i) })
        }
    }
}
