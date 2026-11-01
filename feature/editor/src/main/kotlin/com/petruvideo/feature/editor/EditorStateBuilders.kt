package com.petruvideo.feature.editor

import com.petruvideo.core.model.AudioFx
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.DuckingPlanner
import com.petruvideo.core.model.FREEZE_DEFAULT_MS
import com.petruvideo.core.model.Reverb
import com.petruvideo.core.model.SPEED_UNIT
import com.petruvideo.core.model.TRANSITION_MIN_MS
import com.petruvideo.core.model.TextStyle
import com.petruvideo.core.model.transitionLimitMs
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.VOLUME_DEFAULT_PERCENT
import com.petruvideo.core.model.engine.Delete
import com.petruvideo.core.model.engine.Duplicate
import com.petruvideo.core.model.engine.Split

/** Pure builders of the editor's panel state from the timeline; split out of the view model. */

internal data class OpenSheet(val tool: ClipTool, val clipId: String)

internal data class OpenAudio(val kind: AudioToolKind, val clipId: String)

internal data class OpenText(val kind: TextToolKind, val clipId: String)

internal data class OpenFilter(val kind: FilterToolKind, val clipId: String)

internal data class OpenEffect(val kind: EffectToolKind, val clipId: String)

internal fun soundClipReverb(timeline: Timeline, id: String): Reverb? =
    timeline.videoLane.firstOrNull { it.id == id }?.takeIf { it.attachedAudio }?.reverb
        ?: timeline.audioLane.firstOrNull { it.id == id }?.reverb

internal fun filterPanelFor(
    timeline: Timeline?,
    selection: ClipSelection?,
    open: OpenFilter?,
    playheadMs: Long,
): FilterPanelState? {
    if (timeline == null || selection == null || open == null) return null
    if (open.clipId != selection.clipId) return null
    val clip = timeline.videoLane.firstOrNull { it.id == open.clipId } ?: return null
    val start = timeline.videoStartOf(clip.id) ?: 0L
    val offset = (playheadMs - start).coerceIn(0L, clip.lengthMs)
    return FilterPanelState(
        kind = open.kind,
        clipId = clip.id,
        filter = clip.filter,
        media = timeline.media[clip.mediaItemId],
        frameTimeMs = clip.sourceStartMs + offset * clip.speedMilli / SPEED_UNIT,
        canApplyAll = timeline.videoLane.size > 1,
    )
}

internal fun effectPanelFor(timeline: Timeline?, selection: ClipSelection?, open: OpenEffect?): EffectPanelState? {
    if (timeline == null || selection == null || open == null) return null
    if (open.clipId != selection.clipId) return null
    val index = timeline.videoLane.indexOfFirst { it.id == open.clipId }
    if (index < 0) return null
    val clip = timeline.videoLane[index]
    return EffectPanelState(open.kind, clip.id, clip.transitionOut, clip.effect, transitionLimit(timeline, index))
}

/** The longest transition after the clip at [index]; 0 when it is the last clip or the pair is too short. */
internal fun transitionLimit(timeline: Timeline, index: Int): Long {
    val next = timeline.videoLane.getOrNull(index + 1) ?: return 0L
    val limit = transitionLimitMs(timeline.videoLane[index].lengthMs, next.lengthMs)
    return if (limit < TRANSITION_MIN_MS) 0L else limit
}

/** The keyframes of the selected overlay or text clip as the panel and the lane diamonds need them. */
internal fun keyframePanelFor(
    timeline: Timeline?,
    selection: ClipSelection?,
    open: Boolean,
    selectedRel: Long?,
    playheadMs: Long,
): KeyframePanelState? {
    if (!open || timeline == null || selection == null) return null
    val target = keyframeTarget(timeline, selection) ?: return null
    val rel = (playheadMs - target.startMs).coerceIn(0, target.durationMs)
    val framing = timeline.videoLane.firstOrNull { it.id == selection.clipId }?.poseAt(rel)
    return KeyframePanelState(
        selection.clipId, target.frames, selectedRel?.takeIf { r -> target.frames.any { it.timeMs == r } },
        rel, target.durationMs, framing,
    )
}

/** What the keyframe tools work on: the selected overlay or text clip's keyframes, start, and length. */
internal data class KeyframeSubject(
    val frames: List<com.petruvideo.core.model.Keyframe>,
    val startMs: Long,
    val durationMs: Long,
)

internal fun keyframeTarget(timeline: Timeline, selection: ClipSelection): KeyframeSubject? =
    timeline.overlayLane.firstOrNull { it.id == selection.clipId }
        ?.let { KeyframeSubject(it.keyframes, it.startMs, it.durationMs) }
        ?: timeline.textLane.firstOrNull { it.id == selection.clipId }
            ?.let { KeyframeSubject(it.keyframes, it.startMs, it.durationMs) }
        ?: timeline.videoLane.firstOrNull { it.id == selection.clipId }?.let { clip ->
            timeline.videoStartOf(clip.id)?.let { KeyframeSubject(clip.keyframes, it, clip.lengthMs) }
        }

internal fun transformPanelFor(timeline: Timeline?, selection: ClipSelection?, open: Boolean): TransformPanelState? {
    if (!open || timeline == null || selection !is ClipSelection.Video) return null
    val clip = timeline.videoLane.firstOrNull { it.id == selection.clipId } ?: return null
    return TransformPanelState(clip.id, clip.transform)
}

internal fun freezePanelFor(selection: ClipSelection?, open: Boolean): FreezePanelState? =
    (selection as? ClipSelection.Video)?.takeIf { open }?.let { FreezePanelState(it.clipId, FREEZE_DEFAULT_MS) }

internal fun cropSessionFor(
    timeline: Timeline?,
    open: Triple<String, com.petruvideo.core.model.CropBox, com.petruvideo.core.model.CropShape>?,
): CropSessionState? {
    if (timeline == null || open == null) return null
    val clip = timeline.videoLane.firstOrNull { it.id == open.first } ?: return null
    val media = timeline.media[clip.mediaItemId] ?: return null
    val (w, h) = clip.transform.turnedSize(media.width ?: FALLBACK_WIDTH, media.height ?: FALLBACK_HEIGHT)
    return CropSessionState(
        clip.id, media, clip.sourceStartMs, clip.transform.copy(crop = com.petruvideo.core.model.CropBox()),
        open.second, open.third, w, h,
    )
}

internal const val FALLBACK_WIDTH = 1920
internal const val FALLBACK_HEIGHT = 1080

internal fun textPanelFor(
    timeline: Timeline?,
    selection: ClipSelection?,
    open: OpenText?,
    playheadMs: Long = 0,
): TextPanelState? {
    if (timeline == null || selection == null || open == null) return null
    if (open.clipId != selection.clipId) return null
    val clip = timeline.textLane.firstOrNull { it.id == open.clipId } ?: return null
    return TextPanelState(open.kind, clip.shownAt(playheadMs))
}

internal fun soundClip(timeline: Timeline, id: String): Pair<AudioFx, Long>? =
    timeline.videoLane.firstOrNull { it.id == id }?.takeIf { it.attachedAudio }?.let { it.fx to it.lengthMs }
        ?: timeline.audioLane.firstOrNull { it.id == id }?.let { it.fx to it.lengthMs }

internal fun isMuted(timeline: Timeline?, id: String?) =
    timeline?.audioLane?.firstOrNull { it.id == id }?.muted
        ?: timeline?.videoLane?.firstOrNull { it.id == id }?.muted ?: false

internal fun audioPanelFor(timeline: Timeline?, selection: ClipSelection?, open: OpenAudio?): AudioPanelState? {
    if (timeline == null || selection == null || open == null) return null
    if (open.clipId != selection.clipId) return null
    val (fx, length) = soundClip(timeline, open.clipId) ?: return null
    val video = timeline.videoLane.firstOrNull { it.id == open.clipId }
    val audio = timeline.audioLane.firstOrNull { it.id == open.clipId }
    val media = timeline.media[video?.mediaItemId ?: audio?.mediaItemId]
    return AudioPanelState(
        kind = open.kind,
        clipId = open.clipId,
        clipName = media?.displayName.orEmpty(),
        fx = fx,
        clipLengthMs = length,
        volumePercent = video?.volumePercent ?: audio?.volumePercent ?: VOLUME_DEFAULT_PERCENT,
        muted = video?.muted ?: audio?.muted ?: false,
        reverb = video?.reverb ?: audio?.reverb ?: Reverb(),
        ducking = audio?.ducking,
        duckingIdle = audio != null && !DuckingPlanner.hasTriggerOver(timeline, audio),
    )
}

internal fun reverbPanelFor(timeline: Timeline?, selection: ClipSelection?, open: String?): ReverbPanelState? {
    if (timeline == null || selection == null || open != selection.clipId) return null
    val reverb = soundClipReverb(timeline, open) ?: return null
    val mediaId = timeline.videoLane.firstOrNull { it.id == open }?.mediaItemId
        ?: timeline.audioLane.firstOrNull { it.id == open }?.mediaItemId
    return ReverbPanelState(open, timeline.media[mediaId]?.displayName.orEmpty(), reverb)
}

internal fun disabledToolsFor(timeline: Timeline?, selection: ClipSelection?): Map<ClipTool, String> {
    if (timeline == null || selection == null) return emptyMap()
    val clip = timeline.videoLane.firstOrNull { it.id == selection.clipId && selection is ClipSelection.Video }
        ?: return emptyMap()
    val noSound = "This clip has no sound"
    val index = timeline.videoLane.indexOf(clip)
    val noTransition = when {
        index == timeline.videoLane.lastIndex -> "No clip follows this one"
        transitionLimit(timeline, index) == 0L -> "These clips are too short for a transition"
        else -> null
    }?.let { mapOf(ClipTool.EffectTransition to it) }.orEmpty()
    return noTransition + when {
        timeline.media[clip.mediaItemId]?.kind == MediaKind.Photo -> mapOf(
            ClipTool.Speed to "Photos have no speed",
            ClipTool.Freeze to "A photo is already a still",
            ClipTool.Volume to "Photos have no sound",
            ClipTool.Mute to "Photos have no sound",
            ClipTool.Reverb to "Photos have no sound",
            ClipTool.Audio to "Photos have no sound",
        )
        !clip.attachedAudio ->
            mapOf(
            ClipTool.Volume to noSound,
            ClipTool.Mute to noSound,
            ClipTool.Reverb to noSound,
            ClipTool.Audio to noSound,
        )
        else -> emptyMap()
    }
}

internal fun sheetFor(timeline: Timeline?, selection: ClipSelection?, open: OpenSheet?): SheetState? {
    if (timeline == null || selection == null || open == null) return null
    if (open.clipId != selection.clipId) return null
    val video = timeline.videoLane.firstOrNull { it.id == open.clipId }
    val audio = timeline.audioLane.firstOrNull { it.id == open.clipId }
    val committed = when (open.tool) {
        ClipTool.Speed -> video?.speedMilli ?: audio?.speedMilli
        else -> video?.volumePercent ?: audio?.volumePercent
    } ?: return null
    val media = timeline.media[video?.mediaItemId ?: audio?.mediaItemId]
    return SheetState(
        tool = open.tool,
        clipId = open.clipId,
        clipName = media?.displayName.orEmpty(),
        value = committed,
        sourceLengthMs = video?.sourceLengthMs ?: audio?.sourceLengthMs ?: 0L,
    )
}

internal fun exists(timeline: Timeline, selection: ClipSelection) = when (selection) {
    is ClipSelection.Video -> timeline.videoLane.any { it.id == selection.clipId }
    is ClipSelection.Audio -> timeline.audioLane.any { it.id == selection.clipId }
    is ClipSelection.Text -> timeline.textLane.any { it.id == selection.clipId }
    is ClipSelection.Overlay -> timeline.overlayLane.any { it.id == selection.clipId }
}

internal fun clipToolsFor(
    timeline: Timeline?,
    selection: ClipSelection?,
    tab: ToolbarTab,
): List<ClipTool> = when (selection) {
    null -> emptyList()
    is ClipSelection.Video -> when (tab) {
        ToolbarTab.Filters -> FILTER_TOOLS
        ToolbarTab.Effects -> EFFECT_TOOLS
        ToolbarTab.Audio -> audioTabTools(timeline, selection)
        else -> videoTools(timeline, selection)
    }
    is ClipSelection.Overlay -> listOf(
        ClipTool.OverlayOpacity, ClipTool.Keyframes, ClipTool.Split, ClipTool.Duplicate, ClipTool.Delete,
    )
    is ClipSelection.Text -> listOf(
        ClipTool.TextEdit, ClipTool.TextStyle, ClipTool.TextAnim, ClipTool.Keyframes, ClipTool.Split,
        ClipTool.Duplicate, ClipTool.Delete,
    )
    is ClipSelection.Audio -> if (tab == ToolbarTab.Audio) listOf(ClipTool.Audio, ClipTool.Delete) else
        listOf(
            ClipTool.Split,
            ClipTool.Speed,
            ClipTool.Audio,
            ClipTool.Replace,
            ClipTool.Duplicate,
            ClipTool.Delete,
        )
}

internal fun videoTools(timeline: Timeline?, selection: ClipSelection): List<ClipTool> {
    val attached = timeline?.videoLane?.firstOrNull { it.id == selection.clipId }?.attachedAudio == true
    return listOfNotNull(
        ClipTool.Split,
        ClipTool.Speed,
        ClipTool.Keyframes,
        ClipTool.Transform,
        ClipTool.Freeze,
        ClipTool.Audio,
        ClipTool.Detach.takeIf { attached },
        ClipTool.Duplicate,
        ClipTool.Delete,
    )
}

private val EFFECT_TOOLS = listOf(ClipTool.EffectTransition, ClipTool.EffectFx, ClipTool.Duplicate, ClipTool.Delete)

private fun audioTabTools(timeline: Timeline?, selection: ClipSelection): List<ClipTool> {
    val attached = timeline?.videoLane?.firstOrNull { it.id == selection.clipId }?.attachedAudio == true
    return listOfNotNull(ClipTool.Audio, ClipTool.Detach.takeIf { attached }, ClipTool.Duplicate, ClipTool.Delete)
}

private val FILTER_TOOLS = listOf(ClipTool.FilterLooks, ClipTool.FilterAdjust, ClipTool.Duplicate, ClipTool.Delete)
