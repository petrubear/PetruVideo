package com.petruvideo.core.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration3To4Test : MigrationTestBase() {
    @Test
    fun existingProjectsKeepTheirClipsAndHaveNoExportRows() {
        val name = "migration-3-4"
        helper.createDatabase(name, 3).apply {
            execSQL(
                "INSERT INTO project (id, name, createdAt, modifiedAt, status, playheadMs) " +
                    "VALUES ('p', 'P', 1, 1, 'Draft', 0)",
            )
            close()
        }
        val db = helper.runMigrationsAndValidate(name, 4, true)
        db.query("SELECT COUNT(*) FROM project").use {
            it.moveToFirst()
            assertEquals(1, it.getInt(0))
        }
        for (table in listOf("export_settings", "export_job")) {
            db.query("SELECT COUNT(*) FROM $table").use {
                it.moveToFirst()
                assertEquals(0, it.getInt(0))
            }
        }
        db.execSQL("INSERT INTO export_settings (projectId, resolution, frameRate, quality) VALUES ('p', 'R1080', 30, 'Recommended')")
        db.query("SELECT resolution FROM export_settings WHERE projectId = 'p'").use {
            it.moveToFirst()
            assertEquals("R1080", it.getString(0))
        }
    }
}
