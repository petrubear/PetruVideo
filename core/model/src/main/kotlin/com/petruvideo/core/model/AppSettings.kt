package com.petruvideo.core.model

import com.petruvideo.core.model.export.Quality

/** Which look the app uses: the phone's own light or dark setting, or one of the two always. */
enum class ThemeChoice {
    System,
    Light,
    Dark,
    ;

    /** True when the dark look is in use, given whether the phone is in dark mode. */
    fun isDark(systemDark: Boolean): Boolean = when (this) {
        System -> systemDark
        Light -> false
        Dark -> true
    }
}

/** The few choices that belong to the app rather than to a project; kept on the device. */
data class AppSettings(
    val defaultQuality: Quality = Quality.Recommended,
    val lowerPreview: Boolean = false,
    val theme: ThemeChoice = ThemeChoice.System,
)
