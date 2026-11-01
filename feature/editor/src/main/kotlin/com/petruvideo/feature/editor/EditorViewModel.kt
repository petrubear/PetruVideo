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
import com.petruvideo.core.model.AudioFx
import com.petruvideo.core.model.Availability
import com.petruvideo.core.model.ClipFilter
import com.petruvideo.core.model.Equalizer
import com.petruvideo.core.model.FILTER_INTENSITY_DEFAULT
import com.petruvideo.core.model.Fade
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.NoiseReduction
import com.petruvideo.core.model.OverlayClip
import com.petruvideo.core.model.Project
import com.petruvideo.core.model.Reverb
import com.petruvideo.core.model.ReverbPreset
import com.petruvideo.core.model.ReverbPresets
import com.petruvideo.core.model.SPEED_UNIT
import com.petruvideo.core.model.TextAnimation
import com.petruvideo.core.model.TextClip
import com.petruvideo.core.model.TextStyle
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.VOLUME_DEFAULT_PERCENT
import com.petruvideo.core.model.VoiceFx
import com.petruvideo.core.model.engine.AddAudioClips
import com.petruvideo.core.model.engine.AddOverlay
import com.petruvideo.core.model.engine.AddText
import com.petruvideo.core.model.engine.AddVideoClips
import com.petruvideo.core.model.engine.AddVideoItem
import com.petruvideo.core.model.engine.ApplyFilterToAll
import com.petruvideo.core.model.engine.ApplyReverbPreset
import com.petruvideo.core.model.engine.Command
import com.petruvideo.core.model.engine.CommandResult
import com.petruvideo.core.model.engine.Delete
import com.petruvideo.core.model.engine.DetachAudio
import com.petruvideo.core.model.engine.Duplicate
import com.petruvideo.core.model.engine.MoveAudioClip
import com.petruvideo.core.model.engine.MoveOverlay
import com.petruvideo.core.model.engine.MoveText
import com.petruvideo.core.model.engine.MoveVideoClip
import com.petruvideo.core.model.engine.Notice
import com.petruvideo.core.model.engine.RelinkMedia
import com.petruvideo.core.model.engine.RemoveFilter
import com.petruvideo.core.model.engine.RemoveMissingMedia
import com.petruvideo.core.model.engine.ReplaceAudio
import com.petruvideo.core.model.engine.ResetFilterAdjustments
import com.petruvideo.core.model.engine.SetEqualizer
import com.petruvideo.core.model.engine.SetFade
import com.petruvideo.core.model.engine.SetFilterAdjustments
import com.petruvideo.core.model.engine.SetFilterIntensity
import com.petruvideo.core.model.engine.SetFilterLook
import com.petruvideo.core.model.engine.SetMuted
import com.petruvideo.core.model.engine.SetNoiseReduction
import com.petruvideo.core.model.engine.SetOverlayOpacity
import com.petruvideo.core.model.engine.SetOverlayPlacement
import com.petruvideo.core.model.engine.SetReverbEnabled
import com.petruvideo.core.model.engine.SetReverbSettings
import com.petruvideo.core.model.engine.SetSpeed
import com.petruvideo.core.model.engine.SetTextAnimation
import com.petruvideo.core.model.engine.SetTextContent
import com.petruvideo.core.model.engine.SetTextPlacement
import com.petruvideo.core.model.engine.SetTextStyle
import com.petruvideo.core.model.engine.SetVideoMuted
import com.petruvideo.core.model.engine.SetVoiceFx
import com.petruvideo.core.model.engine.SetVolume
import com.petruvideo.core.model.engine.Split
import com.petruvideo.core.model.engine.TrimEnd
import com.petruvideo.core.model.engine.TrimOverlay
import com.petruvideo.core.model.engine.TrimStart
import com.petruvideo.core.model.engine.TrimText
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
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
        /** The open Audio list or tool panel: which clip and which tool. */
        val audio: OpenAudio? = null,
        val tab: ToolbarTab = ToolbarTab.Edit,
        /** The open text panel: which tool and which clip. */
        val text: OpenText? = null,
        /** The open filter panel: which tool and which clip. */
        val filter: OpenFilter? = null,
        /** Whether the overlay panel is open (for the selected overlay clip). */
        val overlayOpen: Boolean = false,
    )

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
            other.timeline.videoLane.map { it.copy(filter = it.filter.structural()) } ==
                timeline.videoLane.map { it.copy(filter = it.filter.structural()) } &&
            other.timeline.audioLane == timeline.audioLane &&
            other.timeline.media == timeline.media &&
            other.timeline.textLane.map { it.id } == timeline.textLane.map { it.id } &&
            other.timeline.overlayLane.map { it.structural() } == timeline.overlayLane.map { it.structural() } &&
            other.textBeyondVideo == textBeyondVideo

        /** Text past the end of the video makes the video sequence run on, which needs a rebuild. */
        private val textBeyondVideo: Long
            get() = (timeline.textLane.maxOfOrNull { it.endMs } ?: 0L).minus(timeline.videoLengthMs).coerceAtLeast(0)

        private fun OverlayClip.structural() = copy(xPermille = 0, yPermille = 0, widthPermille = 0, opacityPercent = 0)

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
        viewModelScope.launch {
            session.timeline.filterNotNull().map { it.textLane }.distinctUntilChanged()
                .collect { lane -> lane.forEach(engine::setTextLive) }
        }
        viewModelScope.launch {
            session.timeline.filterNotNull().map { it.overlayLane }.distinctUntilChanged()
                .collect { lane -> lane.forEach(engine::setOverlayLive) }
        }
        viewModelScope.launch {
            session.timeline.filterNotNull().map { t -> t.videoLane.associate { it.id to it.filter } }
                .distinctUntilChanged()
                .collect { filters -> filters.forEach { (id, filter) -> engine.setClipFilterLive(id, filter) } }
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
            EditorEvent.Undo -> { flushText(); session.undo() }
            EditorEvent.Redo -> { flushText(); session.redo() }
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
            else -> onToolEvent(event)
        }
    }

    /** The events of the tool panels (reverb, audio, text, filters, overlays), split out of [onEvent]. */
    private fun onToolEvent(event: EditorEvent) {
        when (event) {
            is EditorEvent.ReverbToggle -> selectedClipId()?.let { run(SetReverbEnabled(it, event.on)) }
            is EditorEvent.ReverbPick -> selectedClipId()?.let { run(ApplyReverbPreset(it, event.preset)) }
            is EditorEvent.ReverbPreview -> selectedClipId()?.let { engine.setClipReverbLive(it, event.reverb) }
            is EditorEvent.ReverbCommit -> commitReverb(event.reverb)
            EditorEvent.ReverbReset -> resetReverb()
            EditorEvent.ReverbDismiss -> closeReverb()
            is EditorEvent.OpenAudioTool -> openAudio(event.kind)
            is EditorEvent.AudioFxPreview -> previewFx(event.fx)
            is EditorEvent.AudioFxCommit -> commitFx(event.fx)
            is EditorEvent.AudioFxReset -> resetFx(event.kind)
            EditorEvent.AudioDismiss -> closeAudio()
            is EditorEvent.SelectTab -> selectTab(event.tab)
            EditorEvent.AddText -> addText()
            EditorEvent.AddOverlay -> { pause(); effectChannel.trySend(EditorEffect.PickOverlayMedia) }
            is EditorEvent.AddOverlayMedia -> addOverlay(event.media)
            EditorEvent.OpenOverlayPanel -> openOverlay()
            is EditorEvent.OverlayPreview -> engine.setOverlayLive(event.clip)
            is EditorEvent.OverlayCommit -> commitOverlay(event.clip)
            EditorEvent.OverlayDismiss -> closeOverlay()
            is EditorEvent.MoveOverlayClip -> run(MoveOverlay(event.clipId, event.startMs))
            is EditorEvent.TrimOverlayClip -> run(TrimOverlay(event.clipId, event.startMs, event.endMs))
            is EditorEvent.OpenFilterTool -> openFilter(event.kind)
            is EditorEvent.FilterPreview -> selectedPictureId()?.let { engine.setClipFilterLive(it, event.filter) }
            is EditorEvent.FilterCommit -> commitFilter(event.filter)
            EditorEvent.FilterResetAdjustments -> selectedPictureId()?.let { run(ResetFilterAdjustments(it)) }
            EditorEvent.FilterRemove -> selectedPictureId()?.let { run(RemoveFilter(it)) }
            EditorEvent.FilterApplyAll -> selectedPictureId()?.let { run(ApplyFilterToAll(it)) }
            EditorEvent.FilterDismiss -> closeFilter()
            is EditorEvent.OpenTextTool -> openText(event.kind)
            is EditorEvent.TextPreview -> previewText(event.clip)
            is EditorEvent.TextCommit -> commitText(event.clip)
            is EditorEvent.TextReset -> resetText(event.kind)
            EditorEvent.TextDismiss -> closeText()
            is EditorEvent.MoveTextClip -> run(MoveText(event.clipId, event.startMs))
            is EditorEvent.TrimTextClip -> run(TrimText(event.clipId, event.startMs, event.endMs))
            else -> Unit
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
        flushText()
        closeSheet()
        closeReverb()
        closeAudio()
        closeFilter()
        closeOverlay()
        local.update { it.copy(selection = selection, text = null) }
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

    private fun selectedPictureId(): String? =
        (local.value.selection as? ClipSelection.Video)?.clipId

    private fun openFilter(kind: FilterToolKind) {
        val id = selectedPictureId() ?: return
        flushText()
        closeSheet()
        closeReverb()
        closeAudio()
        local.update { it.copy(filter = OpenFilter(kind, id), sheet = null, text = null) }
    }

    private fun closeFilter() {
        val open = local.value.filter ?: return
        local.update { it.copy(filter = null) }
        restoreLiveFilter(open.clipId)
    }

    /** A drag that did not become an edit must not leave its live values behind. */
    private fun restoreLiveFilter(clipId: String) {
        session.timeline.value?.videoLane?.firstOrNull { it.id == clipId }
            ?.let { engine.setClipFilterLive(clipId, it.filter) }
    }

    /** Applies each changed part of [filter]; one gesture changes one part, so it is one undo step. */
    private fun commitFilter(filter: ClipFilter) {
        val id = selectedPictureId() ?: return
        val current = session.timeline.value?.videoLane?.firstOrNull { it.id == id }?.filter ?: return
        val commands = buildList {
            if (filter.look != current.look) add(SetFilterLook(id, filter.look))
            val baseIntensity = if (filter.look != current.look) FILTER_INTENSITY_DEFAULT else current.intensity
            if (filter.intensity != baseIntensity) {
                add(SetFilterIntensity(id, filter.intensity))
            }
            val moved = filter.copy(look = current.look, intensity = current.intensity)
            if (moved != current) add(SetFilterAdjustments(id, filter))
        }
        val applied = commands.map { session.apply(it) }.any { it is CommandResult.Applied }
        if (!applied) restoreLiveFilter(id)
    }

    private fun selectedOverlayId(): String? = (local.value.selection as? ClipSelection.Overlay)?.clipId

    private fun addOverlay(media: MediaItem) {
        val at = engine.state.value.positionMs
        val before = session.timeline.value?.overlayLane?.map { it.id }.orEmpty().toSet()
        if (session.apply(AddOverlay(media, at)) !is CommandResult.Applied) {
            say("This file cannot be used as an overlay")
            return
        }
        val added = session.timeline.value?.overlayLane?.firstOrNull { it.id !in before } ?: return
        closeSheet()
        closeReverb()
        closeAudio()
        closeFilter()
        local.update { it.copy(selection = ClipSelection.Overlay(added.id), overlayOpen = false, text = null) }
    }

    private fun openOverlay() {
        if (selectedOverlayId() == null) return
        closeSheet()
        closeReverb()
        closeAudio()
        closeFilter()
        local.update { it.copy(overlayOpen = true, sheet = null, text = null) }
    }

    private fun closeOverlay() {
        val id = selectedOverlayId()
        if (!local.value.overlayOpen) return
        local.update { it.copy(overlayOpen = false) }
        id?.let { restoreLiveOverlay(it) }
    }

    private fun restoreLiveOverlay(id: String) {
        session.timeline.value?.overlayLane?.firstOrNull { it.id == id }?.let(engine::setOverlayLive)
    }

    /** Applies the placement and opacity of [clip] as separate edits; one gesture changes one of them. */
    private fun commitOverlay(clip: OverlayClip) {
        val current = session.timeline.value?.overlayLane?.firstOrNull { it.id == clip.id } ?: return
        val commands = listOfNotNull(
            SetOverlayPlacement(clip.id, clip.xPermille, clip.yPermille, clip.widthPermille).takeIf {
                clip.xPermille != current.xPermille || clip.yPermille != current.yPermille ||
                    clip.widthPermille != current.widthPermille
            },
            SetOverlayOpacity(clip.id, clip.opacityPercent).takeIf { clip.opacityPercent != current.opacityPercent },
        )
        val applied = commands.map { session.apply(it) }.any { it is CommandResult.Applied }
        if (!applied) engine.setOverlayLive(current)
    }

    private fun overlayPanelFor(timeline: Timeline?, selection: ClipSelection?, open: Boolean): OverlayPanelState? {
        if (!open || timeline == null || selection == null) return null
        val clip = timeline.overlayLane.firstOrNull { it.id == selection.clipId } ?: return null
        return OverlayPanelState(clip, timeline.media[clip.mediaItemId]?.displayName.orEmpty())
    }

    private fun overlayAspect(timeline: Timeline?, id: String?): Float {
        val clip = timeline?.overlayLane?.firstOrNull { it.id == id } ?: return DEFAULT_ASPECT
        val media = timeline.media[clip.mediaItemId]
        val w = media?.width ?: return DEFAULT_ASPECT
        val h = media.height ?: return DEFAULT_ASPECT
        return if (h > 0) w.toFloat() / h else DEFAULT_ASPECT
    }

    private var pendingText: TextClip? = null

    private fun selectTab(tab: ToolbarTab) {
        if (!tab.active) return
        local.update { it.copy(tab = tab) }
    }

    private fun addText() {
        flushText()
        val id = UUID.randomUUID().toString()
        val at = engine.state.value.positionMs
        if (session.apply(AddText(at, { id })) !is CommandResult.Applied) return
        closeSheet()
        closeReverb()
        closeAudio()
        local.update { it.copy(selection = ClipSelection.Text(id), text = OpenText(TextToolKind.Edit, id)) }
    }

    private fun openText(kind: TextToolKind) {
        val selection = local.value.selection as? ClipSelection.Text ?: return
        flushText()
        local.update { it.copy(text = OpenText(kind, selection.clipId)) }
    }

    private fun closeText() {
        flushText()
        local.update { it.copy(text = null) }
    }

    private fun previewText(clip: TextClip) {
        pendingText = clip
        engine.setTextLive(clip)
    }

    /** Commits typing or a drag that was only previewed, so nothing is lost when the panel or selection changes. */
    private fun flushText() {
        val pending = pendingText ?: return
        pendingText = null
        commitText(pending)
    }

    /** Applies each changed part of [clip] to the stored clip; a gesture changes one part, so it is one undo step. */
    private fun commitText(clip: TextClip) {
        pendingText = null
        val current = session.timeline.value?.textLane?.firstOrNull { it.id == clip.id } ?: return
        val id = clip.id
        val commands = listOfNotNull(
            SetTextContent(id, clip.text).takeIf { clip.text != current.text },
            SetTextStyle(id, clip.style).takeIf { clip.style != current.style },
            SetTextPlacement(id, clip.xPermille, clip.yPermille, clip.sizePermille).takeIf {
                clip.xPermille != current.xPermille || clip.yPermille != current.yPermille ||
                    clip.sizePermille != current.sizePermille
            },
            SetTextAnimation(id, clip.animIn, clip.animOut).takeIf {
                clip.animIn != current.animIn || clip.animOut != current.animOut
            },
            TrimText(id, clip.startMs, clip.endMs).takeIf {
                clip.startMs != current.startMs || clip.durationMs != current.durationMs
            },
        )
        val applied = commands.map { session.apply(it) }.any { it is CommandResult.Applied }
        if (!applied) engine.setTextLive(current)
    }

    private fun resetText(kind: TextToolKind) {
        val id = (local.value.selection as? ClipSelection.Text)?.clipId ?: return
        val current = session.timeline.value?.textLane?.firstOrNull { it.id == id } ?: return
        when (kind) {
            TextToolKind.Style -> commitText(current.copy(style = TextStyle()))
            TextToolKind.Animation -> commitText(current.copy(animIn = TextAnimation(), animOut = TextAnimation()))
            TextToolKind.Edit -> Unit
        }
    }


    private fun openAudio(kind: AudioToolKind) {
        val timeline = session.timeline.value ?: return
        val selection = local.value.selection?.takeIf { exists(timeline, it) } ?: return
        val reason = disabledToolsFor(timeline, selection)[ClipTool.Audio]
        if (reason != null) {
            say(reason)
            return
        }
        closeReverb()
        closeSheet()
        local.update { it.copy(audio = OpenAudio(kind, selection.clipId), sheet = null) }
    }

    private fun closeAudio() {
        val open = local.value.audio ?: return
        local.update { it.copy(audio = null) }
        restoreLiveFx(open.clipId)
    }

    private fun previewFx(fx: AudioFx) {
        val id = selectedClipId() ?: return
        val length = session.timeline.value?.let { soundClip(it, id)?.second } ?: return
        engine.setClipFxLive(id, fx, length)
    }

    /** Applies each changed part of [fx] to the selected clip; a drag changes one part, so it is one undo step. */
    private fun commitFx(fx: AudioFx) {
        val id = selectedClipId() ?: return
        val current = session.timeline.value?.let { soundClip(it, id)?.first } ?: return
        val commands = listOfNotNull(
            SetFade(id, fx.fade).takeIf { fx.fade != current.fade },
            SetNoiseReduction(id, fx.noise).takeIf { fx.noise != current.noise },
            SetEqualizer(id, fx.eq).takeIf { fx.eq != current.eq },
            SetVoiceFx(id, fx.voice).takeIf { fx.voice != current.voice },
        )
        var applied = false
        for (command in commands) {
            val result = session.apply(command)
            if (result is CommandResult.Applied) {
                applied = true
                if (Notice.FadeLimited in result.notices) say("Fade limited to half the clip")
            }
        }
        if (!applied) restoreLiveFx(id)
    }

    private fun resetFx(kind: AudioToolKind) {
        val id = selectedClipId() ?: return
        val current = session.timeline.value?.let { soundClip(it, id)?.first } ?: return
        commitFx(
            when (kind) {
                AudioToolKind.Fade -> current.copy(fade = Fade())
                AudioToolKind.Noise -> current.copy(noise = NoiseReduction())
                AudioToolKind.Eq -> current.copy(eq = Equalizer())
                AudioToolKind.Voice -> current.copy(voice = VoiceFx())
                AudioToolKind.List -> current
            },
        )
    }

    /** A live drag that did not become an edit must not leave its settings behind. */
    private fun restoreLiveFx(clipId: String) {
        val timeline = session.timeline.value ?: return
        soundClip(timeline, clipId)?.let { (fx, length) -> engine.setClipFxLive(clipId, fx, length) }
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
            ClipTool.Audio -> openAudio(AudioToolKind.List)
            ClipTool.Split -> run(Split(id, engine.state.value.positionMs))
            ClipTool.Detach -> run(DetachAudio(id))
            ClipTool.Duplicate -> run(Duplicate(id))
            ClipTool.Delete -> run(Delete(id))
            ClipTool.Mute -> toggleMute(timeline, id)
            ClipTool.Reverb -> openReverb(timeline, selection)
            ClipTool.Replace -> effectChannel.trySend(EditorEffect.PickReplacementAudio(id))
            ClipTool.FilterLooks -> openFilter(FilterToolKind.Looks)
            ClipTool.OverlayOpacity -> openOverlay()
            ClipTool.FilterAdjust -> openFilter(FilterToolKind.Adjust)
            ClipTool.TextEdit -> openText(TextToolKind.Edit)
            ClipTool.TextStyle -> openText(TextToolKind.Style)
            ClipTool.TextAnim -> openText(TextToolKind.Animation)
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
        if (reason != null) {
            say(reason)
        } else {
            closeAudio()
            local.update { it.copy(reverbClipId = selection.clipId, sheet = null) }
        }
    }

    private fun openSheet(tool: ClipTool, timeline: Timeline, selection: ClipSelection) {
        val reason = disabledToolsFor(timeline, selection)[tool]
        if (reason != null) {
            say(reason)
            return
        }
        closeReverb()
        closeAudio()
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
        val fxList = timeline?.videoLane?.filter { it.attachedAudio }?.map { it.fx to it.reverb }.orEmpty() +
            timeline?.audioLane?.map { it.fx to it.reverb }.orEmpty()
        val reverbOn = fxList.any { it.second.enabled }
        val heavyOn = fxList.any { (fx, _) -> fx.noise.enabled || fx.voice.pitch.enabled || fx.voice.echo.enabled }
        return when {
            heavyOn -> "Preview quality lowered and some effects paused to keep playing"
            reverbOn -> "Preview quality lowered and reverb paused to keep playing"
            else -> "Preview quality lowered to keep playing"
        }
    }

    private fun say(message: String) = local.update { it.copy(message = message) }

    private fun toState(core: Core, local: Local): EditorState {
        val timeline = core.timeline
        val selection = local.selection?.takeIf { timeline != null && exists(timeline, it) }
        val format = timeline?.format
        val sound = if (timeline != null && selection != null) soundClip(timeline, selection.clipId) else null
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
            clipTools = clipToolsFor(timeline, selection, local.tab),
            disabledTools = disabledToolsFor(timeline, selection),
            sheet = sheetFor(timeline, selection, local.sheet),
            selectedAudioMuted = isMuted(timeline, selection?.clipId),
            reverbPanel = reverbPanelFor(timeline, selection, local.reverbClipId),
            audioPanel = audioPanelFor(timeline, selection, local.audio),
            toolbarTab = local.tab,
            overlayPanel = overlayPanelFor(timeline, selection, local.overlayOpen),
            selectedOverlay = timeline?.overlayLane?.firstOrNull { it.id == selection?.clipId },
            selectedOverlayAspect = overlayAspect(timeline, selection?.clipId),
            textPanel = textPanelFor(timeline, selection, local.text),
            filterPanel = filterPanelFor(timeline, selection, local.filter, core.preview.positionMs),
            selectedFilter = selection?.let {
                filterPanelFor(timeline, it, OpenFilter(FilterToolKind.Looks, it.clipId), core.preview.positionMs)
            },
            selectedText = timeline?.textLane?.firstOrNull { it.id == selection?.clipId },
            selectedReverb = selection?.let { sel -> timeline?.let { soundClipReverb(it, sel.clipId) } },
            selectedFx = sound?.first,
            selectedClipLengthMs = sound?.second ?: 0,
            fullscreen = local.fullscreen,
            comingSoon = local.comingSoon,
            message = local.message,
        )
    }


    private companion object {
        const val DEFAULT_FPS = 30
        const val DEFAULT_ASPECT = 16f / 9f
        const val HASH_MIX = 31
        const val BOUNDARY_SLACK_MS = 300L
    }
}

