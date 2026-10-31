package com.petruvideo.feature.editor

import com.petruvideo.core.data.autosave.SaveStatus
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.Reverb
import com.petruvideo.core.model.ReverbPreset
import com.petruvideo.core.model.Timeline

/** The selected clip, if any. */
sealed interface ClipSelection {
    val clipId: String

    data class Video(override val clipId: String) : ClipSelection
    data class Audio(override val clipId: String) : ClipSelection
}

/** Main tool bar tabs as designed. Only Edit is active in this feature; the rest show "coming soon". */
enum class ToolbarTab(val label: String, val active: Boolean) {
    Edit("Edit", true),
    Audio("Audio", false),
    Text("Text", false),
    Overlay("Overlay", false),
    Effects("Effects", false),
    Filters("Filters", false),
}

/** Clip tool bar actions. */
enum class ClipTool(val label: String, val active: Boolean) {
    Split("Split", true),
    Speed("Speed", true),
    Volume("Volume", true),
    Detach("Detach", true),
    Duplicate("Duplicate", true),
    Delete("Delete", true),
    Mute("Mute", true),
    Reverb("Reverb", true),
    Replace("Replace", true),
}

/**
 * The Speed or Volume control for one clip. [value] is the clip's committed speedMilli for Speed (1000 = 1x) or
 * percent for Volume.
 */
data class SheetState(
    val tool: ClipTool,
    val clipId: String,
    val clipName: String,
    val value: Int,
    /** Trimmed source length of the clip, so the Speed readout can show the resulting timeline length. */
    val sourceLengthMs: Long,
)

/** The open reverb panel for one clip. */
data class ReverbPanelState(val clipId: String, val clipName: String, val reverb: Reverb)

data class PlaybackState(
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
)

data class EditorHeader(val name: String, val formatLabel: String?, val saveStatus: SaveStatus)

data class EditorState(
    val header: EditorHeader? = null,
    val timeline: Timeline? = null,
    val selection: ClipSelection? = null,
    val playback: PlaybackState = PlaybackState(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    /** Media referenced by clips; shown in the Media panel on large screens. */
    val projectMedia: List<MediaItem> = emptyList(),
    /** Ids of media items whose source file is missing. */
    val missingMedia: Set<String> = emptySet(),
    val clipTools: List<ClipTool> = emptyList(),
    /** Tools of the selected clip shown dimmed, with the reason a tap explains. */
    val disabledTools: Map<ClipTool, String> = emptyMap(),
    /** The open Speed or Volume control, if any. */
    val sheet: SheetState? = null,
    /** The open reverb panel, if any. */
    val reverbPanel: ReverbPanelState? = null,
    /** Reverb of the selected clip when it has sound (the inspector shows it). */
    val selectedReverb: Reverb? = null,
    /** Whether the selected clip (audio, or video with sound) is muted; the Mute tool then shows Unmute. */
    val selectedAudioMuted: Boolean = false,
    val fullscreen: Boolean = false,
    /** Name of the inactive tool that was tapped, shown as "coming soon". */
    val comingSoon: String? = null,
    /** A short non-blocking message, e.g. an audio clip that snapped to the next free spot. */
    val message: String? = null,
) {
    val isEmpty: Boolean get() = timeline?.videoLane.isNullOrEmpty() && timeline?.audioLane.isNullOrEmpty()
}

sealed interface EditorEvent {
    data object Play : EditorEvent
    data object Pause : EditorEvent
    data class SeekTo(val ms: Long) : EditorEvent
    data class StepFrame(val direction: Int) : EditorEvent
    data class Select(val selection: ClipSelection?) : EditorEvent
    data class MoveClip(val clipId: String, val newIndex: Int) : EditorEvent
    /** Commits a trim handle drag. [newSourceMs] is a position in the clip's source (see TrimStart/TrimEnd). */
    data class TrimClip(val clipId: String, val start: Boolean, val newSourceMs: Long) : EditorEvent
    data class MoveAudio(val clipId: String, val newStartMs: Long) : EditorEvent
    /** A tool from the clip tool bar, applied to the current selection. */
    data class UseTool(val tool: ClipTool) : EditorEvent
    /** Adds a media item already in the project at [atMs], or at the playhead when null (Media panel tap or drop). */
    data class AddMediaAt(val mediaItemId: String, val atMs: Long? = null) : EditorEvent
    /** Jumps the playhead to the previous (-1) or next (+1) video clip boundary. */
    data class StepClip(val direction: Int) : EditorEvent
    data class ReplaceAudio(val clipId: String, val replacement: MediaItem) : EditorEvent
    /** The timeline's add clip button: opens Add media. */
    data object AddClip : EditorEvent
    data class RequestRelink(val mediaItemId: String) : EditorEvent
    data object Undo : EditorEvent
    data object Redo : EditorEvent
    data class SetFullscreen(val on: Boolean) : EditorEvent
    data class Relink(val mediaItemId: String, val replacement: MediaItem) : EditorEvent
    data class RemoveMissing(val mediaItemId: String) : EditorEvent
    data class TapInactiveTool(val name: String) : EditorEvent
    /** The Export button: opens the export screen for this project. */
    data object OpenExport : EditorEvent
    data object DismissMessage : EditorEvent
    /** The value while dragging: heard right away for Volume, not yet an edit. */
    data class SheetPreview(val tool: ClipTool, val value: Int) : EditorEvent
    /** The value on release or a quick choice, applied to the selected clip: one edit, one undo step. */
    data class SheetCommit(val tool: ClipTool, val value: Int) : EditorEvent
    data class SheetReset(val tool: ClipTool) : EditorEvent
    data object SheetDismiss : EditorEvent
    data class ReverbToggle(val on: Boolean) : EditorEvent
    data class ReverbPick(val preset: ReverbPreset) : EditorEvent
    /** A reverb setting while dragging: heard live, not yet an edit. */
    data class ReverbPreview(val reverb: Reverb) : EditorEvent
    /** A reverb setting on release: one edit, one undo step. */
    data class ReverbCommit(val reverb: Reverb) : EditorEvent
    data object ReverbReset : EditorEvent
    data object ReverbDismiss : EditorEvent
}

sealed interface EditorEffect {
    data object OpenAddMedia : EditorEffect
    data object OpenExport : EditorEffect
    data class PickRelinkFile(val mediaItemId: String) : EditorEffect
    data class PickReplacementAudio(val clipId: String) : EditorEffect
}
