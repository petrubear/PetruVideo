package com.petruvideo.feature.editor

import com.petruvideo.core.data.autosave.SaveStatus
import com.petruvideo.core.model.MediaItem
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

/** Clip tool bar actions. Speed and Volume stay dimmed until their features exist. */
enum class ClipTool(val label: String, val active: Boolean) {
    Split("Split", true),
    Speed("Speed", false),
    Volume("Volume", false),
    Detach("Detach", true),
    Duplicate("Duplicate", true),
    Delete("Delete", true),
    Mute("Mute", true),
    Replace("Replace", true),
}

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
    data object Undo : EditorEvent
    data object Redo : EditorEvent
    data class SetFullscreen(val on: Boolean) : EditorEvent
    data class Relink(val mediaItemId: String, val replacement: MediaItem) : EditorEvent
    data class RemoveMissing(val mediaItemId: String) : EditorEvent
    data class TapInactiveTool(val name: String) : EditorEvent
    data object DismissMessage : EditorEvent
}

sealed interface EditorEffect {
    data object OpenAddMedia : EditorEffect
    data class PickRelinkFile(val mediaItemId: String) : EditorEffect
}
