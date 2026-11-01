package com.petruvideo.core.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration8To9Test : MigrationTestBase() {
    @Test
    fun existingProjectsOpenWithoutOverlays() {
        val name = "migration-8-9"
        helper.createDatabase(name, 8).apply {
            execSQL(
                "INSERT INTO project (id, name, createdAt, modifiedAt, status, playheadMs) " +
                    "VALUES ('p', 'P', 1, 1, 'Draft', 0)",
            )
            close()
        }
        val db = helper.runMigrationsAndValidate(name, 9, true)
        db.query("SELECT COUNT(*) FROM overlay_clip").use {
            it.moveToFirst()
            assertEquals(0, it.getInt(0))
        }
    }
}
