package com.petruvideo.feature.projects.templates

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petruvideo.core.data.repo.ProjectRepository
import com.petruvideo.core.media.probe.MediaProber
import com.petruvideo.core.model.template.Template
import com.petruvideo.core.model.template.TemplateCatalog
import com.petruvideo.core.model.template.TemplateFiller
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TemplatesState(
    val templates: List<Template> = TemplateCatalog.all,
    /** True while the chosen files are read and the project is made. */
    val busy: Boolean = false,
    val message: String? = null,
)

sealed interface TemplatesEffect {
    /** Open the file picker for up to [limit] videos and photos. */
    data class PickMedia(val limit: Int) : TemplatesEffect

    data class OpenEditor(val projectId: String) : TemplatesEffect
}

/** Lists the built-in templates and turns the files the user picks into a normal project. */
@HiltViewModel
class TemplatesViewModel @Inject constructor(
    private val repository: ProjectRepository,
    private val prober: MediaProber,
) : ViewModel() {
    private val ui = MutableStateFlow(TemplatesState())
    private val effectChannel = Channel<TemplatesEffect>(Channel.BUFFERED)
    private var chosen: Template? = null

    val state: StateFlow<TemplatesState> = ui.asStateFlow()
    val effects = effectChannel.receiveAsFlow()

    fun use(id: String) {
        val template = TemplateCatalog.all.firstOrNull { it.id == id } ?: return
        chosen = template
        effectChannel.trySend(TemplatesEffect.PickMedia(template.slotCount))
    }

    /** The picker returned [uris]; an empty list (cancelled) changes nothing. */
    fun picked(uris: List<String>) {
        val template = chosen ?: return
        if (uris.isEmpty()) return
        ui.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            val items = uris.mapNotNull { prober.probe(Uri.parse(it), UUID.randomUUID().toString()).getOrNull() }
            val fill = TemplateFiller.fill(template, items, projectId = "", newId = { UUID.randomUUID().toString() })
            if (fill == null) {
                ui.update { it.copy(busy = false, message = "None of those files can be used") }
                return@launch
            }
            val project = repository.createProject()
            repository.saveTimeline(project.id, fill.timeline.copy(projectId = project.id))
            repository.rename(project.id, template.name)
            ui.update { it.copy(busy = false, message = unusedMessage(fill.unusedItems, template.slotCount)) }
            effectChannel.send(TemplatesEffect.OpenEditor(project.id))
        }
    }

    fun dismissMessage() = ui.update { it.copy(message = null) }

    private fun unusedMessage(unused: Int, slots: Int): String? =
        if (unused > 0) "Only the first $slots files were used" else null
}
