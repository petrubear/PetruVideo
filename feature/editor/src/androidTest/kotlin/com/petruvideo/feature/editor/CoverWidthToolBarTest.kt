package com.petruvideo.feature.editor

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.petruvideo.core.designsystem.PetruTheme
import com.petruvideo.feature.editor.ui.ClipToolBar
import org.junit.Rule
import org.junit.Test

/** The audio and video tool bars are wider than the 360 dp cover screen; every tool must still be reachable. */
class CoverWidthToolBarTest {
    @get:Rule
    val compose = createComposeRule()

    private fun everyToolIsReachable(tools: List<ClipTool>) {
        compose.setContent {
            PetruTheme { ClipToolBar(tools, muted = false, onTool = {}, modifier = Modifier.width(360.dp)) }
        }
        tools.forEach {
            compose.onNodeWithContentDescription(
                androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext.getString(
                    it.labelRes,
                ),
            ).performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun audioClipTools() = everyToolIsReachable(
        listOf(
            ClipTool.Split, ClipTool.Speed, ClipTool.Audio, ClipTool.Replace,
            ClipTool.Duplicate, ClipTool.Delete,
        ),
    )

    @Test
    fun videoClipTools() = everyToolIsReachable(
        listOf(
            ClipTool.Split, ClipTool.Speed, ClipTool.Audio, ClipTool.Detach,
            ClipTool.Duplicate, ClipTool.Delete,
        ),
    )
}
