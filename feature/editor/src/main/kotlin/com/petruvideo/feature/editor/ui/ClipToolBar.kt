package com.petruvideo.feature.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.icons.PetruIcons
import com.petruvideo.feature.editor.ClipTool

private fun iconFor(tool: ClipTool, muted: Boolean): ImageVector = when (tool) {
    ClipTool.Split -> PetruIcons.Split
    ClipTool.Speed -> PetruIcons.Speed
    ClipTool.Volume -> PetruIcons.Volume
    ClipTool.Audio -> PetruIcons.Audio
    ClipTool.Detach -> PetruIcons.Detach
    ClipTool.Duplicate -> PetruIcons.Duplicate
    ClipTool.Delete -> PetruIcons.Delete
    ClipTool.Mute -> if (muted) PetruIcons.Volume else PetruIcons.Mute
    ClipTool.Reverb -> PetruIcons.Reverb
    ClipTool.Replace -> PetruIcons.Replace
    ClipTool.TextEdit -> PetruIcons.ToolText
    ClipTool.FilterLooks -> PetruIcons.ToolFilters
    ClipTool.FilterAdjust -> PetruIcons.Settings
    ClipTool.TextStyle -> PetruIcons.ToolFilters
    ClipTool.TextAnim -> PetruIcons.ToolEffects
}

/**
 * The clip tool bar shown instead of the main tool bar while a clip is selected. One bar serves video and audio
 * clips; the view model decides which tools apply. Tools that do not apply to the clip (a photo has no speed or
 * sound) are dimmed.
 */
@Composable
fun ClipToolBar(
    tools: List<ClipTool>,
    muted: Boolean,
    onTool: (ClipTool) -> Unit,
    modifier: Modifier = Modifier,
    disabled: Set<ClipTool> = emptySet(),
) {
    Row(
        modifier
            .fillMaxWidth()
            .background(PetruColors.Background)
            .border(1.dp, PetruColors.SurfaceHigh)
            .navigationBarsPadding()
            .height(76.dp)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tools.forEach { tool ->
            val label = if (tool == ClipTool.Mute && muted) "Unmute" else tool.label
            ToolButton(
                label = label,
                icon = iconFor(tool, muted),
                active = tool.active && tool !in disabled,
                unavailableNote = if (tool.active) "unavailable" else "coming soon",
                onClick = { onTool(tool) },
                tint = if (tool == ClipTool.Delete) PetruColors.Danger else null,
            )
        }
    }
}
