package com.petruvideo.core.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration16To17Test : MigrationTestBase() {
    @Test
    fun existingProjectsOpenWithNoSavedLengthYet() {
        val name = "migration-16-17"
        helper.createDatabase(name, 16).apply {
            execSQL(
                "INSERT INTO project (id, name, createdAt, modifiedAt, status, playheadMs) " +
                    "VALUES ('p', 'P', 1, 1, 'Draft', 0)",
            )
            close()
        }
        val db = helper.runMigrationsAndValidate(name, 17, true)
        db.query("SELECT durationMs FROM project").use {
            it.moveToFirst()
            assertEquals(0L, it.getLong(0))
        }
    }
}
