package com.petruvideo.feature.editor.ui

import androidx.compose.runtime.Composable
import com.petruvideo.feature.editor.EditorEvent
import com.petruvideo.feature.editor.EditorState
import com.petruvideo.feature.editor.ToolbarTab

/**
 * What sits at the bottom of the editor: with a clip selected its tool panel and clip tool bar; otherwise the bar of
 * the open main tab. [showPanels] is false where the inspector hosts the controls instead of sheets.
 */
@Composable
fun EditorBottomBars(state: EditorState, onEvent: (EditorEvent) -> Unit, showPanels: Boolean = true) {
    val back = { onEvent(EditorEvent.SelectTab(ToolbarTab.Edit)) }
    if (state.selection != null && state.clipTools.isNotEmpty()) {
        if (showPanels) {
            SpeedVolumeSheet(state.sheet, onEvent)
            ReverbPanel(state.reverbPanel, onEvent)
            AudioPanel(state.audioPanel, onEvent)
            TextPanel(state.textPanel, onEvent)
            FilterPanel(state.filterPanel, onEvent)
            OverlayPanel(state.overlayPanel, onEvent)
        }
        ClipToolBar(
            state.clipTools,
            state.selectedAudioMuted,
            onTool = { onEvent(EditorEvent.UseTool(it)) },
            disabled = state.disabledTools.keys,
        )
    } else {
        when (state.toolbarTab) {
            ToolbarTab.Overlay -> OverlayTabBar(back, onAdd = { onEvent(EditorEvent.AddOverlay) })
            ToolbarTab.Filters -> FiltersTabBar(back)
            ToolbarTab.Text -> TextTabBar(back, onAdd = { onEvent(EditorEvent.AddText) })
            else -> MainToolBar(
                onInactiveTap = { onEvent(EditorEvent.TapInactiveTool(it)) },
                onTab = { onEvent(EditorEvent.SelectTab(it)) },
            )
        }
    }
}
