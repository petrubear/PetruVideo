package com.petruvideo.feature.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petruvideo.core.data.repo.ExportRepository
import com.petruvideo.core.data.repo.SettingsRepository
import com.petruvideo.core.data.session.ProjectSession
import com.petruvideo.core.export.EncoderCapabilities
import com.petruvideo.core.export.ExportCoordinator
import com.petruvideo.core.export.ExportServiceStarter
import com.petruvideo.core.model.export.ExportCaps
import com.petruvideo.core.model.export.ExportFailure
import com.petruvideo.core.model.export.ExportSettings
import com.petruvideo.core.model.export.ExportState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State for the export screen. The render itself lives in the process-scoped [ExportCoordinator], so folding,
 * unfolding, rotating, or leaving the screen never touches it.
 */
@HiltViewModel
class ExportViewModel @Inject constructor(
    private val session: ProjectSession,
    private val coordinator: ExportCoordinator,
    private val encoders: EncoderCapabilities,
    private val repository: ExportRepository,
    private val starter: ExportServiceStarter,
    private val appSettings: SettingsRepository,
) : ViewModel() {
    private val projectId = MutableStateFlow<String?>(null)
    private val chosen = MutableStateFlow<ExportSettings?>(null)
    private val interrupted = MutableStateFlow(false)
    private val effectChannel = Channel<ExportEffect>(Channel.BUFFERED)

    val effects = effectChannel.receiveAsFlow()

    val state: StateFlow<ExportUiState> = combine(
        session.project,
        session.timeline,
        chosen,
        coordinator.job,
        interrupted,
    ) { project, timeline, picked, job, wasInterrupted ->
        if (project == null || timeline == null || project.id != projectId.value) return@combine ExportUiState()
        val caps = ExportCaps.from(timeline)
        val device = encoders.limits
        val first = caps.defaults(timeline.format?.fps ?: DEFAULT_FPS, device)
            .copy(quality = appSettings.settings.value.defaultQuality)
        val settings = caps.clamp(picked ?: first, device)
        val plan = coordinator.plan(timeline, settings)
        ExportUiState(
            loaded = true,
            projectName = project.name,
            durationMs = timeline.totalDurationMs,
            thumbnail = project.thumbnailRef,
            settings = settings,
            resolutions = caps.resolutionOptions(device),
            frameRates = caps.frameRateOptions(device),
            videoBitrate = plan.videoBitrate,
            estimatedBytes = plan.estimatedBytes,
            blocker = coordinator.check(timeline, plan),
            job = job?.takeIf { it.projectId == project.id },
            interrupted = wasInterrupted,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ExportUiState())

    fun open(id: String) {
        viewModelScope.launch {
            if (session.project.value?.id != id) session.open(id)
            projectId.value = id
            chosen.value = repository.settings(id)
            val last = repository.job(id)?.state
            interrupted.value = coordinator.job.value == null && last == ExportState.Failed(ExportFailure.Interrupted)
        }
    }

    fun onEvent(event: ExportEvent) {
        when (event) {
            is ExportEvent.SelectResolution -> choose { it.copy(resolution = event.value) }
            is ExportEvent.SelectFrameRate -> choose { it.copy(frameRate = event.value) }
            is ExportEvent.SelectQuality -> choose { it.copy(quality = event.value) }
            ExportEvent.Start -> start()
            ExportEvent.Cancel -> coordinator.cancel()
            ExportEvent.DismissInterrupted -> interrupted.value = false
            ExportEvent.Open -> finishedUri()?.let { effectChannel.trySend(ExportEffect.OpenVideo(it)) }
            ExportEvent.Share -> finishedUri()?.let { effectChannel.trySend(ExportEffect.ShareVideo(it)) }
        }
    }

    private fun finishedUri(): String? = (state.value.job?.state as? ExportState.Finished)?.uri

    private fun choose(change: (ExportSettings) -> ExportSettings) {
        val current = state.value.settings ?: return
        val next = change(current)
        chosen.value = next
        val id = projectId.value ?: return
        viewModelScope.launch { repository.saveSettings(id, next) }
    }

    private fun start() {
        val ui = state.value
        val settings = ui.settings ?: return
        val project = session.project.value ?: return
        val timeline = session.timeline.value ?: return
        interrupted.update { false }
        if (coordinator.start(project.id, project.name, timeline, settings) == null) starter.start()
    }

    private companion object {
        const val DEFAULT_FPS = 30
    }
}
