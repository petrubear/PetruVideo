package com.petruvideo.feature.editor

import com.petruvideo.core.model.AudioFx
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.DuckingPlanner
import com.petruvideo.core.model.FREEZE_DEFAULT_MS
import com.petruvideo.core.model.REVERSE_MAX_MS
import com.petruvideo.core.model.Reverb
import com.petruvideo.core.model.SPEED_UNIT
import com.petruvideo.core.model.TRANSITION_MIN_MS
import com.petruvideo.core.model.TextStyle
import com.petruvideo.core.model.transitionLimitMs
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.VOLUME_DEFAULT_PERCENT
import com.petruvideo.core.model.engine.VoiceSplitBlock
import com.petruvideo.core.model.engine.VoiceSplitRules
import com.petruvideo.core.model.engine.Delete
import com.petruvideo.core.model.engine.Duplicate
import com.petruvideo.core.model.engine.Split
import com.petruvideo.core.designsystem.UiText

/** Pure builders of the editor's panel state from the timeline; split out of the view model. */

internal data class OpenSheet(val tool: ClipTool, val clipId: String)

internal data class OpenAudio(val kind: AudioToolKind, val clipId: String)

internal data class OpenText(val kind: TextToolKind, val clipId: String)

internal data class OpenFilter(val kind: FilterToolKind, val clipId: String)

internal data class OpenEffect(val kind: EffectToolKind, val clipId: String)

internal fun soundClipReverb(timeline: Timeline, id: String): Reverb? =
    timeline.videoLane.firstOrNull { it.id == id }?.takeIf { it.attachedAudio }?.reverb
        ?: timeline.overlayLane.firstOrNull { it.id == id }?.takeIf { it.attachedAudio }?.reverb
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
    return EffectPanelState(open.kind, clip.id, clip.transitionOut, clip.effects, transitionLimit(timeline, index))
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

internal fun curvePanelFor(
    timeline: Timeline?,
    selection: ClipSelection?,
    open: Boolean,
    draft: com.petruvideo.core.model.SpeedCurve?,
): SpeedCurvePanelState? {
    if (!open || timeline == null || selection !is ClipSelection.Video) return null
    val clip = timeline.videoLane.firstOrNull { it.id == selection.clipId } ?: return null
    val flat = com.petruvideo.core.model.SpeedCurve(
        listOf(
            com.petruvideo.core.model.SpeedPoint(0, clip.speedMilli),
            com.petruvideo.core.model.SpeedPoint(clip.sourceLengthMs, clip.speedMilli),
        ),
    )
    val curve = draft ?: clip.speedCurve ?: flat
    val constant = com.petruvideo.core.model.timelineLengthMs(clip.sourceLengthMs, SPEED_UNIT)
    return SpeedCurvePanelState(clip.id, curve, clip.speedCurve != null, curve.timelineMs, constant)
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
        ?: timeline.overlayLane.firstOrNull { it.id == id }?.takeIf { it.attachedAudio }?.let { it.fx to it.durationMs }
        ?: timeline.audioLane.firstOrNull { it.id == id }?.let { it.fx to it.lengthMs }

internal fun isMuted(timeline: Timeline?, id: String?) =
    timeline?.audioLane?.firstOrNull { it.id == id }?.muted
        ?: timeline?.videoLane?.firstOrNull { it.id == id }?.muted
        ?: timeline?.overlayLane?.firstOrNull { it.id == id }?.muted ?: false

/** What the sound panels read from a video, audio, or overlay clip, whichever the id belongs to. */
private class ClipFacts(
    val mediaItemId: String,
    val volumePercent: Int,
    val muted: Boolean,
    val reverb: Reverb,
    val speedMilli: Int?,
    val sourceLengthMs: Long,
)

private fun clipFacts(timeline: Timeline, id: String): ClipFacts? =
    timeline.videoLane.firstOrNull { it.id == id }?.let {
        ClipFacts(it.mediaItemId, it.volumePercent, it.muted, it.reverb, it.speedMilli, it.sourceLengthMs)
    } ?: timeline.audioLane.firstOrNull { it.id == id }?.let {
        ClipFacts(it.mediaItemId, it.volumePercent, it.muted, it.reverb, it.speedMilli, it.sourceLengthMs)
    } ?: timeline.overlayLane.firstOrNull { it.id == id }?.let {
        ClipFacts(it.mediaItemId, it.volumePercent, it.muted, it.reverb, null, it.durationMs)
    }

internal fun audioPanelFor(timeline: Timeline?, selection: ClipSelection?, open: OpenAudio?): AudioPanelState? {
    if (timeline == null || selection == null || open == null) return null
    if (open.clipId != selection.clipId) return null
    val (fx, length) = soundClip(timeline, open.clipId) ?: return null
    val facts = clipFacts(timeline, open.clipId)
    val audio = timeline.audioLane.firstOrNull { it.id == open.clipId }
    return AudioPanelState(
        kind = open.kind,
        clipId = open.clipId,
        clipName = timeline.media[facts?.mediaItemId]?.displayName.orEmpty(),
        fx = fx,
        clipLengthMs = length,
        volumePercent = facts?.volumePercent ?: VOLUME_DEFAULT_PERCENT,
        muted = facts?.muted ?: false,
        reverb = facts?.reverb ?: Reverb(),
        ducking = audio?.ducking,
        duckingIdle = audio != null && !DuckingPlanner.hasTriggerOver(timeline, audio),
    )
}

internal fun reverbPanelFor(timeline: Timeline?, selection: ClipSelection?, open: String?): ReverbPanelState? {
    if (timeline == null || selection == null || open != selection.clipId) return null
    val reverb = soundClipReverb(timeline, open) ?: return null
    val mediaId = timeline.videoLane.firstOrNull { it.id == open }?.mediaItemId
        ?: timeline.overlayLane.firstOrNull { it.id == open }?.mediaItemId
        ?: timeline.audioLane.firstOrNull { it.id == open }?.mediaItemId
    return ReverbPanelState(open, timeline.media[mediaId]?.displayName.orEmpty(), reverb)
}

internal fun VoiceSplitBlock.text(): UiText = UiText.Res(
    when (this) {
        VoiceSplitBlock.SelectClip -> R.string.editor_select_a_clip
        VoiceSplitBlock.NoSound -> R.string.editor_no_sound_in_this_clip
        VoiceSplitBlock.Reversed -> R.string.editor_not_available_for_reversed_clips
        VoiceSplitBlock.SpeedCurve -> R.string.editor_not_available_with_a_speed_curve
        VoiceSplitBlock.TooLong -> R.string.editor_works_on_clips_up_to_5_minutes
    },
)

internal fun disabledToolsFor(timeline: Timeline?, selection: ClipSelection?): Map<ClipTool, UiText> {
    if (timeline == null || selection == null) return emptyMap()
    if (selection is ClipSelection.Overlay) return disabledOverlayTools(timeline, selection.clipId)
    val clip = timeline.videoLane.firstOrNull { it.id == selection.clipId && selection is ClipSelection.Video }
        ?: return emptyMap()
    val noSound = UiText.Res(R.string.editor_this_clip_has_no_sound)
    val index = timeline.videoLane.indexOf(clip)
    val noTransition = when {
        index == timeline.videoLane.lastIndex -> UiText.Res(R.string.editor_no_clip_follows_this_one)
        transitionLimit(timeline, index) == 0L -> UiText.Res(R.string.editor_these_clips_are_too_short)
        else -> null
    }?.let { mapOf(ClipTool.EffectTransition to it) }.orEmpty()
    val tooLong = if (clip.sourceLengthMs > REVERSE_MAX_MS && !clip.reversed) {
        mapOf(ClipTool.Reverse to UiText.Res(R.string.editor_too_long_to_reverse_trim))
    } else {
        emptyMap()
    }
    val curved = if (clip.speedCurve != null) {
        mapOf(ClipTool.Speed to UiText.Res(R.string.editor_this_clip_uses_a_speed))
    } else {
        emptyMap()
    }
    val noSplit = VoiceSplitRules.reasonNot(timeline, clip.id)?.takeIf { clip.attachedAudio }?.text()
        ?.let { mapOf(ClipTool.SplitVoice to it) }.orEmpty()
    return noTransition + tooLong + curved + noSplit + when {
        timeline.media[clip.mediaItemId]?.kind == MediaKind.Photo -> mapOf(
            ClipTool.Speed to UiText.Res(R.string.editor_photos_have_no_speed),
            ClipTool.Freeze to UiText.Res(R.string.editor_a_photo_is_already_a),
            ClipTool.Reverse to UiText.Res(R.string.editor_photos_cannot_be_reversed),
            ClipTool.Curve to UiText.Res(R.string.editor_photos_have_no_speed),
            ClipTool.Volume to UiText.Res(R.string.editor_photos_have_no_sound),
            ClipTool.Mute to UiText.Res(R.string.editor_photos_have_no_sound),
            ClipTool.Reverb to UiText.Res(R.string.editor_photos_have_no_sound),
            ClipTool.Audio to UiText.Res(R.string.editor_photos_have_no_sound),
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

/** An overlay with no sound of its own has no sound tools; a photo has none either. */
private fun disabledOverlayTools(timeline: Timeline, clipId: String): Map<ClipTool, UiText> {
    val clip = timeline.overlayLane.firstOrNull { it.id == clipId } ?: return emptyMap()
    if (!clip.attachedAudio) {
        val note = UiText.Res(R.string.editor_this_clip_has_no_sound)
        return mapOf(
            ClipTool.Volume to note, ClipTool.Mute to note, ClipTool.Reverb to note, ClipTool.Audio to note,
            ClipTool.Detach to note, ClipTool.SplitVoice to note,
        )
    }
    return VoiceSplitRules.reasonNot(timeline, clip.id)?.let { mapOf(ClipTool.SplitVoice to it.text()) }.orEmpty()
}

internal fun sheetFor(timeline: Timeline?, selection: ClipSelection?, open: OpenSheet?): SheetState? {
    if (timeline == null || selection == null || open == null) return null
    if (open.clipId != selection.clipId) return null
    val facts = clipFacts(timeline, open.clipId) ?: return null
    val committed = (if (open.tool == ClipTool.Speed) facts.speedMilli else facts.volumePercent) ?: return null
    return SheetState(
        tool = open.tool,
        clipId = open.clipId,
        clipName = timeline.media[facts.mediaItemId]?.displayName.orEmpty(),
        value = committed,
        sourceLengthMs = facts.sourceLengthMs,
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
    is ClipSelection.Overlay -> overlayTools(timeline, selection)
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

/** The overlay tools; the sound tools appear when its video has a sound track (feature 029). */
private fun overlayTools(timeline: Timeline?, selection: ClipSelection): List<ClipTool> {
    val clip = timeline?.overlayLane?.firstOrNull { it.id == selection.clipId }
    val media = clip?.let { timeline.media[it.mediaItemId] }
    val hasSound = media?.kind == MediaKind.Video && media.hasAudio
    val attached = clip?.attachedAudio == true
    return listOfNotNull(
        ClipTool.OverlayOpacity,
        ClipTool.Keyframes,
        ClipTool.Audio.takeIf { attached },
        ClipTool.SoundOn.takeIf { hasSound && !attached },
        ClipTool.Detach.takeIf { attached },
        ClipTool.SplitVoice.takeIf { attached },
        ClipTool.Split,
        ClipTool.Duplicate,
        ClipTool.Delete,
    )
}

internal fun videoTools(timeline: Timeline?, selection: ClipSelection): List<ClipTool> {
    val attached = timeline?.videoLane?.firstOrNull { it.id == selection.clipId }?.attachedAudio == true
    return listOfNotNull(
        ClipTool.Split,
        ClipTool.Speed,
        ClipTool.FilterLooks,
        ClipTool.FilterAdjust,
        ClipTool.EffectFx,
        ClipTool.EffectTransition,
        ClipTool.Keyframes,
        ClipTool.Transform,
        ClipTool.Freeze,
        ClipTool.Reverse,
        ClipTool.Curve,
        ClipTool.Audio,
        ClipTool.Detach.takeIf { attached },
        ClipTool.SplitVoice.takeIf { attached },
        ClipTool.Duplicate,
        ClipTool.Delete,
    )
}

private val EFFECT_TOOLS = listOf(ClipTool.EffectTransition, ClipTool.EffectFx, ClipTool.Duplicate, ClipTool.Delete)

private fun audioTabTools(timeline: Timeline?, selection: ClipSelection): List<ClipTool> {
    val attached = timeline?.videoLane?.firstOrNull { it.id == selection.clipId }?.attachedAudio == true
    return listOfNotNull(
        ClipTool.Audio, ClipTool.Detach.takeIf { attached }, ClipTool.SplitVoice.takeIf { attached },
        ClipTool.Duplicate, ClipTool.Delete,
    )
}

private val FILTER_TOOLS = listOf(ClipTool.FilterLooks, ClipTool.FilterAdjust, ClipTool.Duplicate, ClipTool.Delete)

/** What a voice split depends on in its clip; when any of it changes, the split no longer fits and is cancelled. */
internal data class VoiceSplitSnapshot(
    val mediaItemId: String,
    val sourceStartMs: Long,
    val sourceEndMs: Long,
    val speedMilli: Int,
    val startMs: Long,
    val attachedAudio: Boolean,
    val reversed: Boolean,
    val curved: Boolean,
)

/** A running voice split of [clipId], with the clip as it was at the start and the share done. */
internal data class VoiceSplitRun(val clipId: String, val snapshot: VoiceSplitSnapshot, val progress: Int) {
    companion object {
        fun of(timeline: Timeline, clipId: String): VoiceSplitSnapshot? {
            timeline.overlayLane.firstOrNull { it.id == clipId }?.let { overlay ->
                return VoiceSplitSnapshot(
                    overlay.mediaItemId, overlay.sourceStartMs, overlay.sourceEndMs, SPEED_UNIT, overlay.startMs,
                    overlay.attachedAudio, reversed = false, curved = false,
                )
            }
            val clip = timeline.videoLane.firstOrNull { it.id == clipId } ?: return null
            val start = timeline.videoStartOf(clipId) ?: return null
            return VoiceSplitSnapshot(
                clip.mediaItemId, clip.sourceStartMs, clip.sourceEndMs, clip.speedMilli, start,
                clip.attachedAudio, clip.reversed, clip.speedCurve != null,
            )
        }
    }
}
