package com.petruvideo.core.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration10To11Test : MigrationTestBase() {
    @Test
    fun existingProjectsOpenWithoutKeyframes() {
        val name = "migration-10-11"
        helper.createDatabase(name, 10).apply {
            execSQL(
                "INSERT INTO project (id, name, createdAt, modifiedAt, status, playheadMs) " +
                    "VALUES ('p', 'P', 1, 1, 'Draft', 0)",
            )
            close()
        }
        val db = helper.runMigrationsAndValidate(name, 11, true)
        db.query("SELECT COUNT(*) FROM keyframe").use {
            it.moveToFirst()
            assertEquals(0, it.getInt(0))
        }
    }
}
