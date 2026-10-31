package com.petruvideo.app

import android.app.UiAutomation
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Test

/**
 * Rotation and window changes must not recreate the activity (so the project, playhead, selection, and playback
 * stay put) and must settle within one second (SC-009). Fold and unfold use the same configuration path; those
 * are exercised on the foldable emulator profile and the physical Fold (T135).
 */
class ConfigChangeRetentionTest {
    private val automation = InstrumentationRegistry.getInstrumentation().uiAutomation

    @After
    fun unfreezeRotation() {
        automation.setRotation(UiAutomation.ROTATION_UNFREEZE)
    }

    @Test
    fun rotatingDoesNotRecreateTheActivityAndSettlesQuickly() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var before: MainActivity? = null
            scenario.onActivity { before = it }
            automation.setRotation(UiAutomation.ROTATION_FREEZE_0)

            val start = SystemClock.elapsedRealtime()
            automation.setRotation(UiAutomation.ROTATION_FREEZE_90)
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            val elapsed = SystemClock.elapsedRealtime() - start

            var after: MainActivity? = null
            scenario.onActivity { after = it }
            (after === before) shouldBe true
            elapsed shouldBeLessThan SETTLE_LIMIT_MS
        }
    }

    private companion object {
        const val SETTLE_LIMIT_MS = 1_000L
    }
}
