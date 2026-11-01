package com.petruvideo.core.data.repo

import android.content.SharedPreferences
import com.petruvideo.core.model.AppSettings
import com.petruvideo.core.model.export.Quality
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** The app's own settings; changes show up in [settings] at once. */
interface SettingsRepository {
    val settings: StateFlow<AppSettings>

    fun setDefaultQuality(quality: Quality)

    fun setLowerPreview(on: Boolean)
}

/** Keeps the settings in shared preferences; writes are applied in the background. */
class PrefsSettingsRepository(private val prefs: SharedPreferences) : SettingsRepository {
    private val state = MutableStateFlow(read())

    override val settings: StateFlow<AppSettings> = state

    private fun read() = AppSettings(
        defaultQuality = Quality.entries.firstOrNull { it.name == prefs.getString(KEY_QUALITY, null) }
            ?: Quality.Recommended,
        lowerPreview = prefs.getBoolean(KEY_LOWER_PREVIEW, false),
    )

    override fun setDefaultQuality(quality: Quality) {
        prefs.edit().putString(KEY_QUALITY, quality.name).apply()
        state.value = state.value.copy(defaultQuality = quality)
    }

    override fun setLowerPreview(on: Boolean) {
        prefs.edit().putBoolean(KEY_LOWER_PREVIEW, on).apply()
        state.value = state.value.copy(lowerPreview = on)
    }

    private companion object {
        const val KEY_QUALITY = "default_quality"
        const val KEY_LOWER_PREVIEW = "lower_preview"
    }
}
