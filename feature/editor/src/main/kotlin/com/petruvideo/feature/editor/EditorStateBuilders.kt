package com.petruvideo.feature.editor

import com.petruvideo.core.model.AudioFx
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.Reverb
import com.petruvideo.core.model.SPEED_UNIT
import com.petruvideo.core.model.TextStyle
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

internal fun textPanelFor(timeline: Timeline?, selection: ClipSelection?, open: OpenText?): TextPanelState? {
    if (timeline == null || selection == null || open == null) return null
    if (open.clipId != selection.clipId) return null
    val clip = timeline.textLane.firstOrNull { it.id == open.clipId } ?: return null
    return TextPanelState(open.kind, clip)
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
    return when {
        timeline.media[clip.mediaItemId]?.kind == MediaKind.Photo -> mapOf(
            ClipTool.Speed to "Photos have no speed",
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
    is ClipSelection.Video -> if (tab == ToolbarTab.Filters) FILTER_TOOLS else videoTools(timeline, selection)
    is ClipSelection.Overlay -> listOf(ClipTool.OverlayOpacity, ClipTool.Split, ClipTool.Duplicate, ClipTool.Delete)
    is ClipSelection.Text -> listOf(
        ClipTool.TextEdit, ClipTool.TextStyle, ClipTool.TextAnim, ClipTool.Split, ClipTool.Duplicate,
        ClipTool.Delete,
    )
    is ClipSelection.Audio ->
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
        ClipTool.Audio,
        ClipTool.Detach.takeIf { attached },
        ClipTool.Duplicate,
        ClipTool.Delete,
    )
}

private val FILTER_TOOLS = listOf(ClipTool.FilterLooks, ClipTool.FilterAdjust, ClipTool.Duplicate, ClipTool.Delete)
