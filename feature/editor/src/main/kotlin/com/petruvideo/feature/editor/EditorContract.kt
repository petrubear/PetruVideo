package com.petruvideo.feature.editor

import com.petruvideo.core.data.autosave.SaveStatus
import com.petruvideo.core.model.AudioFx
import com.petruvideo.core.model.ClipEffect
import com.petruvideo.core.model.EffectKind
import com.petruvideo.core.model.ClipFilter
import com.petruvideo.core.model.Transition
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.ChromaKey
import com.petruvideo.core.model.OverlayClip
import com.petruvideo.core.model.OverlayMask
import com.petruvideo.core.model.TextClip
import com.petruvideo.core.model.Reverb
import com.petruvideo.core.model.ReverbPreset
import com.petruvideo.core.model.Timeline
import androidx.annotation.StringRes
import com.petruvideo.core.designsystem.UiText
import com.petruvideo.core.model.TEXT_PLACEHOLDER

/** The selected clip, if any. */
sealed interface ClipSelection {
    val clipId: String

    data class Video(override val clipId: String) : ClipSelection
    data class Audio(override val clipId: String) : ClipSelection
    data class Text(override val clipId: String) : ClipSelection
    data class Overlay(override val clipId: String) : ClipSelection
}

/** Main tool bar tabs as designed. */
enum class ToolbarTab(@StringRes val labelRes: Int) {
    Edit(R.string.editor_edit),
    Audio(R.string.editor_audio),
    Text(R.string.editor_text),
    Overlay(R.string.editor_overlay),
    Effects(R.string.editor_effects),
    Filters(R.string.editor_filters),
}

/** Clip tool bar actions. */
enum class ClipTool(@StringRes val labelRes: Int) {
    Split(R.string.editor_split),
    Speed(R.string.editor_speed),
    Volume(R.string.editor_volume),
    Audio(R.string.editor_audio),
    Detach(R.string.editor_detach),
    SplitVoice(R.string.editor_split_voice),
    Duplicate(R.string.editor_duplicate),
    Delete(R.string.editor_delete),
    Mute(R.string.editor_mute),
    SoundOn(R.string.editor_sound_on),
    Reverb(R.string.editor_reverb),
    Replace(R.string.editor_replace),
    TextEdit(R.string.editor_edit),
    TextStyle(R.string.editor_style),
    TextAnim(R.string.editor_animation),
    FilterLooks(R.string.editor_looks),
    FilterAdjust(R.string.editor_adjust),
    EffectTransition(R.string.editor_transition),
    EffectFx(R.string.editor_effect),
    OverlayOpacity(R.string.editor_opacity),
    Keyframes(R.string.editor_keyframes),
    Transform(R.string.editor_transform),
    Freeze(R.string.editor_freeze),
    Reverse(R.string.editor_reverse),
    Curve(R.string.editor_speed_curve),
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
enum class AudioToolKind(@StringRes val labelRes: Int) {
    List(R.string.editor_audio),
    Fade(R.string.editor_fade),
    Noise(R.string.editor_noise_reduction),
    Eq(R.string.editor_equalizer),
    Compressor(R.string.editor_compressor),
    Voice(R.string.editor_voice_effects),
    Ducking(R.string.editor_ducking),
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
    /** The clip's ducking when it is on the audio lane (video clips cannot duck); null otherwise. */
    val ducking: com.petruvideo.core.model.Ducking? = null,
    /** True when nothing plays over the clip, so ducking has nothing to react to. */
    val duckingIdle: Boolean = false,
)

/** The tools of a text clip; each opens its panel. */
enum class TextToolKind(@StringRes val labelRes: Int) {
    Edit(R.string.editor_edit_text),
    Style(R.string.editor_text_style),
    Animation(R.string.editor_text_animation),
}

/** The open text panel with the committed values of the selected text clip. */
data class TextPanelState(val kind: TextToolKind, val clip: TextClip)

/** The open overlay panel: the selected overlay clip and the name of its media. */
data class OverlayPanelState(
    val clip: OverlayClip,
    val name: String,
    /** The checkerboard behind see-through areas of a keyed overlay (the preview only). */
    val chromaChecker: Boolean = false,
    /** The next tap on the preview picks the key colour. */
    val chromaPicking: Boolean = false,
)

/** The two filter panels. */
enum class FilterToolKind(@StringRes val labelRes: Int) { Looks(R.string.editor_looks), Adjust(R.string.editor_adjust) }

/** The open filter panel for the selected picture clip, with what the look tiles need to draw its frame. */
data class FilterPanelState(
    val kind: FilterToolKind,
    val clipId: String,
    val filter: ClipFilter,
    val media: MediaItem?,
    /** Source time of the frame at the playhead, for the look tiles. */
    val frameTimeMs: Long,
    val canApplyAll: Boolean,
)

/** The two Effects panels: the transition after the clip, and the clip's picture effect. */
enum class EffectToolKind(
    @StringRes val labelRes: Int,
) { Transition(R.string.editor_transition), Effect(R.string.editor_effect) }

/**
 * The open Effects panel for the selected video-lane clip. [limitMs] is the longest transition its cut allows (0 when
 * no clip follows or the clips are too short).
 */
data class EffectPanelState(
    val kind: EffectToolKind,
    val clipId: String,
    val transition: Transition?,
    val effects: List<ClipEffect>,
    val limitMs: Long,
)

/**
 * The open Keyframes panel for the selected overlay or text clip: its keyframes (times relative to the clip), the
 * selected one, and where the playhead is inside the clip.
 */
data class KeyframePanelState(
    val clipId: String,
    val keyframes: List<com.petruvideo.core.model.Keyframe>,
    val selectedRelMs: Long?,
    val playheadRelMs: Long,
    val durationMs: Long,
    /** For a video clip: the zoom, pan, and opacity shown at the playhead; null for overlay and text clips. */
    val framing: com.petruvideo.core.model.Pose? = null,
)

/**
 * The open Speed curve panel: the clip's curve (a flat line at its constant speed when it has none), the length it
 * gives the clip, and the length the clip has at constant speed for comparison.
 */
data class SpeedCurvePanelState(
    val clipId: String,
    val curve: com.petruvideo.core.model.SpeedCurve,
    val hasCurve: Boolean,
    val lengthMs: Long,
    val constantLengthMs: Long,
)

/**
 * The open Auto captions panel: whether recognition works here ([availability] is null while it is being checked), the
 * installed [languages], the chosen [language], the Replace switch, progress while it runs, and a note about the
 * last run.
 */
data class CaptionsPanelState(
    val availability: com.petruvideo.core.media.captions.RecognitionAvailability?,
    val languages: List<String>,
    val language: String,
    val replace: Boolean,
    val progress: Int?,
    val note: UiText?,
)

/** The open Freeze frame panel: the clip and how long the still will be held (default length in ms). */
data class FreezePanelState(val clipId: String, val lengthMs: Long)

/** The open Transform panel: the clip's turn, mirrors, and crop. */
data class TransformPanelState(val clipId: String, val transform: com.petruvideo.core.model.ClipTransform)

/**
 * The open crop screen: the still to crop (the clip's picture as turned and mirrored, [stillTimeMs] into its source),
 * the box being edited, and the shape it keeps. [pictureWidth] and [pictureHeight] are the turned picture's pixels.
 */
data class CropSessionState(
    val clipId: String,
    val media: MediaItem,
    val stillTimeMs: Long,
    val transform: com.petruvideo.core.model.ClipTransform,
    val box: com.petruvideo.core.model.CropBox,
    val shape: com.petruvideo.core.model.CropShape,
    val pictureWidth: Int,
    val pictureHeight: Int,
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
    val disabledTools: Map<ClipTool, UiText> = emptyMap(),
    /** The open Speed or Volume control, if any. */
    val sheet: SheetState? = null,
    /** The open Audio list or audio tool panel, if any. */
    val audioPanel: AudioPanelState? = null,
    /** Which main tool bar tab is open: Edit (default) or Text, which offers Add text. */
    val toolbarTab: ToolbarTab = ToolbarTab.Edit,
    /** The open overlay panel, if any. */
    val overlayPanel: OverlayPanelState? = null,
    val chromaChecker: Boolean = false,
    val chromaPicking: Boolean = false,
    /** The selected overlay clip and the shape of its picture (width over height), for the preview box. */
    val selectedOverlay: OverlayClip? = null,
    val selectedOverlayAspect: Float = 1.78f,
    /** The count-in or running voice-over take, if any; editing is locked while it is set. */
    val voiceOver: VoiceOverUi? = null,
    /** The open Transform panel for the selected main-track clip, if any. */
    val transformPanel: TransformPanelState? = null,
    /** The open Freeze frame panel for the selected main-track clip, if any. */
    val freezePanel: FreezePanelState? = null,
    /** The open Background panel (Effects tab, no clip selected): the project's current background. */
    val canvasPanel: com.petruvideo.core.model.CanvasBackground? = null,
    /** Whether the Stickers gallery is open (Text tab). */
    val stickersOpen: Boolean = false,
    /** The open Auto captions panel (Text tab), if any. */
    val captionsPanel: CaptionsPanelState? = null,
    /** The open Speed curve panel for the selected main-track clip, if any. */
    val curvePanel: SpeedCurvePanelState? = null,
    /** Progress (0 to 100) of the backwards copy being made for the selected clip, if any. */
    val reverseProgress: Int? = null,
    /** The share done of a running voice split of the selected clip, or null. */
    val voiceSplitProgress: Int? = null,
    /** Whether the selected main-track clip plays backwards. */
    val selectedReversed: Boolean = false,
    /** The open crop screen, if any. */
    val crop: CropSessionState? = null,
    /** The open Keyframes panel, if any. */
    val keyframePanel: KeyframePanelState? = null,
    /** Time of the selected keyframe inside the selected clip, if one is selected. */
    val selectedKeyframeMs: Long? = null,
    /** The open Effects panel, if any. */
    val effectPanel: EffectPanelState? = null,
    /** The open filter panel, if any. */
    val filterPanel: FilterPanelState? = null,
    /** The selected picture clip's filter with its look tiles (the inspector shows its controls). */
    val selectedFilter: FilterPanelState? = null,
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
    /** A short non-blocking message, e.g. an audio clip that snapped to the next free spot. */
    val message: UiText? = null,
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
    /** Adds a text clip; [defaultText] is its first content, in the user's language. */
    data class AddText(val defaultText: String = TEXT_PLACEHOLDER) : EditorEvent
    /** The Add overlay button: opens a file picker for a video or photo. */
    data object AddOverlay : EditorEvent
    data class AddOverlayMedia(val media: MediaItem) : EditorEvent
    data object OpenOverlayPanel : EditorEvent
    /** An overlay's place or opacity while dragging: seen live, not yet an edit. */
    data class OverlayPreview(val clip: OverlayClip) : EditorEvent
    /** The overlay on release or a tap: one edit per changed part. */
    data class OverlayCommit(val clip: OverlayClip) : EditorEvent
    data object OverlayDismiss : EditorEvent
    data class ChromaPreview(val key: ChromaKey) : EditorEvent
    data class ChromaCommit(val key: ChromaKey) : EditorEvent
    data class ChromaChecker(val on: Boolean) : EditorEvent
    data class MaskPreview(val mask: OverlayMask) : EditorEvent
    data class MaskCommit(val mask: OverlayMask) : EditorEvent
    data object VoiceSplitCancel : EditorEvent
    data class ChromaPicking(val on: Boolean) : EditorEvent

    /** A tap on the preview while picking, as permille of the frame width and of the frame height. */
    data class ChromaPick(val xPermille: Int, val yPermille: Int) : EditorEvent
    data class MoveOverlayClip(val clipId: String, val startMs: Long) : EditorEvent
    data class TrimOverlayClip(val clipId: String, val startMs: Long, val endMs: Long) : EditorEvent
    data class OpenFilterTool(val kind: FilterToolKind) : EditorEvent
    /** A look strength or adjustment while dragging: seen live, not yet an edit. */
    data class FilterPreview(val filter: ClipFilter) : EditorEvent
    /** The filter on release or a tap: one edit per changed part. */
    data class FilterCommit(val filter: ClipFilter) : EditorEvent
    data object FilterResetAdjustments : EditorEvent
    data object FilterRemove : EditorEvent
    data object FilterApplyAll : EditorEvent
    data object FilterDismiss : EditorEvent
    /** The Record voice-over button: asks for the microphone when needed, then counts in. */
    data object RecordVoiceOver : EditorEvent
    data class MicrophoneResult(val access: MicAccess) : EditorEvent
    data object StopVoiceOver : EditorEvent
    data object CancelVoiceOver : EditorEvent
    /** The screen left the foreground: a running take stops and is kept. */
    data object VoiceOverInterrupted : EditorEvent
    data object OpenKeyframes : EditorEvent
    data object KeyframeAdd : EditorEvent
    data class KeyframeJump(val forward: Boolean) : EditorEvent
    data class KeyframeSelect(val relMs: Long) : EditorEvent
    data class KeyframeMove(val clipId: String, val fromRelMs: Long, val toRelMs: Long) : EditorEvent
    data object KeyframeCopyHere : EditorEvent
    data object KeyframeDelete : EditorEvent
    data class KeyframeMotion(val ease: Boolean) : EditorEvent
    data object KeyframeDismiss : EditorEvent

    data object TransformRotate : EditorEvent
    data class TransformFlip(val horizontal: Boolean) : EditorEvent
    data object TransformReset : EditorEvent
    data object TransformDismiss : EditorEvent
    data object CropOpen : EditorEvent

    /** Inserts a still of the frame at the playhead, held for [lengthMs]. */
    data class FreezeInsert(val lengthMs: Long) : EditorEvent

    data object FreezeDismiss : EditorEvent

    /** Turns Reverse on (making the backwards copy first) or off for the selected clip. */
    data object ReverseToggle : EditorEvent

    data object ReverseCancel : EditorEvent

    data object OpenCaptions : EditorEvent
    data object OpenStickers : EditorEvent
    data object OpenCanvas : EditorEvent
    data object CanvasDismiss : EditorEvent
    data class CanvasSet(val canvas: com.petruvideo.core.model.CanvasBackground) : EditorEvent
    data object StickersDismiss : EditorEvent
    data class AddStickerLayer(val sticker: com.petruvideo.core.model.Sticker) : EditorEvent
    data class CaptionsLanguage(val tag: String) : EditorEvent
    data class CaptionsReplace(val on: Boolean) : EditorEvent
    data object CaptionsStart : EditorEvent
    data object CaptionsCancel : EditorEvent
    data object CaptionsDismiss : EditorEvent
    data object CaptionsOpenSettings : EditorEvent

    data class SpeedCurvePreset(val preset: com.petruvideo.core.model.SpeedPreset) : EditorEvent

    /** A curve being dragged: only the readout follows until it is committed. */
    data class SpeedCurveDraft(val curve: com.petruvideo.core.model.SpeedCurve) : EditorEvent

    data class SpeedCurveCommit(val curve: com.petruvideo.core.model.SpeedCurve) : EditorEvent
    data object SpeedCurveReset : EditorEvent
    data object SpeedCurveDismiss : EditorEvent

    /** Drags [handle] (or the whole box when null) by permille of the picture. */
    data class CropDrag(val handle: com.petruvideo.core.model.CropHandle?, val dx: Int, val dy: Int) : EditorEvent
    data class CropShapePick(val shape: com.petruvideo.core.model.CropShape) : EditorEvent
    data object CropFullPicture : EditorEvent
    data object CropDone : EditorEvent
    data object CropCancel : EditorEvent

    /** The ducking of the selected audio-lane clip as it should be; one undo step. */
    data class DuckingChange(val ducking: com.petruvideo.core.model.Ducking) : EditorEvent

    /** A zoom, pan, or fade of the selected video clip at the playhead, live while it is dragged. */
    data class FramingPreview(val pose: com.petruvideo.core.model.Pose) : EditorEvent

    /** The same change when the gesture or slider is released: one undo step. */
    data class FramingCommit(val pose: com.petruvideo.core.model.Pose) : EditorEvent
    data class OpenEffectTool(val kind: EffectToolKind) : EditorEvent
    /** Sets the transition after the selected clip: one edit. */
    data class TransitionSet(val kind: com.petruvideo.core.model.TransitionKind, val lengthMs: Long) : EditorEvent
    data object TransitionRemove : EditorEvent
    /** An effect strength while dragging: seen live, not yet an edit. */
    data class EffectPreview(val effect: ClipEffect) : EditorEvent
    /** The effect on release or a tap: one edit. */
    data class EffectCommit(val effect: ClipEffect) : EditorEvent
    data class EffectRemove(val kind: EffectKind) : EditorEvent
    data object EffectDismiss : EditorEvent
    /** The Add music button: opens a file picker for an audio file. */
    data object AddMusic : EditorEvent
    data class AddMusicMedia(val media: MediaItem) : EditorEvent
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
    data object PickOverlayMedia : EditorEffect
    data object PickMusic : EditorEffect
    data object RequestMicrophone : EditorEffect
    data object OpenSpeechSettings : EditorEffect
    data object OpenAppSettings : EditorEffect
}
