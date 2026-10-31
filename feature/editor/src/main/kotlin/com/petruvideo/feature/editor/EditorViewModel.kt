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
import com.petruvideo.core.model.Project
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.engine.CommandResult
import com.petruvideo.core.model.engine.MoveVideoClip
import com.petruvideo.core.model.engine.Notice
import com.petruvideo.core.model.engine.RelinkMedia
import com.petruvideo.core.model.engine.RemoveMissingMedia
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
            EditorEvent.AddClip -> { pause(); effectChannel.trySend(EditorEffect.OpenAddMedia) }
            is EditorEvent.RequestRelink -> effectChannel.trySend(EditorEffect.PickRelinkFile(event.mediaItemId))
            EditorEvent.Undo -> session.undo()
            EditorEvent.Redo -> session.redo()
            is EditorEvent.SetFullscreen -> local.update { it.copy(fullscreen = event.on) }
            is EditorEvent.Relink -> relink(event.mediaItemId, event.replacement)
            is EditorEvent.RemoveMissing -> session.apply(RemoveMissingMedia(event.mediaItemId))
            is EditorEvent.TapInactiveTool -> local.update { it.copy(comingSoon = event.name) }
            EditorEvent.DismissMessage -> local.update { it.copy(comingSoon = null, message = null) }
        }
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
        local.update { it.copy(selection = selection) }
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
            PressureNotice.QualityReduced -> say("Preview quality lowered to keep playing")
            PressureNotice.PlaybackPaused -> { pause(); say("Playback paused to cool down") }
            PressureNotice.QualityRestored -> say("Preview quality restored")
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
            fullscreen = local.fullscreen,
            comingSoon = local.comingSoon,
            message = local.message,
        )
    }

    private fun exists(timeline: Timeline, selection: ClipSelection) = when (selection) {
        is ClipSelection.Video -> timeline.videoLane.any { it.id == selection.clipId }
        is ClipSelection.Audio -> timeline.audioLane.any { it.id == selection.clipId }
    }

    private fun clipToolsFor(timeline: Timeline?, selection: ClipSelection?): List<ClipTool> = when (selection) {
        null -> emptyList()
        is ClipSelection.Audio ->
            listOf(ClipTool.Split, ClipTool.Mute, ClipTool.Replace, ClipTool.Duplicate, ClipTool.Delete)
        is ClipSelection.Video -> {
            val attached = timeline?.videoLane?.firstOrNull { it.id == selection.clipId }?.attachedAudio == true
            listOfNotNull(
                ClipTool.Split,
                ClipTool.Speed,
                ClipTool.Volume,
                ClipTool.Detach.takeIf { attached },
                ClipTool.Duplicate,
                ClipTool.Delete,
            )
        }
    }

    private companion object {
        const val DEFAULT_FPS = 30
        const val HASH_MIX = 31
    }
}

