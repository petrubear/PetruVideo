package com.petruvideo.feature.importmedia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petruvideo.core.data.importing.ImportProgress
import com.petruvideo.core.data.importing.MediaImporter
import com.petruvideo.core.media.library.LibraryAlbum
import com.petruvideo.core.media.library.LibraryItem
import com.petruvideo.core.media.library.MediaLibrary
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.PHOTO_DEFAULT_MS
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AddMediaViewModel @Inject constructor(
    private val library: MediaLibrary,
    private val importer: MediaImporter,
) : ViewModel() {
    private val _state = MutableStateFlow(AddMediaState())
    private val effectChannel = Channel<AddMediaEffect>(Channel.BUFFERED)
    private var loaded: List<LibraryItem> = emptyList()
    private var loadJob: Job? = null
    private var importJob: Job? = null

    val state: StateFlow<AddMediaState> = _state
    val effects = effectChannel.receiveAsFlow()

    fun onEvent(event: AddMediaEvent) {
        when (event) {
            is AddMediaEvent.SwitchTab -> { _state.update { it.copy(tab = event.tab) }; reload() }
            is AddMediaEvent.SwitchAlbum -> { _state.update { it.copy(selectedAlbum = event.album) }; reload() }
            is AddMediaEvent.ToggleItem -> toggle(event.uri)
            AddMediaEvent.AddToTimelineInOrder -> addToTimeline()
            AddMediaEvent.LoadMore -> loadMore()
            AddMediaEvent.Close -> effectChannel.trySend(AddMediaEffect.Close)
            AddMediaEvent.RequestAccess -> effectChannel.trySend(AddMediaEffect.RequestAccess(_state.value.tab.kind))
            AddMediaEvent.OpenSystemPicker ->
                effectChannel.trySend(AddMediaEffect.LaunchSystemPicker(_state.value.tab.kind))
            AddMediaEvent.CancelImport -> cancelImport()
            is AddMediaEvent.AccessChanged -> { _state.update { it.copy(permission = event.access) }; reload() }
            is AddMediaEvent.PickedFromSystem -> pickedFromSystem(event.uris)
            AddMediaEvent.DismissErrors -> _state.update { it.copy(errors = emptyList()) }
        }
    }

    private fun canBrowse() = _state.value.permission.let { it == MediaAccess.Granted || it == MediaAccess.Partial }

    private fun reload() {
        loadJob?.cancel()
        loaded = emptyList()
        publishItems()
        if (!canBrowse()) return
        val tab = _state.value.tab
        val album = _state.value.selectedAlbum
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            loaded = library.items(tab.kind, album, PAGE_SIZE, 0)
            _state.update { it.copy(isLoading = false) }
            publishItems()
        }
    }

    private fun loadMore() {
        val hasMorePages = loaded.isNotEmpty() && loaded.size % PAGE_SIZE == 0
        if (!canBrowse() || !hasMorePages || loadJob?.isActive == true) return
        val tab = _state.value.tab
        val album = _state.value.selectedAlbum
        loadJob = viewModelScope.launch {
            loaded = loaded + library.items(tab.kind, album, PAGE_SIZE, loaded.size)
            publishItems()
        }
    }

    private fun toggle(uri: String) {
        val selection = _state.value.selection
        if (selection.any { it.uri == uri }) {
            setSelection(selection.filterNot { it.uri == uri })
        } else {
            val item = loaded.firstOrNull { it.uri == uri } ?: return
            setSelection(selection + SelectedItem(item.uri, item.kind, item.displayName, item.durationMs))
        }
    }

    private fun pickedFromSystem(uris: List<String>) {
        val kind = _state.value.tab.kind
        val existing = _state.value.selection.map { it.uri }.toSet()
        val added = uris.filterNot { it in existing }.map { SelectedItem(it, kind, it.substringAfterLast('/'), null) }
        setSelection(_state.value.selection + added)
    }

    private fun setSelection(selection: List<SelectedItem>) {
        _state.update { it.copy(selection = selection, totalDurationMs = selection.sumOf(::durationOf)) }
        publishItems()
    }

    private fun durationOf(item: SelectedItem): Long =
        if (item.kind == MediaKind.Photo) PHOTO_DEFAULT_MS else item.durationMs ?: 0L

    private fun publishItems() {
        val order = _state.value.selection.mapIndexed { index, item -> item.uri to index + 1 }.toMap()
        _state.update { state ->
            state.copy(
                items = loaded.map {
                    GridItem(it.uri, it.kind, it.displayName, it.durationMs, order[it.uri])
                },
            )
        }
    }

    private fun addToTimeline() {
        val selection = _state.value.selection
        if (selection.isEmpty() || importJob?.isActive == true) return
        _state.update { it.copy(importProgress = ImportProgress(0, selection.size), errors = emptyList()) }
        importJob = viewModelScope.launch {
            try {
                val result = importer.import(selection.map { it.uri }) { progress ->
                    _state.update { it.copy(importProgress = progress) }
                }
                setSelection(emptyList())
                _state.update { it.copy(importProgress = null, errors = result.errors) }
                if (result.items.isNotEmpty()) {
                    effectChannel.send(AddMediaEffect.Done(result.items, closeScreen = result.errors.isEmpty()))
                }
            } catch (e: CancellationException) {
                _state.update { it.copy(importProgress = null) }
                throw e
            }
        }
    }

    private fun cancelImport() {
        importJob?.cancel()
        _state.update { it.copy(importProgress = null) }
    }

    private companion object {
        const val PAGE_SIZE = 60
    }
}
