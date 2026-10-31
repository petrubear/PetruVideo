package com.petruvideo.feature.editor.ui.expanded

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.designsystem.adaptive.HingeBounds
import com.petruvideo.core.designsystem.adaptive.HingeSplit
import com.petruvideo.core.designsystem.adaptive.HingeAvoidance
import com.petruvideo.feature.editor.ClipSelection
import com.petruvideo.feature.editor.EditorEvent
import com.petruvideo.feature.editor.EditorState
import com.petruvideo.feature.editor.ui.ClipToolBar
import com.petruvideo.feature.editor.ui.EditorTopBar
import com.petruvideo.feature.editor.ui.PreviewSurface
import com.petruvideo.feature.editor.ui.TimelineView
import com.petruvideo.feature.editor.ui.TransportRow

private const val LANE_LABEL_TOP_DP = 30
private const val LANE_LABEL_GAP_DP = 44

/**
 * The unfolded editor as designed: tool rail, Media panel, preview with transport, inspector, and a full-width
 * timeline with labeled lanes. With a vertical separating hinge the window is split in two so nothing interactive
 * sits under it: rail, Media, and preview on the left; inspector and timeline on the right.
 */
@Composable
fun ExpandedEditorLayout(
    state: EditorState,
    player: Player?,
    hinge: HingeBounds?,
    onEvent: (EditorEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val timeline = state.timeline
    val separated = hinge != null && HingeAvoidance.isVertical(hinge)
    Column(modifier.fillMaxSize().statusBarsPadding()) {
        if (!separated) {
            EditorTopBar(state.header, onBack = onBack, onExport = { onEvent(EditorEvent.TapInactiveTool("Export")) })
        }
        if (separated) {
            HingeSplit(hinge, Modifier.weight(1f), first = {
                Column(Modifier.fillMaxSize()) {
                    EditorTopBar(state.header, onBack, { onEvent(EditorEvent.TapInactiveTool("Export")) })
                    Row(Modifier.weight(1f)) {
                        ToolRail({ onEvent(EditorEvent.TapInactiveTool(it)) })
                        MediaColumn(state, onEvent, Modifier.weight(1f))
                    }
                    PreviewColumn(state, player, onEvent, Modifier.height(PREVIEW_HEIGHT))
                }
            }, second = {
                Column(Modifier.fillMaxSize()) {
                    InspectorPanel(selectedClip(state), Modifier.weight(1f).fillMaxWidth())
                    TimelineWithLabels(state, onEvent, Modifier.height(TIMELINE_HEIGHT))
                }
            })
        } else {
            Row(Modifier.weight(1f)) {
                ToolRail({ onEvent(EditorEvent.TapInactiveTool(it)) })
                MediaColumn(state, onEvent, Modifier.width(MEDIA_WIDTH))
                PreviewColumn(state, player, onEvent, Modifier.weight(1f).fillMaxHeight())
                InspectorPanel(selectedClip(state), Modifier.width(INSPECTOR_WIDTH))
            }
            if (timeline != null) TimelineWithLabels(state, onEvent, Modifier.height(TIMELINE_HEIGHT))
        }
        if (state.selection != null && state.clipTools.isNotEmpty()) {
            ClipToolBar(state.clipTools, state.selectedAudioMuted, onTool = { onEvent(EditorEvent.UseTool(it)) })
        }
    }
}

@Composable
private fun MediaColumn(state: EditorState, onEvent: (EditorEvent) -> Unit, modifier: Modifier) {
    Box(modifier) {
        MediaPanel(
            media = state.projectMedia,
            onImport = { onEvent(EditorEvent.AddClip) },
            onAdd = { onEvent(EditorEvent.AddMediaAt(it.id)) },
        )
    }
}

@Composable
private fun PreviewColumn(state: EditorState, player: Player?, onEvent: (EditorEvent) -> Unit, modifier: Modifier) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        PreviewSurface(player, Modifier.weight(1f).fillMaxWidth())
        TransportRow(
            playback = state.playback,
            canUndo = state.canUndo,
            canRedo = state.canRedo,
            enabled = !state.isEmpty,
            onUndo = { if (state.canUndo) onEvent(EditorEvent.Undo) },
            onPlayPause = { onEvent(if (state.playback.isPlaying) EditorEvent.Pause else EditorEvent.Play) },
            onRedo = { if (state.canRedo) onEvent(EditorEvent.Redo) },
            onFullscreen = { onEvent(EditorEvent.SetFullscreen(true)) },
        )
    }
}

@Composable
private fun TimelineWithLabels(state: EditorState, onEvent: (EditorEvent) -> Unit, modifier: Modifier) {
    val timeline = state.timeline ?: return
    Row(modifier.fillMaxWidth()) {
        Column(Modifier.width(LABEL_WIDTH).fillMaxHeight()) {
            Spacer(Modifier.height(LANE_LABEL_TOP_DP.dp))
            LaneLabel("V1", 56.dp)
            Spacer(Modifier.height(LANE_LABEL_GAP_DP.dp))
            LaneLabel("A1", 32.dp)
        }
        TimelineView(
            timeline = timeline,
            positionMs = state.playback.positionMs,
            selection = state.selection,
            onSeek = { onEvent(EditorEvent.SeekTo(it)) },
            onSelect = { onEvent(EditorEvent.Select(it)) },
            onMoveClip = { id, index -> onEvent(EditorEvent.MoveClip(id, index)) },
            onTrim = { id, start, ms -> onEvent(EditorEvent.TrimClip(id, start, ms)) },
            onAddClip = { onEvent(EditorEvent.AddClip) },
            onDropMedia = { id, atMs -> onEvent(EditorEvent.AddMediaAt(id, atMs)) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun LaneLabel(text: String, height: androidx.compose.ui.unit.Dp) {
    Box(Modifier.width(LABEL_WIDTH).height(height), contentAlignment = Alignment.Center) {
        Text(text, style = PetruType.MonoSmall, color = PetruColors.OnSurfaceMuted)
    }
}

private fun selectedClip(state: EditorState): InspectorClip? {
    val timeline = state.timeline ?: return null
    return when (val selection = state.selection) {
        null -> null
        is ClipSelection.Video -> timeline.videoLane.withIndex().firstOrNull { it.value.id == selection.clipId }?.let {
            val clip = it.value
            inspectorClip(
                name = clip.mediaItemId,
                label = "Clip ${it.index + 1}",
                lengthMs = clip.lengthMs,
                startMs = timeline.videoStartOf(clip.id) ?: 0L,
                media = timeline.media[clip.mediaItemId],
            )
        }
        is ClipSelection.Audio -> timeline.audioLane.firstOrNull { it.id == selection.clipId }?.let {
            inspectorClip(it.mediaItemId, "Audio", it.lengthMs, it.timelineStartMs, timeline.media[it.mediaItemId])
        }
    }
}

private val MEDIA_WIDTH = 244.dp
private val INSPECTOR_WIDTH = 260.dp
private val TIMELINE_HEIGHT = 200.dp
private val PREVIEW_HEIGHT = 260.dp
private val LABEL_WIDTH = 44.dp
