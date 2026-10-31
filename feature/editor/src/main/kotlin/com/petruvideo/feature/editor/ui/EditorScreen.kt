package com.petruvideo.feature.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruShapes
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.model.Availability
import com.petruvideo.feature.editor.EditorEvent
import com.petruvideo.feature.editor.EditorState
import kotlinx.coroutines.delay

private const val MESSAGE_MS = 2_500L

/**
 * The compact editor as designed: top bar, preview, transport row, timeline, and the main tool bar. Stateless:
 * everything flows through [onEvent], so the same state drives every arrangement.
 */
@Composable
fun EditorScreen(
    state: EditorState,
    player: Player?,
    onEvent: (EditorEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val timeline = state.timeline
    Box(modifier.fillMaxSize().background(PetruColors.Background)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            EditorTopBar(state.header, onBack = onBack, onExport = { onEvent(EditorEvent.TapInactiveTool("Export")) })
            PreviewSurface(player, Modifier.fillMaxWidth().height(232.dp))
            TransportRow(
                playback = state.playback,
                canUndo = state.canUndo,
                canRedo = state.canRedo,
                enabled = !state.isEmpty,
                onUndo = { onEvent(EditorEvent.Undo) },
                onPlayPause = { onEvent(if (state.playback.isPlaying) EditorEvent.Pause else EditorEvent.Play) },
                onRedo = { onEvent(EditorEvent.Redo) },
                onFullscreen = { onEvent(EditorEvent.SetFullscreen(true)) },
            )
            if (timeline != null) {
                TimelineView(
                    timeline = timeline,
                    positionMs = state.playback.positionMs,
                    selection = state.selection,
                    onSeek = { onEvent(EditorEvent.SeekTo(it)) },
                    onSelect = { onEvent(EditorEvent.Select(it)) },
                    onMoveClip = { id, index -> onEvent(EditorEvent.MoveClip(id, index)) },
                    onAddClip = { onEvent(EditorEvent.AddClip) },
                    modifier = Modifier.weight(1f),
                )
                MissingBar(state, onEvent)
            } else {
                Box(Modifier.weight(1f))
            }
            MainToolBar(onInactiveTap = { onEvent(EditorEvent.TapInactiveTool(it)) })
        }
        Banner(state, onEvent, Modifier.align(Alignment.BottomCenter).padding(bottom = 168.dp))
        if (state.fullscreen) FullscreenPreview(player, onClose = { onEvent(EditorEvent.SetFullscreen(false)) })
    }
}

@Composable
private fun MissingBar(state: EditorState, onEvent: (EditorEvent) -> Unit) {
    val timeline = state.timeline ?: return
    val missing = timeline.media.values.filter { it.availability == Availability.Missing }
    val first = missing.firstOrNull() ?: return
    MissingMediaBar(
        name = first.displayName,
        count = missing.size,
        onRelink = { onEvent(EditorEvent.RequestRelink(first.id)) },
        onRemove = { onEvent(EditorEvent.RemoveMissing(first.id)) },
    )
}

/** A short non-blocking message: "Coming soon" for inactive tools, or a notice such as lowered preview quality. */
@Composable
private fun Banner(state: EditorState, onEvent: (EditorEvent) -> Unit, modifier: Modifier = Modifier) {
    val text = state.comingSoon?.let { "$it: coming soon" } ?: state.message ?: return
    LaunchedEffect(text) {
        delay(MESSAGE_MS)
        onEvent(EditorEvent.DismissMessage)
    }
    Text(
        text,
        style = PetruType.Body,
        color = PetruColors.OnBackground,
        modifier = modifier
            .clip(PetruShapes.Chip)
            .background(PetruColors.SurfaceHighest)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}
