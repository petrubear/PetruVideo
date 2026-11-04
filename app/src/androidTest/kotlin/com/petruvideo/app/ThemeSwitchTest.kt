package com.petruvideo.app

import android.content.res.Configuration
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Test

/**
 * A change of the phone's light or dark setting must not recreate the activity (so the project, playhead, selection,
 * open panels, playback, and a running export stay put) and the app must show the new look within one second
 * (SC-002 of feature 030). This test changes the phone's display mode and puts it back in [restore].
 */
class ThemeSwitchTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val originalNight = instrumentation.targetContext.resources.configuration.uiMode and
        Configuration.UI_MODE_NIGHT_MASK

    @After
    fun restore() {
        shell(if (originalNight == Configuration.UI_MODE_NIGHT_YES) "yes" else "no")
    }

    private fun shell(mode: String) {
        instrumentation.uiAutomation.executeShellCommand("cmd uimode night $mode").close()
    }

    private fun nightOf(activity: MainActivity) =
        activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

    @Test
    fun switchingLightAndDarkKeepsTheActivityAndSettlesQuickly() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var before: MainActivity? = null
            scenario.onActivity { before = it }
            val target = if (originalNight == Configuration.UI_MODE_NIGHT_YES) "no" else "yes"

            val start = SystemClock.elapsedRealtime()
            shell(target)
            var changed = false
            while (SystemClock.elapsedRealtime() - start < SETTLE_LIMIT_MS && !changed) {
                scenario.onActivity { changed = nightOf(it) != originalNight }
                if (!changed) SystemClock.sleep(POLL_MS)
            }
            instrumentation.waitForIdleSync()
            val elapsed = SystemClock.elapsedRealtime() - start

            var after: MainActivity? = null
            scenario.onActivity { after = it }
            changed shouldBe true
            (after === before) shouldBe true
            elapsed shouldBeLessThan SETTLE_LIMIT_MS + 1
        }
    }

    private companion object {
        const val SETTLE_LIMIT_MS = 1_000L
        const val POLL_MS = 50L
    }
}
