package com.petruvideo.core.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration17To18Test : MigrationTestBase() {
    @Test
    fun existingProjectsKeepABlackBackground() {
        val name = "migration-17-18"
        helper.createDatabase(name, 17).apply {
            execSQL(
                "INSERT INTO project (id, name, createdAt, modifiedAt, status, playheadMs) " +
                    "VALUES ('p', 'P', 1, 1, 'Draft', 0)",
            )
            close()
        }
        val db = helper.runMigrationsAndValidate(name, 18, true)
        db.query("SELECT canvasKind, canvasColor, canvasBlur FROM project").use {
            it.moveToFirst()
            assertEquals("Black", it.getString(0))
            assertEquals(-16777216, it.getInt(1))
            assertEquals("Medium", it.getString(2))
        }
    }
}
