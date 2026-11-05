package com.petruvideo.feature.projects.settings

import androidx.lifecycle.ViewModel
import com.petruvideo.core.data.repo.SettingsRepository
import com.petruvideo.core.model.AppSettings
import com.petruvideo.core.model.ThemeChoice
import com.petruvideo.core.model.export.Quality
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class SettingsViewModel @Inject constructor(private val repository: SettingsRepository) : ViewModel() {
    val settings: StateFlow<AppSettings> = repository.settings

    fun setDefaultQuality(quality: Quality) = repository.setDefaultQuality(quality)

    fun setLowerPreview(on: Boolean) = repository.setLowerPreview(on)

    fun setTheme(theme: ThemeChoice) = repository.setTheme(theme)
}
