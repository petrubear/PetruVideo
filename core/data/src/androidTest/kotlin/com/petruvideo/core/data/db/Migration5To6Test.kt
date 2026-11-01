package com.petruvideo.core.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration5To6Test : MigrationTestBase() {
    @Test
    fun existingProjectsOpenWithoutTextClips() {
        val name = "migration-5-6"
        helper.createDatabase(name, 5).apply {
            execSQL(
                "INSERT INTO project (id, name, createdAt, modifiedAt, status, playheadMs) " +
                    "VALUES ('p', 'P', 1, 1, 'Draft', 0)",
            )
            close()
        }
        val db = helper.runMigrationsAndValidate(name, 6, true)
        db.query("SELECT COUNT(*) FROM text_clip").use {
            it.moveToFirst()
            assertEquals(0, it.getInt(0))
        }
    }
}
