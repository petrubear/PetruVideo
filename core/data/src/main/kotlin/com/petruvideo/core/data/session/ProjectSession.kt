package com.petruvideo.core.data.session

import com.petruvideo.core.data.autosave.AutosaveWriter
import com.petruvideo.core.data.autosave.SaveStatus
import com.petruvideo.core.data.repo.ProjectRepository
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.Project
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.engine.AddAudioClips
import com.petruvideo.core.model.engine.AddVideoClips
import com.petruvideo.core.model.engine.AddVideoItem
import com.petruvideo.core.model.engine.Command
import com.petruvideo.core.model.engine.CommandResult
import com.petruvideo.core.model.engine.EditHistory
import com.petruvideo.core.model.engine.Notice
import com.petruvideo.core.model.engine.RejectReason
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class HistoryState(
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val undoLabel: String? = null,
    val redoLabel: String? = null,
)

/**
 * The open project: its timeline, edit history, playhead, and autosave. The instance is scoped to the process
 * (not to a screen), so fold, unfold, rotation, and resize never lose the project, playhead, or unsaved edits.
 */
class ProjectSession(
    private val repository: ProjectRepository,
    scope: CoroutineScope,
) {
    private val autosave = AutosaveWriter(scope, repository::saveTimeline)
    private var editHistory: EditHistory? = null

    private val _project = MutableStateFlow<Project?>(null)
    private val _timeline = MutableStateFlow<Timeline?>(null)
    private val _history = MutableStateFlow(HistoryState())

    val project: StateFlow<Project?> = _project
    val timeline: StateFlow<Timeline?> = _timeline
    val history: StateFlow<HistoryState> = _history
    val saveStatus: StateFlow<SaveStatus> = autosave.status

    /** Opens a project, replacing the current one. Pending changes of the previous project are saved first. */
    suspend fun open(projectId: String): Boolean {
        flush()
        val loaded = repository.loadProject(projectId) ?: return false
        editHistory = EditHistory(loaded.timeline)
        _project.value = loaded.project
        publish()
        return true
    }

    fun apply(command: Command): CommandResult {
        val history = editHistory ?: return CommandResult.Rejected(RejectReason.NotFound)
        val result = history.apply(command)
        if (result is CommandResult.Applied) publishAndSave()
        return result
    }

    fun undo() {
        if (editHistory?.undo() == true) publishAndSave()
    }

    fun redo() {
        if (editHistory?.redo() == true) publishAndSave()
    }

    /** Moves the playhead. It is saved with the project but is not an undo step. */
    fun setPlayhead(ms: Long) {
        val history = editHistory ?: return
        history.replaceCurrent(history.current.copy(playheadMs = ms.coerceAtLeast(0)))
        publishAndSave()
    }

    /**
     * Adds imported media in selection order: videos and photos after the clip under the playhead, audio files
     * at the playhead on the audio lane. Returns the notices of the applied commands (e.g. a snapped audio clip).
     */
    fun addMedia(items: List<MediaItem>): List<Notice> {
        val playhead = _timeline.value?.playheadMs ?: return emptyList()
        val visual = items.filter { it.kind != MediaKind.Audio }
        val audio = items.filter { it.kind == MediaKind.Audio }
        val results = buildList {
            if (visual.isNotEmpty()) add(apply(AddVideoClips(visual.map(::AddVideoItem), playhead)))
            if (audio.isNotEmpty()) add(apply(AddAudioClips(audio, playhead)))
        }
        return results.filterIsInstance<CommandResult.Applied>().flatMap { it.notices }
    }

    suspend fun flush() = autosave.flush()

    /** Retries a failed save, e.g. after the user freed storage. */
    suspend fun retrySave() = autosave.retry()

    suspend fun close() {
        flush()
        editHistory = null
        _project.value = null
        _timeline.value = null
        _history.value = HistoryState()
    }

    private fun publish() {
        val history = editHistory ?: return
        _timeline.value = history.current
        _history.value = HistoryState(history.canUndo, history.canRedo, history.undoLabel, history.redoLabel)
    }

    private fun publishAndSave() {
        publish()
        val projectId = _project.value?.id ?: return
        val timeline = _timeline.value ?: return
        autosave.submit(projectId, timeline)
    }
}
