package com.petruvideo.feature.editor.ui.tabletop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.designsystem.components.IconAction
import com.petruvideo.core.designsystem.icons.PetruIcons
import com.petruvideo.feature.editor.ClipTool
import com.petruvideo.feature.editor.EditorEvent
import com.petruvideo.feature.editor.EditorState
import com.petruvideo.feature.editor.ui.ClipToolBar
import com.petruvideo.feature.editor.ui.EditorTopBar
import com.petruvideo.feature.editor.ui.MainToolBar
import com.petruvideo.feature.editor.ui.PreviewSurface
import com.petruvideo.feature.editor.ui.TimelineView
import com.petruvideo.feature.editor.ui.clock

/** The area above the hinge: the top bar and the preview, which gets the whole upper half. */
@Composable
fun TabletopPreviewHalf(
    state: EditorState,
    player: Player?,
    onBack: () -> Unit,
    onEvent: (EditorEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().statusBarsPadding()) {
        EditorTopBar(state.header, onBack = onBack, onExport = { onEvent(EditorEvent.TapInactiveTool("Export")) })
        PreviewSurface(player, Modifier.weight(1f).fillMaxWidth())
    }
}

/** The area below the hinge: previous clip, play, next clip, split; the timeline; and the tool bar. */
@Composable
fun TabletopControlsHalf(
    state: EditorState,
    onEvent: (EditorEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(PetruColors.Background)) {
        Row(
            Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${clock(state.playback.positionMs)} / ${clock(state.playback.durationMs)}",
                style = PetruType.Mono,
                color = PetruColors.OnBackground,
                modifier = Modifier.padding(end = 16.dp),
            )
            IconAction(
                PetruIcons.Undo,
                "Undo",
                { if (state.canUndo) onEvent(EditorEvent.Undo) },
                dimmed = !state.canUndo,
            )
            IconAction(PetruIcons.Previous, "Previous clip", { onEvent(EditorEvent.StepClip(-1)) })
            IconAction(
                icon = if (state.playback.isPlaying) PetruIcons.Pause else PetruIcons.Play,
                contentDescription = if (state.playback.isPlaying) "Pause" else "Play",
                onClick = { onEvent(if (state.playback.isPlaying) EditorEvent.Pause else EditorEvent.Play) },
                modifier = Modifier.background(PetruColors.OnBackground, CircleShape),
                tint = PetruColors.Background,
                dimmed = state.isEmpty,
            )
            IconAction(PetruIcons.Next, "Next clip", { onEvent(EditorEvent.StepClip(1)) })
            IconAction(
                PetruIcons.Redo,
                "Redo",
                { if (state.canRedo) onEvent(EditorEvent.Redo) },
                dimmed = !state.canRedo,
            )
            IconAction(
                PetruIcons.Split,
                "Split at playhead",
                { onEvent(EditorEvent.UseTool(ClipTool.Split)) },
                modifier = Modifier.background(PetruColors.Accent, CircleShape),
                tint = PetruColors.Background,
                dimmed = state.selection == null,
            )
        }
        Box(Modifier.weight(1f)) {
            state.timeline?.let {
                TimelineView(
                    timeline = it,
                    positionMs = state.playback.positionMs,
                    selection = state.selection,
                    onSeek = { ms -> onEvent(EditorEvent.SeekTo(ms)) },
                    onSelect = { sel -> onEvent(EditorEvent.Select(sel)) },
                    onMoveClip = { id, index -> onEvent(EditorEvent.MoveClip(id, index)) },
                    onTrim = { id, start, ms -> onEvent(EditorEvent.TrimClip(id, start, ms)) },
                    onAddClip = { onEvent(EditorEvent.AddClip) },
                    onDropMedia = { id, atMs -> onEvent(EditorEvent.AddMediaAt(id, atMs)) },
                )
            }
        }
        if (state.selection != null && state.clipTools.isNotEmpty()) {
            ClipToolBar(state.clipTools, state.selectedAudioMuted, onTool = { onEvent(EditorEvent.UseTool(it)) })
        } else {
            MainToolBar(onInactiveTap = { onEvent(EditorEvent.TapInactiveTool(it)) })
        }
    }
}
