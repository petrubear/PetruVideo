package com.petruvideo.feature.importmedia

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runAndroidComposeUiTest
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import com.petruvideo.core.designsystem.PetruTheme
import com.petruvideo.core.model.MediaKind
import com.petruvideo.feature.importmedia.ui.AddMediaScreen
import org.junit.Test

/** Runs the Android accessibility checks (touch target size, labels, contrast) over Add media. */
@OptIn(ExperimentalTestApi::class)
class AccessibilityTest {
    private fun check(state: AddMediaState) = runAndroidComposeUiTest<ComponentActivity> {
        enableAccessibilityChecks()
        setContent { PetruTheme { AddMediaScreen(state, onEvent = {}) } }
        onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun withLibraryItems() = check(
        AddMediaState(
            items = (0 until 6).map { GridItem("content://lib/$it", MediaKind.Video, "clip$it.mp4", 10_000L * (it + 1), null) },
            totalDurationMs = 0,
            permission = MediaAccess.Granted,
        ),
    )
}
