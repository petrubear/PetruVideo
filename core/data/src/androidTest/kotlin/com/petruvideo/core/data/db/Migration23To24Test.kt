package com.petruvideo.core.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration23To24Test : MigrationTestBase() {
    @Test
    fun existingClipsKeepTheCompressorOffWithTheVoiceSettings() {
        val name = "migration-23-24"
        helper.createDatabase(name, 23).apply {
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
        val db = helper.runMigrationsAndValidate(name, 24, true)
        db.query(
            "SELECT fx_cmpEnabled, fx_cmpPreset, fx_cmpThreshold, fx_cmpRatioTenths, fx_cmpAttackMs, " +
                "fx_cmpReleaseMs, fx_cmpLevel FROM video_clip",
        ).use {
            it.moveToFirst()
            assertEquals(0, it.getInt(0))
            assertEquals("Voice", it.getString(1))
            assertEquals(-22, it.getInt(2))
            assertEquals(49, it.getInt(3))
            assertEquals(10, it.getInt(4))
            assertEquals(120, it.getInt(5))
            assertEquals(2, it.getInt(6))
        }
    }
}
