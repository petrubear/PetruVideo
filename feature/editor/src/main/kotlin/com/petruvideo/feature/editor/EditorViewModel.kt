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
import com.petruvideo.core.model.ClipEffect
import com.petruvideo.core.model.EFFECT_MAX
import com.petruvideo.core.model.ClipFilter
import com.petruvideo.core.model.VideoClip
import com.petruvideo.core.model.engine.RemoveTransition
import com.petruvideo.core.model.engine.ResetClipEffect
import com.petruvideo.core.model.engine.SetClipEffect
import com.petruvideo.core.model.engine.SetTransition
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
import com.petruvideo.core.model.Compressor
import com.petruvideo.core.model.CompressorPreset
import com.petruvideo.core.model.CompressorPresets
import com.petruvideo.core.model.ChromaKey
import com.petruvideo.core.model.OverlayMask
import com.petruvideo.core.model.Keyframes
import com.petruvideo.core.model.VOICE_OVER_MIN_MS
import com.petruvideo.core.model.VoiceOverLimit
import com.petruvideo.core.model.VoiceOverStop
import com.petruvideo.core.model.engine.AddVoiceOver
import com.petruvideo.core.model.Pose
import com.petruvideo.core.model.engine.AddAudioClips
import com.petruvideo.core.model.engine.AddKeyframe
import com.petruvideo.core.model.engine.CopyKeyframe
import com.petruvideo.core.model.engine.DeleteKeyframe
import com.petruvideo.core.model.engine.MoveKeyframe
import com.petruvideo.core.model.engine.RejectReason
import com.petruvideo.core.model.CropBox
import com.petruvideo.core.model.CropShape
import com.petruvideo.core.media.chroma.ChromaSampler
import com.petruvideo.core.media.voicesplit.VoiceSplitter
import com.petruvideo.core.media.voicesplit.NoVoiceSplitter
import com.petruvideo.core.media.freeze.FreezeStillMaker
import com.petruvideo.core.media.captions.CaptionMaker
import com.petruvideo.core.media.captions.CaptionResult
import com.petruvideo.core.media.captions.NoCaptionMaker
import com.petruvideo.core.media.captions.RecognitionAvailability
import com.petruvideo.core.media.reverse.NoReversePreparer
import com.petruvideo.core.model.CAPTION_MAX
import com.petruvideo.core.model.Sticker
import com.petruvideo.core.model.engine.AddCaptions
import com.petruvideo.core.model.engine.AddSticker
import com.petruvideo.core.model.engine.SetCanvas
import java.util.Locale
import kotlinx.coroutines.CancellationException
import com.petruvideo.core.media.reverse.ReversePreparer
import com.petruvideo.core.model.REVERSE_MAX_MS
import com.petruvideo.core.model.engine.SetReversed
import kotlinx.coroutines.Job
import com.petruvideo.core.model.FREEZE_DEFAULT_MS
import com.petruvideo.core.model.SpeedCurve
import com.petruvideo.core.model.engine.ApplySpeedPreset
import com.petruvideo.core.model.engine.FlipClip
import com.petruvideo.core.model.engine.ResetSpeedCurve
import com.petruvideo.core.model.engine.SetSpeedCurve
import com.petruvideo.core.model.engine.FreezeFrame
import com.petruvideo.core.model.engine.ResetTransform
import com.petruvideo.core.model.engine.RotateClip
import com.petruvideo.core.model.engine.SetCrop
import com.petruvideo.core.model.engine.SetDucking
import com.petruvideo.core.model.engine.SetKeyframeMotion
import com.petruvideo.core.model.engine.SetPoseAt
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
import com.petruvideo.core.model.engine.SetCompressor
import com.petruvideo.core.model.engine.SetEqualizer
import com.petruvideo.core.model.engine.SetFade
import com.petruvideo.core.model.engine.SetFilterAdjustments
import com.petruvideo.core.model.engine.SetFilterIntensity
import com.petruvideo.core.model.engine.SetFilterLook
import com.petruvideo.core.model.engine.SetMuted
import com.petruvideo.core.model.engine.SetNoiseReduction
import com.petruvideo.core.model.engine.SetChromaKey
import com.petruvideo.core.model.engine.SetOverlayMask
import com.petruvideo.core.model.engine.SetOverlaySound
import com.petruvideo.core.model.engine.VoiceSplitRules
import com.petruvideo.core.model.engine.SplitVoice
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
import com.petruvideo.core.designsystem.UiText

/**
 * Editor state holder. The open project lives in the process-scoped [ProjectSession] and playback in the
 * [PreviewEngine]; this view model only combines them and routes events, so fold, unfold, rotation, and resize
 * never lose the playhead, selection source, or edits.
 */
@Suppress("LargeClass") // The state holder for every editor tool; each tool's logic is small and lives with its event.
@HiltViewModel
class EditorViewModel @Inject constructor(
    private val session: ProjectSession,
    private val engine: PreviewEngine,
    private val availability: MediaAvailabilityChecker,
    private val thumbnails: ThumbnailRefresher,
    private val pressure: ResourcePressureMonitor,
    private val voiceOver: VoiceOverController,
    private val freezeStills: FreezeStillMaker = FreezeStillMaker { _, _ -> null },
    private val chromaSampler: ChromaSampler = ChromaSampler { _, _, _, _ -> null },
    private val voiceSplitter: VoiceSplitter = NoVoiceSplitter,
    private val reverse: ReversePreparer = NoReversePreparer,
    private val captions: CaptionMaker = NoCaptionMaker,
) : ViewModel() {

    private data class Local(
        val selection: ClipSelection? = null,
        val fullscreen: Boolean = false,
        val message: UiText? = null,
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
        /** The open Effects panel: which tool and which clip. */
        val effect: OpenEffect? = null,
        /** Whether the Keyframes panel is open, and the selected keyframe's time inside the clip. */
        val keyframesOpen: Boolean = false,
        val keyframeRel: Long? = null,
        /** The microphone was refused for good, so Record opens the phone's settings. */
        val micBlocked: Boolean = false,
        /** Whether the overlay panel is open (for the selected overlay clip). */
        val overlayOpen: Boolean = false,
        val chromaChecker: Boolean = false,
        val chromaPicking: Boolean = false,
        /** Whether the Transform panel is open, and the crop being edited on its screen. */
        val transformOpen: Boolean = false,
        val freezeOpen: Boolean = false,
        val curveOpen: Boolean = false,
        val captions: CaptionsLocal? = null,
        val stickersOpen: Boolean = false,
        val canvasOpen: Boolean = false,
        /** A running voice split: the clip, how it looked when it started, and the share done. */
        val voiceSplit: VoiceSplitRun? = null,
        /** The curve being dragged on the graph, shown in the panel until it is committed. */
        val curveDraft: SpeedCurve? = null,
        val crop: OpenCrop? = null,
    )

    /** The Auto captions panel: what was checked on this phone and what the user chose. */
    private data class CaptionsLocal(
        val availability: RecognitionAvailability? = null,
        val languages: List<String> = emptyList(),
        val language: String = Locale.getDefault().toLanguageTag(),
        val replace: Boolean = true,
        val progress: Int? = null,
        val note: UiText? = null,
    )

    private fun CaptionsLocal.toPanel() =
        CaptionsPanelState(availability, languages, language, replace, progress, note)

    private data class OpenCrop(val clipId: String, val box: CropBox, val shape: CropShape)

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
            other.timeline.videoLane.map { it.structural() } == timeline.videoLane.map { it.structural() } &&
            other.timeline.audioLane == timeline.audioLane &&
            other.timeline.media == timeline.media &&
            other.timeline.textLane.map { it.id } == timeline.textLane.map { it.id } &&
            other.timeline.overlayLane.map { it.structural() } == timeline.overlayLane.map { it.structural() } &&
            other.textBeyondVideo == textBeyondVideo

        /** Text past the end of the video makes the video sequence run on, which needs a rebuild. */
        private val textBeyondVideo: Long
            get() = (timeline.textLane.maxOfOrNull { it.endMs } ?: 0L).minus(timeline.videoLengthMs).coerceAtLeast(0)

        /** Filter numbers and effect strength change live, so only whether they are on can force a rebuild. */
        private fun VideoClip.structural() =
            copy(filter = filter.structural(), effects = effects.map { it.copy(strength = 0) })

        private fun OverlayClip.structural() =
            copy(xPermille = 0, yPermille = 0, widthPermille = 0, opacityPercent = 0, keyframes = emptyList())

        override fun hashCode() = timeline.videoLane.hashCode() * HASH_MIX + timeline.media.hashCode()
    }

    private val local = MutableStateFlow(Local())
    private val effectChannel = Channel<EditorEffect>(Channel.BUFFERED)
    private val checkedMedia = mutableSetOf<String>()
    private var loadedProjectId: String? = null
    private var reverseJob: Job? = null
    private var captionsJob: Job? = null

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
    ).combine(local, ::toState)
        .combine(voiceOver.ui) { state, take -> state.copy(voiceOver = take) }
        .combine(local) { state, own ->
            state.copy(
                captionsPanel = own.captions?.toPanel(),
                stickersOpen = own.stickersOpen,
                canvasPanel = state.timeline?.canvas?.takeIf { own.canvasOpen },
                voiceSplitProgress = own.voiceSplit?.takeIf { it.clipId == state.selection?.clipId }?.progress,
            )
        }
        .combine(reverse.progress) { state, progress ->
            state.copy(reverseProgress = progress[state.selection?.clipId])
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, EditorState())

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
        viewModelScope.launch {
            session.timeline.filterNotNull().collect { timeline ->
                val run = local.value.voiceSplit ?: return@collect
                if (VoiceSplitRun.of(timeline, run.clipId) != run.snapshot) cancelVoiceSplit(CHANGED_MESSAGE)
            }
        }
        viewModelScope.launch {
            engine.state.map { it.error }.filterNotNull().distinctUntilChanged().collect { onPlaybackError() }
        }
        viewModelScope.launch {
            engine.state.map { it.overlaySoundOff }.distinctUntilChanged().collect { off ->
                if (off) say(UiText.Res(R.string.editor_overlay_sound_plays_in_the))
            }
        }
    }

    fun open(projectId: String) {
        viewModelScope.launch {
            session.open(projectId)
            recoverTake(projectId)
        }
    }

    fun flush() {
        viewModelScope.launch { session.flush() }
    }

    private var wasAway = false

    /** The app left the screen: playback pauses where it is. */
    fun onAway() {
        wasAway = true
        pause()
    }

    /** The app is back: the system may have taken the player's decoders and its picture, so the preview is rebuilt. */
    fun onBack() {
        if (!wasAway) return
        wasAway = false
        engine.rebuild()
    }

    override fun onCleared() {
        engine.release()
    }

    fun onEvent(event: EditorEvent) {
        // While counting in or recording only Stop, Cancel, and the lifecycle stop do anything (spec FR-005a).
        if (voiceOver.active && event !in VOICE_OVER_EVENTS && event !is EditorEvent.DismissMessage) return
        when (event) {
            EditorEvent.RecordVoiceOver -> recordVoiceOver()
            is EditorEvent.MicrophoneResult -> onMicrophone(event.access)
            EditorEvent.StopVoiceOver -> voiceOver.stop()
            EditorEvent.CancelVoiceOver -> voiceOver.cancel()
            EditorEvent.VoiceOverInterrupted -> voiceOver.stop(VoiceOverStop.Interrupted)
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
            EditorEvent.DismissMessage -> local.update { it.copy(message = null) }
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
            is EditorEvent.AddText -> addText(event.defaultText)
            EditorEvent.AddOverlay -> { pause(); effectChannel.trySend(EditorEffect.PickOverlayMedia) }
            is EditorEvent.AddOverlayMedia -> addOverlay(event.media)
            EditorEvent.OpenOverlayPanel -> openOverlay()
            is EditorEvent.OverlayPreview -> previewOverlay(event.clip)
            is EditorEvent.OverlayCommit -> commitOverlay(event.clip)
            EditorEvent.OverlayDismiss -> closeOverlay()
            is EditorEvent.ChromaPreview, is EditorEvent.ChromaCommit, is EditorEvent.ChromaChecker,
            is EditorEvent.ChromaPicking, is EditorEvent.ChromaPick, is EditorEvent.MaskPreview,
            is EditorEvent.MaskCommit, EditorEvent.VoiceSplitCancel -> onChromaEvent(event)
            is EditorEvent.MoveOverlayClip -> run(MoveOverlay(event.clipId, event.startMs))
            is EditorEvent.TrimOverlayClip -> run(TrimOverlay(event.clipId, event.startMs, event.endMs))
            is EditorEvent.OpenFilterTool -> openFilter(event.kind)
            is EditorEvent.FilterPreview -> selectedPictureId()?.let { engine.setClipFilterLive(it, event.filter) }
            is EditorEvent.FilterCommit -> commitFilter(event.filter)
            EditorEvent.FilterResetAdjustments -> selectedPictureId()?.let { run(ResetFilterAdjustments(it)) }
            EditorEvent.FilterRemove -> selectedPictureId()?.let { run(RemoveFilter(it)) }
            EditorEvent.FilterApplyAll -> selectedPictureId()?.let { run(ApplyFilterToAll(it)) }
            EditorEvent.FilterDismiss -> closeFilter()
            is EditorEvent.OpenEffectTool -> openEffect(event.kind)
            is EditorEvent.TransitionSet ->
                selectedPictureId()?.let { run(SetTransition(it, event.kind, event.lengthMs)) }
            EditorEvent.TransitionRemove -> selectedPictureId()?.let { run(RemoveTransition(it)) }
            is EditorEvent.EffectPreview -> selectedPictureId()?.let { engine.setClipEffectLive(it, event.effect) }
            is EditorEvent.EffectCommit -> commitEffect(event.effect)
            is EditorEvent.EffectRemove -> selectedPictureId()?.let { run(ResetClipEffect(it, event.kind)) }
            EditorEvent.EffectDismiss -> closeEffect()
            EditorEvent.AddMusic -> { pause(); effectChannel.trySend(EditorEffect.PickMusic) }
            is EditorEvent.AddMusicMedia -> addMusic(event.media)
            is EditorEvent.OpenTextTool -> openText(event.kind)
            is EditorEvent.TextPreview -> previewText(event.clip)
            is EditorEvent.TextCommit -> commitText(event.clip)
            is EditorEvent.TextReset -> resetText(event.kind)
            EditorEvent.TextDismiss -> closeText()
            is EditorEvent.MoveTextClip -> run(MoveText(event.clipId, event.startMs))
            is EditorEvent.TrimTextClip -> run(TrimText(event.clipId, event.startMs, event.endMs))
            else -> onCaptionsEvent(event)
        }
    }

    /** The events of the Auto captions panel on the Text tab. */
    private fun onCaptionsEvent(event: EditorEvent) {
        when (event) {
            EditorEvent.OpenCaptions -> openCaptions()
            EditorEvent.OpenStickers -> local.update { it.copy(stickersOpen = true, captions = null) }
            EditorEvent.OpenCanvas -> local.update { it.copy(canvasOpen = true) }
            EditorEvent.CanvasDismiss -> local.update { it.copy(canvasOpen = false) }
            is EditorEvent.CanvasSet -> run(SetCanvas(event.canvas))
            EditorEvent.StickersDismiss -> local.update { it.copy(stickersOpen = false) }
            is EditorEvent.AddStickerLayer -> addSticker(event.sticker)
            is EditorEvent.CaptionsLanguage -> {
                updateCaptions { it.copy(language = event.tag, availability = null) }
                viewModelScope.launch { refreshCaptionAvailability() }
            }
            is EditorEvent.CaptionsReplace -> updateCaptions { it.copy(replace = event.on) }
            EditorEvent.CaptionsStart -> startCaptions()
            EditorEvent.CaptionsCancel -> captionsJob?.cancel()
            EditorEvent.CaptionsDismiss -> {
                captionsJob?.cancel()
                local.update { it.copy(captions = null) }
            }
            EditorEvent.CaptionsOpenSettings -> effectChannel.trySend(EditorEffect.OpenSpeechSettings)
            else -> onKeyframeEvent(event)
        }
    }

    /** The events of the Keyframes panel and the video framing gestures. */
    private fun onKeyframeEvent(event: EditorEvent) {
        when (event) {
            EditorEvent.OpenKeyframes -> openKeyframes()
            EditorEvent.KeyframeAdd -> addKeyframe()
            is EditorEvent.KeyframeJump -> jumpKeyframe(event.forward)
            is EditorEvent.KeyframeSelect -> selectKeyframe(event.relMs)
            is EditorEvent.KeyframeMove -> moveKeyframe(event.clipId, event.fromRelMs, event.toRelMs)
            EditorEvent.KeyframeCopyHere -> copyKeyframeHere()
            EditorEvent.KeyframeDelete -> deleteKeyframe()
            is EditorEvent.KeyframeMotion -> setKeyframeMotion(event.ease)
            EditorEvent.KeyframeDismiss -> closeKeyframes()
            is EditorEvent.DuckingChange ->
                (local.value.selection as? ClipSelection.Audio)?.let { run(SetDucking(it.clipId, event.ducking)) }
            is EditorEvent.FramingPreview -> previewFraming(event.pose)
            is EditorEvent.FramingCommit -> commitFraming(event.pose)
            else -> onTransformEvent(event)
        }
    }

    /** The events of the Transform panel and the crop screen. */
    private fun onTransformEvent(event: EditorEvent) {
        val id = (local.value.selection as? ClipSelection.Video)?.clipId ?: return
        when (event) {
            EditorEvent.TransformRotate -> run(RotateClip(id))
            is EditorEvent.TransformFlip -> run(FlipClip(id, event.horizontal))
            EditorEvent.TransformReset -> run(ResetTransform(id))
            EditorEvent.TransformDismiss -> local.update { it.copy(transformOpen = false, crop = null) }
            EditorEvent.CropOpen -> openCrop(id)
            is EditorEvent.SpeedCurvePreset -> commitCurve(ApplySpeedPreset(id, event.preset))
            is EditorEvent.SpeedCurveDraft -> local.update { it.copy(curveDraft = event.curve) }
            is EditorEvent.SpeedCurveCommit -> commitCurve(SetSpeedCurve(id, event.curve))
            EditorEvent.SpeedCurveReset -> commitCurve(ResetSpeedCurve(id))
            EditorEvent.SpeedCurveDismiss -> local.update { it.copy(curveOpen = false, curveDraft = null) }
            EditorEvent.ReverseToggle -> toggleReverse(id)
            EditorEvent.ReverseCancel -> reverseJob?.cancel()
            is EditorEvent.FreezeInsert -> insertFreeze(id, event.lengthMs)
            EditorEvent.FreezeDismiss -> local.update { it.copy(freezeOpen = false) }
            is EditorEvent.CropDrag -> dragCrop(event)
            is EditorEvent.CropShapePick -> pickCropShape(event.shape)
            EditorEvent.CropFullPicture ->
                local.update { s -> s.copy(crop = s.crop?.copy(box = CropBox(), shape = CropShape.Free)) }
            EditorEvent.CropDone -> doneCrop()
            EditorEvent.CropCancel -> local.update { it.copy(crop = null) }
            else -> Unit
        }
    }

    private fun openExport() {
        if (state.value.isEmpty) {
            say(UiText.Res(R.string.editor_add_a_clip_to_export))
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
        closeEffect()
        closeOverlay()
        closeKeyframes()
        local.update {
            it.copy(
                selection = selection, text = null, transformOpen = false, freezeOpen = false, curveOpen = false,
                curveDraft = null, crop = null,
            )
        }
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
        closeEffect()
        local.update { it.copy(filter = OpenFilter(kind, id), sheet = null, text = null) }
    }

    private fun recordVoiceOver() {
        if (local.value.micBlocked) {
            effectChannel.trySend(EditorEffect.OpenAppSettings)
        } else {
            effectChannel.trySend(EditorEffect.RequestMicrophone)
        }
    }

    private fun onMicrophone(access: MicAccess) {
        when (access) {
            MicAccess.Granted -> startVoiceOver()
            MicAccess.Denied -> say(UiText.Res(R.string.editor_a_voiceover_needs_the_microphone))
            MicAccess.Blocked -> {
                local.update { it.copy(micBlocked = true) }
                say(UiText.Res(R.string.editor_microphone_access_is_off_tap))
            }
        }
    }

    private fun startVoiceOver() {
        val timeline = session.timeline.value ?: return
        val projectId = session.project.value?.id ?: return
        local.update { it.copy(micBlocked = false) }
        pause()
        select(null)
        val start = engine.state.value.positionMs
        val free = voiceOver.files.freeBytes()
        val max = VoiceOverLimit.maxDurationMs(start, timeline.audioLane, free)
        if (max < VOICE_OVER_MIN_MS) {
            val storage = VoiceOverLimit.stopReason(start, timeline.audioLane, free) == VoiceOverStop.Storage
            say(
                if (storage) UiText.Res(R.string.editor_not_enough_storage_to_record) else UiText.Res(
                    R.string.editor_no_room_here_before_the,
                ),
            )
            return
        }
        voiceOver.begin(
            viewModelScope, projectId, start, max, VoiceOverLimit.stopReason(start, timeline.audioLane, free),
            onRecording = {
                engine.seekTo(start)
                engine.play()
            },
            onDone = ::onVoiceOverDone,
        )
    }

    private fun onVoiceOverDone(result: VoiceOverResult) {
        engine.pause()
        session.setPlayhead(engine.state.value.positionMs)
        when (result) {
            VoiceOverResult.TooShort -> say(UiText.Res(R.string.editor_too_short))
            VoiceOverResult.MicBusy -> say(UiText.Res(R.string.editor_the_microphone_is_in_use))
            is VoiceOverResult.Take -> addTake(result)
        }
    }

    private fun addTake(take: VoiceOverResult.Take) {
        val item = voiceOver.files.mediaItemFor(take.file, take.durationMs)
        val result = session.apply(AddVoiceOver(item, take.startMs))
        if (result !is CommandResult.Applied) {
            take.file.delete()
            say(UiText.Res(R.string.editor_that_recording_could_not_be))
            return
        }
        val clip = result.timeline.audioLane.firstOrNull { it.mediaItemId == item.id }
        if (clip != null) local.update { it.copy(selection = ClipSelection.Audio(clip.id)) }
        when (take.reason) {
            VoiceOverStop.NextClip -> say(UiText.Res(R.string.editor_stopped_at_the_next_clip))
            VoiceOverStop.Storage -> say(UiText.Res(R.string.editor_storage_is_almost_full_recording))
            else -> Unit
        }
    }

    /** A take left by a crash or a lost process becomes a clip when its project opens (research 5). */
    private fun recoverTake(projectId: String) {
        if (voiceOver.active) return
        val pending = voiceOver.files.pendingTake() ?: return
        if (pending.projectId != projectId) return
        voiceOver.files.closeTake()
        val timeline = session.timeline.value
        val length = VoiceOverLimit.durationOfBytes(pending.file.length())
        val room = timeline?.let { VoiceOverLimit.maxDurationMs(pending.startMs, it.audioLane, Long.MAX_VALUE) } ?: 0L
        val usable = minOf(length, room)
        if (!pending.file.exists() || usable < VOICE_OVER_MIN_MS) {
            pending.file.delete()
            return
        }
        val item = voiceOver.files.mediaItemFor(pending.file, usable)
        if (session.apply(AddVoiceOver(item, pending.startMs)) is CommandResult.Applied) {
            say(UiText.Res(R.string.editor_recovered_your_voiceover))
        } else {
            pending.file.delete()
        }
    }

    private fun keyframeSubject(): KeyframeSubject? {
        val timeline = session.timeline.value ?: return null
        val selection = local.value.selection ?: return null
        return keyframeTarget(timeline, selection)
    }

    private fun openKeyframes() {
        val selection = local.value.selection
        if (selection == null || selection is ClipSelection.Audio) return
        // A video clip draws through its framing effect only once it exists, so ask the preview to build it now.
        if (selection is ClipSelection.Video) engine.armClipFraming(selection.clipId)
        flushText()
        closeSheet()
        closeReverb()
        closeAudio()
        closeFilter()
        closeEffect()
        local.update { it.copy(keyframesOpen = true, overlayOpen = false, sheet = null, text = null) }
    }

    private fun openTransform() {
        if (local.value.selection !is ClipSelection.Video) return
        flushText()
        closeSheet()
        closeReverb()
        closeAudio()
        closeFilter()
        closeEffect()
        closeKeyframes()
        local.update { it.copy(transformOpen = true, overlayOpen = false, sheet = null, text = null) }
    }

    private fun updateCaptions(change: (CaptionsLocal) -> CaptionsLocal) {
        local.update { s -> s.captions?.let { s.copy(captions = change(it)) } ?: s }
    }

    private fun openCaptions() {
        flushText()
        local.update { it.copy(captions = CaptionsLocal()) }
        viewModelScope.launch { refreshCaptionAvailability() }
    }

    private suspend fun refreshCaptionAvailability() {
        val language = local.value.captions?.language ?: return
        val availability = captions.availability(language)
        val installed = captions.languages()
        updateCaptions { it.copy(availability = availability, languages = installed) }
    }

    private fun startCaptions() {
        val panel = local.value.captions ?: return
        val timeline = session.timeline.value ?: return
        if (panel.availability != RecognitionAvailability.Available || captionsJob?.isActive == true) return
        pause()
        captionsJob = viewModelScope.launch {
            updateCaptions { it.copy(progress = 0, note = null) }
            try {
                val result = captions.make(timeline, panel.language) { percent ->
                    updateCaptions { it.copy(progress = percent) }
                }
                val applied = session.apply(AddCaptions(result.lines, panel.replace))
                val note = captionNote(result, applied is CommandResult.Applied)
                updateCaptions { it.copy(progress = null, note = note) }
            } catch (e: CancellationException) {
                updateCaptions { it.copy(progress = null) }
                throw e
            }
        }
    }

    private fun captionNote(result: CaptionResult, applied: Boolean): UiText = when {
        !applied -> UiText.Res(R.string.editor_there_was_no_speech_to)
        else -> UiText.Joined(
            listOfNotNull(
                UiText.Plural(R.plurals.editor_captions_added, result.lines.size, listOf(result.lines.size)),
                UiText.Plural(
                    R.plurals.editor_phrases_not_recognised,
                    result.skippedPhrases,
                    listOf(result.skippedPhrases),
                )
                    .takeIf { result.skippedPhrases > 0 },
                UiText.Plural(R.plurals.editor_clips_skipped, result.skippedClips, listOf(result.skippedClips))
                    .takeIf { result.skippedClips > 0 },
                UiText.Res(R.string.editor_only_the_first_were_made, CAPTION_MAX).takeIf { result.truncated },
            ),
        )
    }

    private fun toggleReverse(id: String) {
        val timeline = session.timeline.value ?: return
        val clip = timeline.videoLane.firstOrNull { it.id == id } ?: return
        if (clip.reversed) return run(SetReversed(id, false))
        val media = timeline.media[clip.mediaItemId] ?: return
        when {
            media.kind != MediaKind.Video -> say(UiText.Res(R.string.editor_photos_cannot_be_reversed))
            clip.sourceLengthMs > REVERSE_MAX_MS -> say(UiText.Res(R.string.editor_too_long_to_reverse_trim))
            !reverse.hasRoom(clip.sourceStartMs, clip.sourceEndMs) -> say(
                UiText.Res(R.string.editor_not_enough_storage_to_reverse),
            )
            reverseJob?.isActive == true -> say(UiText.Res(R.string.editor_another_clip_is_being_reversed))
            else -> reverseJob = viewModelScope.launch {
                if (reverse.prepare(id, media, clip.sourceStartMs, clip.sourceEndMs)) {
                    run(SetReversed(id, true))
                } else {
                    say(UiText.Res(R.string.editor_this_clip_could_not_be))
                }
            }
        }
    }

    private fun openCurve() {
        if (local.value.selection !is ClipSelection.Video) return
        flushText()
        closeSheet()
        closeReverb()
        closeAudio()
        closeFilter()
        closeEffect()
        closeKeyframes()
        local.update {
            it.copy(
                curveOpen = true, curveDraft = null, freezeOpen = false, transformOpen = false, overlayOpen = false,
                sheet = null, text = null,
            )
        }
    }

    private fun commitCurve(command: Command) {
        val result = session.apply(command)
        local.update { it.copy(curveDraft = null) }
        if (result is CommandResult.Rejected && result.reason == RejectReason.BelowMinimumLength) {
            say(UiText.Res(R.string.editor_a_clip_cannot_be_shorter))
        } else if (result is CommandResult.Rejected && command is SetSpeedCurve) {
            say(UiText.Res(R.string.editor_that_speed_curve_does_not))
        }
    }

    private fun openFreeze() {
        if (local.value.selection !is ClipSelection.Video) return
        flushText()
        closeSheet()
        closeReverb()
        closeAudio()
        closeFilter()
        closeEffect()
        closeKeyframes()
        local.update {
            it.copy(freezeOpen = true, transformOpen = false, overlayOpen = false, sheet = null, text = null)
        }
    }

    private fun insertFreeze(id: String, lengthMs: Long) {
        val timeline = session.timeline.value ?: return
        val clip = timeline.videoLane.firstOrNull { it.id == id } ?: return
        val media = timeline.media[clip.mediaItemId] ?: return
        val at = engine.state.value.positionMs
        val start = timeline.videoStartOf(id) ?: return
        if (at < start || at > start + clip.lengthMs) return say(
            UiText.Res(R.string.editor_move_the_playhead_onto_this),
        )
        pause()
        viewModelScope.launch {
            val still = freezeStills.make(media, clip.sourceTimeAt(at - start))
            if (still == null) {
                say(UiText.Res(R.string.editor_that_frame_could_not_be))
            } else {
                run(FreezeFrame(id, at, lengthMs, still))
                local.update { it.copy(freezeOpen = false) }
            }
        }
    }

    private fun cropPicture(id: String): Pair<Int, Int> {
        val timeline = session.timeline.value
        val clip = timeline?.videoLane?.firstOrNull { it.id == id }
        val media = clip?.let { timeline.media[it.mediaItemId] }
        return clip?.transform?.turnedSize(media?.width ?: FALLBACK_WIDTH, media?.height ?: FALLBACK_HEIGHT)
            ?: (FALLBACK_WIDTH to FALLBACK_HEIGHT)
    }

    private fun openCrop(id: String) {
        val clip = session.timeline.value?.videoLane?.firstOrNull { it.id == id } ?: return
        local.update { it.copy(crop = OpenCrop(id, clip.transform.crop, CropShape.Free)) }
    }

    private fun dragCrop(event: EditorEvent.CropDrag) {
        val open = local.value.crop ?: return
        val (w, h) = cropPicture(open.clipId)
        val box = event.handle?.let { open.box.resized(it, event.dx, event.dy, open.shape.aspect, w, h) }
            ?: open.box.moved(event.dx, event.dy)
        local.update { it.copy(crop = open.copy(box = box)) }
    }

    private fun pickCropShape(shape: CropShape) {
        val open = local.value.crop ?: return
        val (w, h) = cropPicture(open.clipId)
        val box = shape.aspect?.let { CropBox.forShape(it, w, h) } ?: open.box
        local.update { it.copy(crop = open.copy(box = box, shape = shape)) }
    }

    private fun doneCrop() {
        val open = local.value.crop ?: return
        run(SetCrop(open.clipId, open.box))
        local.update { it.copy(crop = null) }
    }

    private fun closeKeyframes() {
        if (local.value.keyframesOpen || local.value.keyframeRel != null) {
            local.update { it.copy(keyframesOpen = false, keyframeRel = null) }
        }
    }

    /** The selected video clip with a keyframe at the playhead holding [pose], or null when that is not possible. */
    private fun framedClip(pose: Pose): Pair<String, com.petruvideo.core.model.VideoClip>? {
        val id = (local.value.selection as? ClipSelection.Video)?.clipId ?: return null
        val timeline = session.timeline.value ?: return null
        val result = SetPoseAt(id, engine.state.value.positionMs, pose).apply(timeline) as? CommandResult.Applied
        return result?.timeline?.videoLane?.firstOrNull { it.id == id }?.let { id to it }
    }

    private fun previewFraming(pose: Pose) {
        framedClip(pose)?.let { engine.setClipFramingLive(it.second) }
    }

    private fun commitFraming(pose: Pose) {
        val id = (local.value.selection as? ClipSelection.Video)?.clipId ?: return
        val result = session.apply(SetPoseAt(id, engine.state.value.positionMs, pose))
        if (result is CommandResult.Rejected && result.reason == RejectReason.LimitReached) {
            say(UiText.Res(R.string.editor_a_clip_can_have_50))
        }
        // A drag that did not become an edit must not leave its live values behind.
        session.timeline.value?.videoLane?.firstOrNull { it.id == id }?.let { engine.setClipFramingLive(it) }
    }

    private fun addKeyframe() {
        val id = selectedClipId() ?: return
        val subject = keyframeSubject() ?: return
        val at = engine.state.value.positionMs
        val result = session.apply(AddKeyframe(id, at))
        if (result is CommandResult.Rejected && result.reason == RejectReason.LimitReached) {
            say(UiText.Res(R.string.editor_a_clip_can_have_50))
            return
        }
        val rel = (at - subject.startMs).coerceIn(0, subject.durationMs)
        val frames = keyframeSubject()?.frames.orEmpty()
        val near = frames.minByOrNull { kotlin.math.abs(it.timeMs - rel) } ?: return
        local.update { it.copy(keyframeRel = near.timeMs) }
    }

    private fun jumpKeyframe(forward: Boolean) {
        val subject = keyframeSubject() ?: return
        val rel = engine.state.value.positionMs - subject.startMs
        val target = if (forward) {
            subject.frames.firstOrNull { it.timeMs > rel }
        } else {
            subject.frames.lastOrNull { it.timeMs < rel }
        } ?: return
        selectKeyframe(target.timeMs)
    }

    private fun selectKeyframe(relMs: Long) {
        val subject = keyframeSubject() ?: return
        local.update { it.copy(keyframeRel = relMs) }
        seek(subject.startMs + relMs)
    }

    private fun moveKeyframe(clipId: String, from: Long, to: Long) {
        if (session.apply(MoveKeyframe(clipId, from, to)) is CommandResult.Applied) {
            local.update { it.copy(keyframeRel = to) }
        }
    }

    private fun copyKeyframeHere() {
        val id = selectedClipId() ?: return
        val rel = local.value.keyframeRel ?: return
        val subject = keyframeSubject() ?: return
        val at = engine.state.value.positionMs
        val result = session.apply(CopyKeyframe(id, rel, at))
        if (result is CommandResult.Rejected && result.reason == RejectReason.LimitReached) {
            say(UiText.Res(R.string.editor_a_clip_can_have_50))
        } else if (result is CommandResult.Applied) {
            local.update { it.copy(keyframeRel = (at - subject.startMs).coerceIn(0, subject.durationMs)) }
        }
    }

    private fun deleteKeyframe() {
        val id = selectedClipId() ?: return
        val rel = local.value.keyframeRel ?: return
        if (session.apply(DeleteKeyframe(id, rel)) is CommandResult.Applied) {
            local.update { it.copy(keyframeRel = null) }
        }
    }

    private fun setKeyframeMotion(ease: Boolean) {
        val id = selectedClipId() ?: return
        val rel = local.value.keyframeRel ?: return
        run(SetKeyframeMotion(id, rel, ease))
    }

    private fun openEffect(kind: EffectToolKind) {
        val id = selectedPictureId() ?: return
        val timeline = session.timeline.value ?: return
        if (kind == EffectToolKind.Transition) {
            disabledToolsFor(timeline, ClipSelection.Video(id))[ClipTool.EffectTransition]?.let { return say(it) }
        }
        flushText()
        closeSheet()
        closeReverb()
        closeAudio()
        closeFilter()
        local.update { it.copy(effect = OpenEffect(kind, id), sheet = null, text = null) }
    }

    private fun closeEffect() {
        val open = local.value.effect ?: return
        local.update { it.copy(effect = null) }
        // A drag that did not become an edit must not leave its live strength behind.
        session.timeline.value?.videoLane?.firstOrNull { it.id == open.clipId }?.effects
            ?.forEach { engine.setClipEffectLive(open.clipId, it) }
    }

    private fun commitEffect(effect: ClipEffect) {
        val id = selectedPictureId() ?: return
        val result = session.apply(SetClipEffect(id, effect.kind, effect.strength))
        if (result is CommandResult.Rejected && result.reason == RejectReason.LimitReached) {
            say(UiText.Res(R.string.editor_effect_limit, EFFECT_MAX))
        }
        session.timeline.value?.videoLane?.firstOrNull { it.id == id }?.effects
            ?.forEach { engine.setClipEffectLive(id, it) }
    }

    private fun addMusic(media: MediaItem) {
        val at = engine.state.value.positionMs
        val result = session.apply(AddAudioClips(listOf(media), at))
        if (result !is CommandResult.Applied) say(UiText.Res(R.string.editor_that_file_cant_be_used))
        else if (Notice.Snapped in result.notices) say(UiText.Res(R.string.editor_music_moved_to_the_next))
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
            say(UiText.Res(R.string.editor_this_file_cannot_be_used))
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
        local.update { it.copy(overlayOpen = false, chromaPicking = false) }
        id?.let { restoreLiveOverlay(it) }
    }

    private fun onChromaEvent(event: EditorEvent) {
        when (event) {
            is EditorEvent.ChromaPreview -> previewChroma(event.key)
            is EditorEvent.ChromaCommit -> selectedOverlayId()?.let { run(SetChromaKey(it, event.key)) }
            is EditorEvent.ChromaChecker -> setChromaChecker(event.on)
            is EditorEvent.ChromaPicking -> local.update { it.copy(chromaPicking = event.on) }
            is EditorEvent.ChromaPick -> pickChroma(event.xPermille, event.yPermille)
            is EditorEvent.MaskPreview -> previewMask(event.mask)
            is EditorEvent.MaskCommit -> selectedOverlayId()?.let { run(SetOverlayMask(it, event.mask)) }
            EditorEvent.VoiceSplitCancel -> cancelVoiceSplit(UiText.Res(R.string.editor_split_cancelled))
            else -> Unit
        }
    }

    private fun previewChroma(key: ChromaKey) {
        val stored = selectedOverlayId()
            ?.let { id -> session.timeline.value?.overlayLane?.firstOrNull { it.id == id } } ?: return
        engine.setOverlayLive(stored.copy(chroma = key.normalized()))
    }

    private var voiceSplitJob: Job? = null

    /** Separates the voice of the clip in the background; editing the clip meanwhile cancels it (feature 027). */
    private fun startVoiceSplit(timeline: Timeline, clipId: String) {
        VoiceSplitRules.reasonNot(timeline, clipId)?.let { return say(it.text()) }
        if (local.value.voiceSplit != null) return say(UiText.Res(R.string.editor_a_voice_split_is_already))
        val snapshot = VoiceSplitRun.of(timeline, clipId) ?: return
        val media = timeline.media[snapshot.mediaItemId] ?: return
        local.update { it.copy(voiceSplit = VoiceSplitRun(clipId, snapshot, 0)) }
        voiceSplitJob = viewModelScope.launch {
            try {
                val stems = voiceSplitter.split(media, snapshot.sourceStartMs, snapshot.sourceEndMs) { percent ->
                    local.update { state -> state.copy(voiceSplit = state.voiceSplit?.copy(progress = percent)) }
                }
                // The run ends before the edit, so the edit is not mistaken for a change of the clip.
                local.update { it.copy(voiceSplit = null) }
                if (stems == null) {
                    say(UiText.Res(R.string.editor_could_not_split_the_voice))
                } else {
                    val result = session.apply(SplitVoice(clipId, stems.voice, stems.backing))
                    if (result is CommandResult.Rejected) {
                        say(
                            if (result.reason == RejectReason.LimitReached) {
                                UiText.Res(R.string.editor_up_to_3_audio_layers)
                            } else {
                                UiText.Res(R.string.editor_could_not_split_the_voice)
                            },
                        )
                    }
                }
            } finally {
                local.update { it.copy(voiceSplit = null) }
            }
        }
    }

    private fun cancelVoiceSplit(message: UiText) {
        if (local.value.voiceSplit == null) return
        voiceSplitJob?.cancel()
        local.update { it.copy(voiceSplit = null) }
        say(message)
    }

    private fun previewMask(mask: OverlayMask) {
        val stored = selectedOverlayId()
            ?.let { id -> session.timeline.value?.overlayLane?.firstOrNull { it.id == id } } ?: return
        engine.setOverlayLive(stored.copy(mask = mask.normalized()))
    }

    private fun setChromaChecker(on: Boolean) {
        local.update { it.copy(chromaChecker = on) }
        engine.setChromaChecker(on)
    }

    /** The colour of the selected overlay's picture under a tap on the preview becomes its key colour. */
    private fun pickChroma(xPermille: Int, yPermille: Int) {
        val timeline = session.timeline.value ?: return
        val clip = timeline.overlayLane.firstOrNull { it.id == selectedOverlayId() } ?: return
        val media = timeline.media[clip.mediaItemId] ?: return
        val format = timeline.format ?: return
        val at = engine.state.value.positionMs
        val shown = clip.shownAt(at)
        val aspect = overlayAspect(timeline, clip.id)
        // The overlay is shown [widthPermille] of the frame wide and as tall as its own shape makes it.
        val wide = shown.widthPermille.toFloat()
        val high = wide / aspect * format.width / format.height
        val u = (xPermille - (shown.xPermille - wide / 2)) / wide
        val tallPermille = high
        val v = (yPermille - (shown.yPermille - tallPermille / 2)) / tallPermille
        local.update { it.copy(chromaPicking = false) }
        viewModelScope.launch {
            val source = clip.sourceStartMs + (at - clip.startMs).coerceIn(0, clip.durationMs)
            val color = chromaSampler.sample(media, source, u, v) ?: return@launch say(
                UiText.Res(R.string.editor_tap_on_the_overlays_picture),
            )
            run(SetChromaKey(clip.id, clip.chroma.copy(enabled = true, color = color)))
        }
    }

    private fun restoreLiveOverlay(id: String) {
        session.timeline.value?.overlayLane?.firstOrNull { it.id == id }?.let(engine::setOverlayLive)
    }

    /** Applies the placement and opacity of [clip] as separate edits; one gesture changes one of them. */
    private fun commitOverlay(clip: OverlayClip) {
        val stored = session.timeline.value?.overlayLane?.firstOrNull { it.id == clip.id } ?: return
        // The clip comes from the preview box as shown at the playhead, so it is compared with what is shown there.
        val at = engine.state.value.positionMs
        val current = stored.shownAt(at)
        val commands = listOfNotNull(
            SetOverlayPlacement(clip.id, clip.xPermille, clip.yPermille, clip.widthPermille, at).takeIf {
                clip.xPermille != current.xPermille || clip.yPermille != current.yPermille ||
                    clip.widthPermille != current.widthPermille
            },
            SetOverlayOpacity(clip.id, clip.opacityPercent, at)
                .takeIf { clip.opacityPercent != current.opacityPercent },
        )
        val applied = commands.map { session.apply(it) }.any { it is CommandResult.Applied }
        if (!applied) engine.setOverlayLive(stored)
    }

    /** A drag or slider on an animated clip shows live as a keyframe at the playhead; nothing is stored yet. */
    private fun previewOverlay(clip: OverlayClip) {
        val stored = session.timeline.value?.overlayLane?.firstOrNull { it.id == clip.id }
        if (stored == null || stored.keyframes.isEmpty()) return engine.setOverlayLive(clip)
        val rel = (engine.state.value.positionMs - stored.startMs).coerceIn(0, stored.durationMs)
        val pose = Pose(clip.xPermille, clip.yPermille, clip.widthPermille, clip.opacityPercent)
        Keyframes.withPoseAt(stored.keyframes, rel, pose)?.let { engine.setOverlayLive(stored.withKeyframes(it)) }
    }

    private fun overlayPanelFor(
        timeline: Timeline?,
        selection: ClipSelection?,
        open: Boolean,
        playheadMs: Long,
        local: Local,
    ): OverlayPanelState? {
        if (!open || timeline == null || selection == null) return null
        val clip = timeline.overlayLane.firstOrNull { it.id == selection.clipId } ?: return null
        return OverlayPanelState(
            clip.shownAt(playheadMs), timeline.media[clip.mediaItemId]?.displayName.orEmpty(),
            local.chromaChecker, local.chromaPicking,
        )
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
        local.update { it.copy(tab = tab) }
    }

    private fun addText(defaultText: String) {
        flushText()
        val id = UUID.randomUUID().toString()
        val at = engine.state.value.positionMs
        if (session.apply(AddText(at, { id }, defaultText)) !is CommandResult.Applied) return
        closeSheet()
        closeReverb()
        closeAudio()
        local.update { it.copy(selection = ClipSelection.Text(id), text = OpenText(TextToolKind.Edit, id)) }
    }

    private fun addSticker(sticker: Sticker) {
        flushText()
        val id = UUID.randomUUID().toString()
        val at = engine.state.value.positionMs
        if (session.apply(AddSticker(at, sticker) { id }) !is CommandResult.Applied) return
        closeSheet()
        closeReverb()
        closeAudio()
        local.update { it.copy(selection = ClipSelection.Text(id), text = null) }
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
        val stored = session.timeline.value?.textLane?.firstOrNull { it.id == clip.id }
        if (stored == null || stored.keyframes.isEmpty()) return engine.setTextLive(clip)
        val rel = (engine.state.value.positionMs - stored.startMs).coerceIn(0, stored.durationMs)
        val pose = Pose(clip.xPermille, clip.yPermille, clip.sizePermille, stored.poseAt(rel).opacityPercent)
        Keyframes.withPoseAt(stored.keyframes, rel, pose)
            ?.let { engine.setTextLive(stored.copy(text = clip.text, style = clip.style).withKeyframes(it)) }
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
        val stored = session.timeline.value?.textLane?.firstOrNull { it.id == clip.id } ?: return
        val at = engine.state.value.positionMs
        val current = stored.shownAt(at)
        val id = clip.id
        val commands = listOfNotNull(
            SetTextContent(id, clip.text).takeIf { clip.text != current.text },
            SetTextStyle(id, clip.style).takeIf { clip.style != current.style },
            SetTextPlacement(id, clip.xPermille, clip.yPermille, clip.sizePermille, at).takeIf {
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
        if (!applied) engine.setTextLive(stored)
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
            SetCompressor(id, fx.compressor).takeIf { fx.compressor != current.compressor },
            SetVoiceFx(id, fx.voice).takeIf { fx.voice != current.voice },
        )
        var applied = false
        for (command in commands) {
            val result = session.apply(command)
            if (result is CommandResult.Applied) {
                applied = true
                if (Notice.FadeLimited in result.notices) say(UiText.Res(R.string.editor_fade_limited_to_half_the))
            }
        }
        if (!applied) restoreLiveFx(id)
    }

    /** Back to the chosen preset (Voice when it is Custom), keeping the compressor on or off. */
    private fun resetCompressor(current: Compressor): Compressor {
        val preset = current.preset.takeIf { it != CompressorPreset.Custom } ?: CompressorPreset.Voice
        return CompressorPresets.settings(preset, current.enabled)
    }

    private fun resetFx(kind: AudioToolKind) {
        val id = selectedClipId() ?: return
        val current = session.timeline.value?.let { soundClip(it, id)?.first } ?: return
        commitFx(
            when (kind) {
                AudioToolKind.Fade -> current.copy(fade = Fade())
                AudioToolKind.Noise -> current.copy(noise = NoiseReduction())
                AudioToolKind.Eq -> current.copy(eq = Equalizer())
                AudioToolKind.Compressor -> current.copy(compressor = resetCompressor(current.compressor))
                AudioToolKind.Voice -> current.copy(voice = VoiceFx())
                AudioToolKind.List, AudioToolKind.Ducking -> current
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

    private fun speedLimitMessage(timeline: Timeline, clipId: String): UiText {
        val milli = timeline.videoLane.firstOrNull { it.id == clipId }?.speedMilli
            ?: timeline.audioLane.firstOrNull { it.id == clipId }?.speedMilli
            ?: return UiText.Res(R.string.editor_speed_limited)
        val audio = timeline.audioLane.any { it.id == clipId }
        return if (audio && milli < SPEED_UNIT) {
            UiText.Res(R.string.editor_limited_to_avoid_overlap, formatSpeed(milli))
        } else {
            UiText.Res(R.string.editor_limited_to, formatSpeed(milli))
        }
    }

    /** Applies [command] and tells the user when audio snapped or a replacement shortened a clip. */
    private fun run(command: Command) {
        val result = session.apply(command) as? CommandResult.Applied ?: return
        when {
            Notice.Snapped in result.notices -> say(UiText.Res(R.string.editor_audio_moved_to_the_next))
            Notice.LengthChanged in result.notices -> say(UiText.Res(R.string.editor_clip_shortened_to_fit_the))
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
            ClipTool.Detach -> detach(id)
            ClipTool.SoundOn -> run(SetOverlaySound(id, true))
            ClipTool.SplitVoice -> startVoiceSplit(timeline, id)
            ClipTool.Duplicate -> run(Duplicate(id))
            ClipTool.Delete -> run(Delete(id))
            ClipTool.Mute -> toggleMute(timeline, id)
            ClipTool.Reverb -> openReverb(timeline, selection)
            ClipTool.Replace -> effectChannel.trySend(EditorEffect.PickReplacementAudio(id))
            ClipTool.FilterLooks -> openFilter(FilterToolKind.Looks)
            ClipTool.OverlayOpacity -> openOverlay()
            ClipTool.FilterAdjust -> openFilter(FilterToolKind.Adjust)
            ClipTool.Keyframes -> openKeyframes()
            ClipTool.Transform -> openTransform()
            ClipTool.Freeze -> openFreeze()
            ClipTool.Reverse -> toggleReverse(id)
            ClipTool.Curve -> openCurve()
            ClipTool.EffectTransition -> openEffect(EffectToolKind.Transition)
            ClipTool.EffectFx -> openEffect(EffectToolKind.Effect)
            ClipTool.TextEdit -> openText(TextToolKind.Edit)
            ClipTool.TextStyle -> openText(TextToolKind.Style)
            ClipTool.TextAnim -> openText(TextToolKind.Animation)
        }
    }

    private fun detach(id: String) {
        val result = session.apply(DetachAudio(id))
        when {
            result is CommandResult.Rejected && result.reason == RejectReason.LimitReached ->
                say(UiText.Res(R.string.editor_up_to_3_audio_layers))
            result is CommandResult.Applied && Notice.Snapped in result.notices ->
                say(UiText.Res(R.string.editor_audio_moved_to_the_next))
        }
    }

    private fun toggleMute(timeline: Timeline, id: String) {
        val audio = timeline.audioLane.firstOrNull { it.id == id }
        val video = timeline.videoLane.firstOrNull { it.id == id }
        val overlay = timeline.overlayLane.firstOrNull { it.id == id }
        when {
            audio != null -> run(SetMuted(id, !audio.muted))
            overlay?.attachedAudio == true -> run(SetMuted(id, !overlay.muted))
            video?.attachedAudio == true -> run(SetVideoMuted(id, !video.muted))
            else -> say(UiText.Res(R.string.editor_this_clip_has_no_sound))
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
                local.update { it.copy(message = UiText.Res(R.string.editor_clip_shortened_to_fit_the)) }
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

    /** Playback stopped: a source file that went away since the project opened is the usual reason, so look again. */
    private suspend fun onPlaybackError() {
        val timeline = session.timeline.value ?: return
        session.setAvailability(availability.check(timeline.media.values.toList()))
        val missing = session.timeline.value?.media?.values?.firstOrNull { it.availability == Availability.Missing }
        say(
            if (missing != null) {
                UiText.Res(R.string.editor_media_missing_relink, missing.displayName)
            } else {
                engine.state.value.error?.let { UiText.Res(R.string.editor_playback_stopped_code, it) }
                    ?: UiText.Res(R.string.editor_playback_stopped)
            },
        )
    }

    private fun onPressureNotice(notice: PressureNotice) {
        when (notice) {
            PressureNotice.QualityReduced -> say(qualityReducedMessage())
            PressureNotice.PlaybackPaused -> { pause(); say(UiText.Res(R.string.editor_playback_paused_to_cool_down)) }
            PressureNotice.QualityRestored -> say(UiText.Res(R.string.editor_preview_quality_restored))
        }
    }

    private fun qualityReducedMessage(): UiText {
        val timeline = session.timeline.value
        val fxList = timeline?.videoLane?.filter { it.attachedAudio }?.map { it.fx to it.reverb }.orEmpty() +
            timeline?.audioLane?.map { it.fx to it.reverb }.orEmpty()
        val reverbOn = fxList.any { it.second.enabled }
        val heavyOn = fxList.any { (fx, _) -> fx.noise.enabled || fx.voice.pitch.enabled || fx.voice.echo.enabled }
        return when {
            heavyOn -> UiText.Res(R.string.editor_preview_quality_lowered_and_some)
            reverbOn -> UiText.Res(R.string.editor_preview_quality_lowered_and_reverb)
            else -> UiText.Res(R.string.editor_preview_quality_lowered_to_keep)
        }
    }

    private fun say(message: UiText) = local.update { it.copy(message = message) }

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
            chromaChecker = local.chromaChecker,
            chromaPicking = local.chromaPicking,
            overlayPanel = overlayPanelFor(timeline, selection, local.overlayOpen, core.preview.positionMs, local),
            selectedOverlay = timeline?.overlayLane?.firstOrNull { it.id == selection?.clipId }
                ?.shownAt(core.preview.positionMs),
            selectedOverlayAspect = overlayAspect(timeline, selection?.clipId),
            textPanel = textPanelFor(timeline, selection, local.text, core.preview.positionMs),
            transformPanel = transformPanelFor(timeline, selection, local.transformOpen),
            freezePanel = freezePanelFor(selection, local.freezeOpen),
            curvePanel = curvePanelFor(timeline, selection, local.curveOpen, local.curveDraft),
            selectedReversed = timeline?.videoLane?.firstOrNull { it.id == selection?.clipId }?.reversed == true,
            crop = cropSessionFor(timeline, local.crop?.let { Triple(it.clipId, it.box, it.shape) }),
            keyframePanel = keyframePanelFor(
                timeline, selection, local.keyframesOpen, local.keyframeRel, core.preview.positionMs,
            ),
            selectedKeyframeMs = local.keyframeRel,
            effectPanel = effectPanelFor(timeline, selection, local.effect),
            filterPanel = filterPanelFor(timeline, selection, local.filter, core.preview.positionMs),
            selectedFilter = selection?.let {
                filterPanelFor(timeline, it, OpenFilter(FilterToolKind.Looks, it.clipId), core.preview.positionMs)
            },
            selectedText = timeline?.textLane?.firstOrNull { it.id == selection?.clipId }
                ?.shownAt(core.preview.positionMs),
            selectedReverb = selection?.let { sel -> timeline?.let { soundClipReverb(it, sel.clipId) } },
            selectedFx = sound?.first,
            selectedClipLengthMs = sound?.second ?: 0,
            fullscreen = local.fullscreen,
            message = local.message,
        )
    }


    private companion object {
        val CHANGED_MESSAGE = UiText.Res(R.string.editor_split_cancelled_because_the_clip)

        val VOICE_OVER_EVENTS = setOf<EditorEvent>(
            EditorEvent.StopVoiceOver, EditorEvent.CancelVoiceOver, EditorEvent.VoiceOverInterrupted,
        )
        const val DEFAULT_FPS = 30
        const val DEFAULT_ASPECT = 16f / 9f
        const val HASH_MIX = 31
        const val BOUNDARY_SLACK_MS = 300L
    }
}

