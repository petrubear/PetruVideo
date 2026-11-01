package com.petruvideo.feature.editor

import com.petruvideo.core.data.autosave.SaveStatus
import com.petruvideo.core.model.AudioFx
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.TextClip
import com.petruvideo.core.model.Reverb
import com.petruvideo.core.model.ReverbPreset
import com.petruvideo.core.model.Timeline

/** The selected clip, if any. */
sealed interface ClipSelection {
    val clipId: String

    data class Video(override val clipId: String) : ClipSelection
    data class Audio(override val clipId: String) : ClipSelection
    data class Text(override val clipId: String) : ClipSelection
}

/** Main tool bar tabs as designed. Only Edit is active in this feature; the rest show "coming soon". */
enum class ToolbarTab(val label: String, val active: Boolean) {
    Edit("Edit", true),
    Audio("Audio", false),
    Text("Text", true),
    Overlay("Overlay", false),
    Effects("Effects", false),
    Filters("Filters", false),
}

/** Clip tool bar actions. */
enum class ClipTool(val label: String, val active: Boolean) {
    Split("Split", true),
    Speed("Speed", true),
    Volume("Volume", true),
    Audio("Audio", true),
    Detach("Detach", true),
    Duplicate("Duplicate", true),
    Delete("Delete", true),
    Mute("Mute", true),
    Reverb("Reverb", true),
    Replace("Replace", true),
    TextEdit("Edit", true),
    TextStyle("Style", true),
    TextAnim("Animation", true),
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

/** The tools in the Audio list; [List] is the list itself. */
enum class AudioToolKind(val label: String) {
    List("Audio"),
    Fade("Fade"),
    Noise("Noise reduction"),
    Eq("Equalizer"),
    Voice("Voice effects"),
}

/** The open Audio list or one of its panels for one clip, with the values the rows and panels show. */
data class AudioPanelState(
    val kind: AudioToolKind,
    val clipId: String,
    val clipName: String,
    val fx: AudioFx,
    val clipLengthMs: Long,
    val volumePercent: Int,
    val muted: Boolean,
    val reverb: Reverb,
)

/** The tools of a text clip; each opens its panel. */
enum class TextToolKind(val label: String) {
    Edit("Edit text"),
    Style("Text style"),
    Animation("Text animation"),
}

/** The open text panel with the committed values of the selected text clip. */
data class TextPanelState(val kind: TextToolKind, val clip: TextClip)

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
    /** The open Audio list or audio tool panel, if any. */
    val audioPanel: AudioPanelState? = null,
    /** Which main tool bar tab is open: Edit (default) or Text, which offers Add text. */
    val toolbarTab: ToolbarTab = ToolbarTab.Edit,
    /** The open text panel, if any. */
    val textPanel: TextPanelState? = null,
    /** The selected text clip (the inspector shows its controls). */
    val selectedText: TextClip? = null,
    /** The open reverb panel, if any. */
    val reverbPanel: ReverbPanelState? = null,
    /** Reverb of the selected clip when it has sound (the inspector shows it). */
    val selectedReverb: Reverb? = null,
    /** Fade, noise, equalizer and voice values of the selected clip when it has sound; the inspector shows them. */
    val selectedFx: AudioFx? = null,
    val selectedClipLengthMs: Long = 0,
    /** Whether the selected clip (audio, or video with sound) is muted; the Mute tool then shows Unmute. */
    val selectedAudioMuted: Boolean = false,
    val fullscreen: Boolean = false,
    /** Name of the inactive tool that was tapped, shown as "coming soon". */
    val comingSoon: String? = null,
    /** A short non-blocking message, e.g. an audio clip that snapped to the next free spot. */
    val message: String? = null,
) {
    val isEmpty: Boolean
        get() = timeline?.videoLane.isNullOrEmpty() && timeline?.audioLane.isNullOrEmpty() &&
            timeline?.textLane.isNullOrEmpty()
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
    /** Opens one of the Audio tools (or the list) for the selected clip. */
    data class OpenAudioTool(val kind: AudioToolKind) : EditorEvent
    /** Fade, noise, equalizer, or voice values while dragging: heard live, not yet an edit. */
    data class AudioFxPreview(val fx: AudioFx) : EditorEvent
    /** The values on release or a tap: one edit per changed part, one undo step. */
    data class AudioFxCommit(val fx: AudioFx) : EditorEvent
    data class AudioFxReset(val kind: AudioToolKind) : EditorEvent
    data object AudioDismiss : EditorEvent
    data class SelectTab(val tab: ToolbarTab) : EditorEvent
    /** Adds a text clip at the playhead, selects it, and opens its edit panel. */
    data object AddText : EditorEvent
    data class OpenTextTool(val kind: TextToolKind) : EditorEvent
    /** A text clip value while typing or dragging: seen live, not yet an edit. */
    data class TextPreview(val clip: TextClip) : EditorEvent
    /** The text clip value on release, a tap or a pause in typing: one edit per changed part. */
    data class TextCommit(val clip: TextClip) : EditorEvent
    data class TextReset(val kind: TextToolKind) : EditorEvent
    data object TextDismiss : EditorEvent
    data class MoveTextClip(val clipId: String, val startMs: Long) : EditorEvent
    data class TrimTextClip(val clipId: String, val startMs: Long, val endMs: Long) : EditorEvent
}

sealed interface EditorEffect {
    data object OpenAddMedia : EditorEffect
    data object OpenExport : EditorEffect
    data class PickRelinkFile(val mediaItemId: String) : EditorEffect
    data class PickReplacementAudio(val clipId: String) : EditorEffect
}
