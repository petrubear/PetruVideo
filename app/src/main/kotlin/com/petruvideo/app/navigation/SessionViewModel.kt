package com.petruvideo.app.navigation

import androidx.lifecycle.ViewModel
import com.petruvideo.core.data.session.ProjectSession
import com.petruvideo.core.model.MediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Gives navigation destinations access to the process-scoped open project. */
@HiltViewModel
class SessionViewModel @Inject constructor(private val session: ProjectSession) : ViewModel() {
    fun addMedia(items: List<MediaItem>) {
        session.addMedia(items)
    }
}
