package com.petruvideo.core.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration24To25Test : MigrationTestBase() {
    @Test
    fun existingTextsKeepTheirLookWithNoRotationShadowOrSpacing() {
        val name = "migration-24-25"
        helper.createDatabase(name, 24).apply {
            execSQL(
                "INSERT INTO project (id, name, createdAt, modifiedAt, status, playheadMs) " +
                    "VALUES ('p', 'P', 1, 1, 'Draft', 0)",
            )
            execSQL(
                "INSERT INTO text_clip (id, projectId, startMs, durationMs, text, x, y, size, font, color, align, " +
                    "bgEnabled, bgColor, bgOpacity, olEnabled, olColor, olWidth, inKind, inMs, outKind, outMs, " +
                    "caption) VALUES ('t', 'p', 0, 1000, 'Hi', 500, 500, 60, 'Sans', -1, 'Center', 0, -16777216, 60, " +
                    "0, -16777216, 12, 'None', 500, 'None', 500, 0)",
            )
            close()
        }
        val db = helper.runMigrationsAndValidate(name, 25, true)
        db.query(
            "SELECT rotation, shEnabled, shColor, shOpacity, shSoftness, shDistance, shDirection, letterSpacing, " +
                "lineSpacing FROM text_clip",
        ).use {
            it.moveToFirst()
            val expected = intArrayOf(0, 0, -16777216, 60, 40, 30, 45, 0, 100)
            for (i in expected.indices) assertEquals(expected[i], it.getInt(i))
        }
    }
}
