package com.petruvideo.core.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration15To16Test : MigrationTestBase() {
    @Test
    fun existingTextClipsOpenAsTypedText() {
        val name = "migration-15-16"
        helper.createDatabase(name, 15).apply {
            execSQL(
                "INSERT INTO project (id, name, createdAt, modifiedAt, status, playheadMs) " +
                    "VALUES ('p', 'P', 1, 1, 'Draft', 0)",
            )
            execSQL(
                "INSERT INTO text_clip (id, projectId, startMs, durationMs, text, x, y, size, font, color, align, " +
                    "bgEnabled, bgColor, bgOpacity, olEnabled, olColor, olWidth, inKind, inMs, outKind, outMs) " +
                    "VALUES ('t', 'p', 0, 1000, 'Hi', 500, 500, 60, 'Sans', -1, 'Center', 0, -16777216, 60, 0, " +
                    "-16777216, 12, 'None', 500, 'None', 500)",
            )
            close()
        }
        val db = helper.runMigrationsAndValidate(name, 16, true)
        db.query("SELECT caption FROM text_clip").use {
            it.moveToFirst()
            assertEquals(0, it.getInt(0))
        }
    }
}
