package com.petruvideo.feature.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petruvideo.core.data.autosave.SaveStatus
import com.petruvideo.core.data.session.HistoryState
import com.petruvideo.core.data.session.ProjectSession
import com.petruvideo.core.media.pressure.PressureNotice
import com.petruvideo.core.media.pressure.ResourcePressureMonitor
import com.petruvideo.core.media.preview.MediaAvailabilityChecker
import com.petruvideo.core.media.preview.PreviewEngine
import com.petruvideo.core.media.preview.PreviewState
import com.petruvideo.core.media.preview.ThumbnailRefresher
import com.petruvideo.core.model.Availability
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.Project
import com.petruvideo.core.model.Reverb
import com.petruvideo.core.model.ReverbPreset
import com.petruvideo.core.model.ReverbPresets
import com.petruvideo.core.model.SPEED_UNIT
import com.petruvideo.core.model.VOLUME_DEFAULT_PERCENT
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.engine.AddAudioClips
import com.petruvideo.core.model.engine.ApplyReverbPreset
import com.petruvideo.core.model.engine.SetReverbEnabled
import com.petruvideo.core.model.engine.SetReverbSettings
import com.petruvideo.core.model.engine.SetVideoMuted
import com.petruvideo.core.model.engine.AddVideoClips
import com.petruvideo.core.model.engine.AddVideoItem
import com.petruvideo.core.model.engine.Command
import com.petruvideo.core.model.engine.CommandResult
import com.petruvideo.core.model.engine.DetachAudio
import com.petruvideo.core.model.engine.Delete
import com.petruvideo.core.model.engine.Duplicate
import com.petruvideo.core.model.engine.MoveAudioClip
import com.petruvideo.core.model.engine.MoveVideoClip
import com.petruvideo.core.model.engine.Notice
import com.petruvideo.core.model.engine.RelinkMedia
import com.petruvideo.core.model.engine.RemoveMissingMedia
import com.petruvideo.core.model.engine.ReplaceAudio
import com.petruvideo.core.model.engine.SetMuted
import com.petruvideo.core.model.engine.SetSpeed
import com.petruvideo.core.model.engine.SetVolume
import com.petruvideo.core.model.engine.Split
import com.petruvideo.core.model.engine.TrimEnd
import com.petruvideo.core.model.engine.TrimStart
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.min
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Editor state holder. The open project lives in the process-scoped [ProjectSession] and playback in the
 * [PreviewEngine]; this view model only combines them and routes events, so fold, unfold, rotation, and resize
 * never lose the playhead, selection source, or edits.
 */
@HiltViewModel
class EditorViewModel @Inject constructor(
    private val session: ProjectSession,
    private val engine: PreviewEngine,
    private val availability: MediaAvailabilityChecker,
    private val thumbnails: ThumbnailRefresher,
    private val pressure: ResourcePressureMonitor,
) : ViewModel() {

    private data class Local(
        val selection: ClipSelection? = null,
        val fullscreen: Boolean = false,
        val comingSoon: String? = null,
        val message: String? = null,
        /** The open Speed or Volume sheet: which tool and which clip. */
        val sheet: OpenSheet? = null,
        /** Clip id of the open reverb panel. */
        val reverbClipId: String? = null,
    )

    private data class OpenSheet(val tool: ClipTool, val clipId: String)

    private data class Core(
        val project: Project?,
        val timeline: Timeline?,
        val history: HistoryState,
        val saveStatus: SaveStatus,
        val preview: PreviewState,
    )

    /** What the engine plays; the playhead is deliberately not part of it. */
    private data class Content(val timeline: Timeline) {
        override fun equals(other: Any?) = other is Content &&
            other.timeline.videoLane == timeline.videoLane &&
            other.timeline.audioLane == timeline.audioLane &&
            other.timeline.media == timeline.media
        override fun hashCode() = timeline.videoLane.hashCode() * HASH_MIX + timeline.media.hashCode()
    }

    private val local = MutableStateFlow(Local())
    private val effectChannel = Channel<EditorEffect>(Channel.BUFFERED)
    private val checkedMedia = mutableSetOf<String>()
    private var loadedProjectId: String? = null

    val effects = effectChannel.receiveAsFlow()

    /** The player to draw, valid after the first content has loaded. */
    val previewPlayer get() = engine.player

    val state: StateFlow<EditorState> = combine(
        session.project,
        session.timeline,
        session.history,
        session.saveStatus,
        engine.state,
        ::Core,
    ).combine(local, ::toState).stateIn(viewModelScope, SharingStarted.Eagerly, EditorState())

    init {
        viewModelScope.launch {
            session.timeline.filterNotNull().map(::Content).distinctUntilChanged()
                .collect { onContentChanged(it.timeline) }
        }
        viewModelScope.launch {
            pressure.quality.drop(1).collect { quality -> session.timeline.value?.let { engine.load(it, quality) } }
        }
        viewModelScope.launch { pressure.notices.collect(::onPressureNotice) }
    }

    fun open(projectId: String) {
        viewModelScope.launch { session.open(projectId) }
    }

    fun flush() {
        viewModelScope.launch { session.flush() }
    }

    override fun onCleared() {
        engine.release()
    }

    fun onEvent(event: EditorEvent) {
        when (event) {
            EditorEvent.Play -> play()
            EditorEvent.Pause -> pause()
            is EditorEvent.SeekTo -> seek(event.ms)
            is EditorEvent.StepFrame -> seek(engine.state.value.positionMs + event.direction * frameMs())
            is EditorEvent.Select -> select(event.selection)
            is EditorEvent.MoveClip -> session.apply(MoveVideoClip(event.clipId, event.newIndex))
            is EditorEvent.TrimClip -> run(trimCommand(event))
            is EditorEvent.MoveAudio -> run(MoveAudioClip(event.clipId, event.newStartMs))
            is EditorEvent.UseTool -> useTool(event.tool)
            is EditorEvent.AddMediaAt -> addMediaAt(event.mediaItemId, event.atMs)
            is EditorEvent.StepClip -> stepClip(event.direction)
            is EditorEvent.ReplaceAudio -> run(ReplaceAudio(event.clipId, event.replacement))
            EditorEvent.AddClip -> { pause(); effectChannel.trySend(EditorEffect.OpenAddMedia) }
            is EditorEvent.RequestRelink -> effectChannel.trySend(EditorEffect.PickRelinkFile(event.mediaItemId))
            EditorEvent.Undo -> session.undo()
            EditorEvent.Redo -> session.redo()
            is EditorEvent.SetFullscreen -> local.update { it.copy(fullscreen = event.on) }
            is EditorEvent.Relink -> relink(event.mediaItemId, event.replacement)
            is EditorEvent.RemoveMissing -> session.apply(RemoveMissingMedia(event.mediaItemId))
            EditorEvent.OpenExport -> openExport()
            is EditorEvent.TapInactiveTool -> local.update { it.copy(comingSoon = event.name) }
            EditorEvent.DismissMessage -> local.update { it.copy(comingSoon = null, message = null) }
            is EditorEvent.SheetPreview -> previewValue(event.tool, event.value)
            is EditorEvent.SheetCommit -> commitValue(event.tool, event.value)
            is EditorEvent.SheetReset ->
                commitValue(event.tool, if (event.tool == ClipTool.Speed) SPEED_UNIT else VOLUME_DEFAULT_PERCENT)
            EditorEvent.SheetDismiss -> closeSheet()
            is EditorEvent.ReverbToggle -> selectedClipId()?.let { run(SetReverbEnabled(it, event.on)) }
            is EditorEvent.ReverbPick -> selectedClipId()?.let { run(ApplyReverbPreset(it, event.preset)) }
            is EditorEvent.ReverbPreview -> selectedClipId()?.let { engine.setClipReverbLive(it, event.reverb) }
            is EditorEvent.ReverbCommit -> commitReverb(event.reverb)
            EditorEvent.ReverbReset -> resetReverb()
            EditorEvent.ReverbDismiss -> closeReverb()
        }
    }

    private fun openExport() {
        if (state.value.isEmpty) {
            say("Add a clip to export")
            return
        }
        pause()
        flush()
        effectChannel.trySend(EditorEffect.OpenExport)
    }

    private fun play() {
        val preview = engine.state.value
        val duration = session.timeline.value?.totalDurationMs ?: 0
        if (duration == 0L) return
        if (preview.ended || preview.positionMs >= duration) engine.seekTo(0)
        engine.play()
    }

    private fun pause() {
        engine.pause()
        session.setPlayhead(engine.state.value.positionMs)
    }

    private fun seek(ms: Long) {
        val duration = session.timeline.value?.totalDurationMs ?: 0
        val target = ms.coerceIn(0, duration)
        engine.seekTo(target)
        session.setPlayhead(target)
    }

    private fun frameMs(): Long = 1_000L / (session.timeline.value?.format?.fps ?: DEFAULT_FPS)

    private fun select(selection: ClipSelection?) {
        val timeline = session.timeline.value
        if (selection != null && (timeline == null || !exists(timeline, selection))) return
        closeSheet()
        closeReverb()
        local.update { it.copy(selection = selection) }
    }

    private fun selectedClipId(): String? = local.value.selection?.clipId

    private fun previewValue(tool: ClipTool, value: Int) {
        val id = selectedClipId() ?: return
        if (tool == ClipTool.Volume) engine.setClipVolumeLive(id, value)
    }

    private fun commitValue(tool: ClipTool, value: Int) {
        val id = selectedClipId() ?: return
        val command = if (tool == ClipTool.Speed) SetSpeed(id, value) else SetVolume(id, value)
        val result = session.apply(command)
        if (result !is CommandResult.Applied) {
            restoreLiveVolume(tool, id)
            return
        }
        if (Notice.SpeedLimited in result.notices) say(speedLimitMessage(result.timeline, id))
    }

    private fun soundClipReverb(timeline: Timeline, id: String): Reverb? =
        timeline.videoLane.firstOrNull { it.id == id }?.takeIf { it.attachedAudio }?.reverb
            ?: timeline.audioLane.firstOrNull { it.id == id }?.reverb

    private fun commitReverb(reverb: Reverb) {
        val id = selectedClipId() ?: return
        if (session.apply(SetReverbSettings(id, reverb)) !is CommandResult.Applied) restoreLiveReverb(id)
    }

    private fun resetReverb() {
        val id = selectedClipId() ?: return
        val current = session.timeline.value?.let { soundClipReverb(it, id) } ?: return
        val preset = current.preset.takeIf { it != ReverbPreset.Custom } ?: ReverbPresets.DEFAULT
        run(ApplyReverbPreset(id, preset))
    }

    private fun closeReverb() {
        val id = local.value.reverbClipId ?: return
        local.update { it.copy(reverbClipId = null) }
        restoreLiveReverb(id)
    }

    /** A reverb drag that did not become an edit must not leave its live settings behind. */
    private fun restoreLiveReverb(clipId: String) {
        val timeline = session.timeline.value ?: return
        soundClipReverb(timeline, clipId)?.let { engine.setClipReverbLive(clipId, it) }
    }

    private fun closeSheet() {
        val sheet = local.value.sheet ?: return
        local.update { it.copy(sheet = null) }
        restoreLiveVolume(sheet.tool, sheet.clipId)
    }

    /** A volume drag that did not become an edit must not leave its live gain behind. */
    private fun restoreLiveVolume(tool: ClipTool, clipId: String) {
        if (tool != ClipTool.Volume) return
        val timeline = session.timeline.value ?: return
        val committed = timeline.videoLane.firstOrNull { it.id == clipId }?.volumePercent
            ?: timeline.audioLane.firstOrNull { it.id == clipId }?.volumePercent
            ?: return
        engine.setClipVolumeLive(clipId, committed)
    }

    private fun speedLimitMessage(timeline: Timeline, clipId: String): String {
        val milli = timeline.videoLane.firstOrNull { it.id == clipId }?.speedMilli
            ?: timeline.audioLane.firstOrNull { it.id == clipId }?.speedMilli
            ?: return "Speed limited"
        val audio = timeline.audioLane.any { it.id == clipId }
        val reason = if (audio && milli < SPEED_UNIT) " to avoid overlapping the next audio clip" else ""
        return "Limited to ${formatSpeed(milli)}$reason"
    }

    /** Applies [command] and tells the user when audio snapped or a replacement shortened a clip. */
    private fun run(command: Command) {
        val result = session.apply(command) as? CommandResult.Applied ?: return
        when {
            Notice.Snapped in result.notices -> say("Audio moved to the next free spot")
            Notice.LengthChanged in result.notices -> say("Clip shortened to fit the new file")
        }
    }

    private fun addMediaAt(mediaItemId: String, atMs: Long?) {
        val timeline = session.timeline.value ?: return
        val media = timeline.media[mediaItemId] ?: return
        val at = atMs ?: engine.state.value.positionMs
        run(
            if (media.kind == MediaKind.Audio) {
                AddAudioClips(listOf(media), at)
            } else {
                AddVideoClips(listOf(AddVideoItem(media)), at)
            },
        )
    }

    private fun stepClip(direction: Int) {
        val timeline = session.timeline.value ?: return
        val starts = timeline.videoLane.map { timeline.videoStartOf(it.id) ?: 0L } + timeline.videoLengthMs
        val now = engine.state.value.positionMs
        val target = if (direction > 0) {
            starts.firstOrNull { it > now }
        } else {
            starts.lastOrNull { it < now - BOUNDARY_SLACK_MS }
        }
        seek(target ?: if (direction > 0) timeline.totalDurationMs else 0L)
    }

    private fun trimCommand(event: EditorEvent.TrimClip): Command =
        if (event.start) TrimStart(event.clipId, event.newSourceMs) else TrimEnd(event.clipId, event.newSourceMs)

    private fun useTool(tool: ClipTool) {
        val timeline = session.timeline.value ?: return
        val selection = local.value.selection?.takeIf { exists(timeline, it) } ?: return
        val id = selection.clipId
        when (tool) {
            ClipTool.Speed, ClipTool.Volume -> openSheet(tool, timeline, selection)
            ClipTool.Split -> run(Split(id, engine.state.value.positionMs))
            ClipTool.Detach -> run(DetachAudio(id))
            ClipTool.Duplicate -> run(Duplicate(id))
            ClipTool.Delete -> run(Delete(id))
            ClipTool.Mute -> toggleMute(timeline, id)
            ClipTool.Reverb -> openReverb(timeline, selection)
            ClipTool.Replace -> effectChannel.trySend(EditorEffect.PickReplacementAudio(id))
        }
    }

    private fun toggleMute(timeline: Timeline, id: String) {
        val audio = timeline.audioLane.firstOrNull { it.id == id }
        val video = timeline.videoLane.firstOrNull { it.id == id }
        when {
            audio != null -> run(SetMuted(id, !audio.muted))
            video?.attachedAudio == true -> run(SetVideoMuted(id, !video.muted))
            else -> say("This clip has no sound")
        }
    }

    private fun openReverb(timeline: Timeline, selection: ClipSelection) {
        val reason = disabledToolsFor(timeline, selection)[ClipTool.Reverb]
        if (reason != null) say(reason) else local.update { it.copy(reverbClipId = selection.clipId, sheet = null) }
    }

    private fun openSheet(tool: ClipTool, timeline: Timeline, selection: ClipSelection) {
        val reason = disabledToolsFor(timeline, selection)[tool]
        if (reason != null) {
            say(reason)
            return
        }
        closeReverb()
        local.update { it.copy(sheet = OpenSheet(tool, selection.clipId)) }
    }

    private fun relink(mediaItemId: String, replacement: MediaItem) {
        val result = session.apply(RelinkMedia(mediaItemId, replacement))
        if (result is CommandResult.Applied) {
            checkedMedia.remove(mediaItemId)
            if (Notice.LengthChanged in result.notices) {
                local.update { it.copy(message = "Clip shortened to fit the new file") }
            }
        }
    }

    private suspend fun onContentChanged(timeline: Timeline) {
        engine.load(timeline, pressure.quality.value)
        if (loadedProjectId != timeline.projectId) {
            loadedProjectId = timeline.projectId
            engine.seekTo(timeline.playheadMs)
        }
        val unchecked = timeline.media.values.filter { checkedMedia.add(it.id) }
        if (unchecked.isNotEmpty()) session.setAvailability(availability.check(unchecked))
        thumbnails.refresh(timeline.projectId, timeline)
    }

    private fun onPressureNotice(notice: PressureNotice) {
        when (notice) {
            PressureNotice.QualityReduced -> say(qualityReducedMessage())
            PressureNotice.PlaybackPaused -> { pause(); say("Playback paused to cool down") }
            PressureNotice.QualityRestored -> say("Preview quality restored")
        }
    }

    private fun qualityReducedMessage(): String {
        val timeline = session.timeline.value
        val reverbOn = timeline?.videoLane?.any { it.attachedAudio && it.reverb.enabled } == true ||
            timeline?.audioLane?.any { it.reverb.enabled } == true
        return if (reverbOn) {
            "Preview quality lowered and reverb paused to keep playing"
        } else {
            "Preview quality lowered to keep playing"
        }
    }

    private fun say(message: String) = local.update { it.copy(message = message) }

    private fun toState(core: Core, local: Local): EditorState {
        val timeline = core.timeline
        val selection = local.selection?.takeIf { timeline != null && exists(timeline, it) }
        val format = timeline?.format
        return EditorState(
            header = core.project?.let {
                EditorHeader(it.name, format?.let { f -> "${min(f.width, f.height)}p · ${f.fps} fps" }, core.saveStatus)
            },
            timeline = timeline,
            selection = selection,
            playback = PlaybackState(
                isPlaying = core.preview.isPlaying,
                positionMs = core.preview.positionMs,
                durationMs = timeline?.totalDurationMs ?: 0,
            ),
            canUndo = core.history.canUndo,
            canRedo = core.history.canRedo,
            projectMedia = timeline?.media?.values?.toList().orEmpty(),
            missingMedia = timeline?.media?.values?.filter { it.availability == Availability.Missing }
                ?.map { it.id }?.toSet().orEmpty(),
            clipTools = clipToolsFor(timeline, selection),
            disabledTools = if (timeline != null && selection != null) {
                disabledToolsFor(timeline, selection)
            } else {
                emptyMap()
            },
            sheet = if (timeline != null && selection != null) sheetFor(timeline, selection, local.sheet) else null,
            selectedAudioMuted = selection != null && timeline != null && isMuted(timeline, selection.clipId),
            reverbPanel = reverbPanelFor(timeline, selection, local.reverbClipId),
            selectedReverb = selection?.let { sel -> timeline?.let { soundClipReverb(it, sel.clipId) } },
            fullscreen = local.fullscreen,
            comingSoon = local.comingSoon,
            message = local.message,
        )
    }

    private fun isMuted(timeline: Timeline, id: String) =
        timeline.audioLane.firstOrNull { it.id == id }?.muted
            ?: timeline.videoLane.firstOrNull { it.id == id }?.muted ?: false

    private fun reverbPanelFor(timeline: Timeline?, selection: ClipSelection?, open: String?): ReverbPanelState? {
        if (timeline == null || selection == null || open != selection.clipId) return null
        val reverb = soundClipReverb(timeline, open) ?: return null
        val mediaId = timeline.videoLane.firstOrNull { it.id == open }?.mediaItemId
            ?: timeline.audioLane.firstOrNull { it.id == open }?.mediaItemId
        return ReverbPanelState(open, timeline.media[mediaId]?.displayName.orEmpty(), reverb)
    }

    private fun disabledToolsFor(timeline: Timeline, selection: ClipSelection): Map<ClipTool, String> {
        val clip = timeline.videoLane.firstOrNull { it.id == selection.clipId && selection is ClipSelection.Video }
            ?: return emptyMap()
        val noSound = "This clip has no sound"
        return when {
            timeline.media[clip.mediaItemId]?.kind == MediaKind.Photo -> mapOf(
                ClipTool.Speed to "Photos have no speed",
                ClipTool.Volume to "Photos have no sound",
                ClipTool.Mute to "Photos have no sound",
                ClipTool.Reverb to "Photos have no sound",
            )
            !clip.attachedAudio ->
                mapOf(ClipTool.Volume to noSound, ClipTool.Mute to noSound, ClipTool.Reverb to noSound)
            else -> emptyMap()
        }
    }

    private fun sheetFor(timeline: Timeline, selection: ClipSelection, open: OpenSheet?): SheetState? {
        if (open == null || open.clipId != selection.clipId) return null
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

    private fun exists(timeline: Timeline, selection: ClipSelection) = when (selection) {
        is ClipSelection.Video -> timeline.videoLane.any { it.id == selection.clipId }
        is ClipSelection.Audio -> timeline.audioLane.any { it.id == selection.clipId }
    }

    private fun clipToolsFor(timeline: Timeline?, selection: ClipSelection?): List<ClipTool> = when (selection) {
        null -> emptyList()
        is ClipSelection.Audio ->
            listOf(
                ClipTool.Split,
                ClipTool.Speed,
                ClipTool.Volume,
                ClipTool.Mute,
                ClipTool.Reverb,
                ClipTool.Replace,
                ClipTool.Duplicate,
                ClipTool.Delete,
            )
        is ClipSelection.Video -> {
            val attached = timeline?.videoLane?.firstOrNull { it.id == selection.clipId }?.attachedAudio == true
            listOfNotNull(
                ClipTool.Split,
                ClipTool.Speed,
                ClipTool.Volume,
                ClipTool.Mute,
                ClipTool.Reverb,
                ClipTool.Detach.takeIf { attached },
                ClipTool.Duplicate,
                ClipTool.Delete,
            )
        }
    }

    private companion object {
        const val DEFAULT_FPS = 30
        const val HASH_MIX = 31
        const val BOUNDARY_SLACK_MS = 300L
    }
}

