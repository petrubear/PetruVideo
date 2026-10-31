package com.petruvideo.feature.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petruvideo.core.data.session.ProjectSession
import com.petruvideo.core.model.Project
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Minimal for now: opens the project in the process-scoped session. Playback and editing arrive with US3. */
@HiltViewModel
class EditorViewModel @Inject constructor(private val session: ProjectSession) : ViewModel() {
    val project: StateFlow<Project?> = session.project

    fun open(projectId: String) {
        viewModelScope.launch { session.open(projectId) }
    }

    fun flush() {
        viewModelScope.launch { session.flush() }
    }
}
