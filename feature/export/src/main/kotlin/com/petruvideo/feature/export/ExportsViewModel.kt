package com.petruvideo.feature.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petruvideo.core.data.repo.ExportRepository
import com.petruvideo.core.model.export.ExportRecord
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

/** State of the Exports list: the history from the repository plus a background check of each video file. */
@HiltViewModel
class ExportsViewModel @Inject constructor(
    private val repository: ExportRepository,
    private val checker: ExportFileChecker,
) : ViewModel() {
    private val missing = MutableStateFlow<Set<String>>(emptySet())
    private val checked = HashMap<String, Boolean>()
    private val undo = MutableStateFlow<ExportRecord?>(null)
    private val records = MutableStateFlow<List<ExportRecord>?>(null)
    private val effectChannel = Channel<ExportsEffect>(Channel.BUFFERED)

    val effects = effectChannel.receiveAsFlow()

    val state: StateFlow<ExportsUiState> = combine(records, missing, undo) { list, gone, removed ->
        ExportsUiState(
            rows = list.orEmpty().map { ExportRow(it, it.id in gone) },
            loaded = list != null,
            undo = removed,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ExportsUiState())

    init {
        viewModelScope.launch {
            repository.records().collect { list ->
                records.value = list
                checkFiles(list)
            }
        }
    }

    /** Checks each video once per screen visit; a deleted file is marked, not removed. */
    private suspend fun checkFiles(list: List<ExportRecord>) {
        for (record in list) {
            if (checked.containsKey(record.id)) continue
            val found = checker.exists(record.uri)
            checked[record.id] = found
            if (!found) missing.update { it + record.id }
        }
    }

    /** Forgets earlier file checks, so reopening the screen looks again. */
    fun refresh() {
        checked.clear()
        missing.value = emptySet()
        records.value?.let { list -> viewModelScope.launch { checkFiles(list) } }
    }

    fun onEvent(event: ExportsEvent) {
        when (event) {
            is ExportsEvent.Open -> withRecord(event.id, openable = true) {
                effectChannel.trySend(ExportsEffect.OpenVideo(it.uri))
            }
            is ExportsEvent.Share -> withRecord(event.id, openable = true) {
                effectChannel.trySend(ExportsEffect.ShareVideo(it.uri))
            }
            is ExportsEvent.Remove -> withRecord(event.id, openable = false) { record ->
                undo.value = record
                viewModelScope.launch { repository.removeRecord(record.id) }
            }
            ExportsEvent.Undo -> undo.value?.let { record ->
                undo.value = null
                viewModelScope.launch { repository.addRecord(record) }
            }
            ExportsEvent.DismissUndo -> undo.value = null
        }
    }

    /** Runs [block] for the entry; with [openable] a row whose video is missing is skipped. */
    private fun withRecord(id: String, openable: Boolean, block: (ExportRecord) -> Unit) {
        val row = state.value.rows.firstOrNull { it.record.id == id } ?: return
        if (!openable || !row.missing) block(row.record)
    }
}
