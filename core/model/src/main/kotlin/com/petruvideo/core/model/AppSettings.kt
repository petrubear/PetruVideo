package com.petruvideo.core.model

import com.petruvideo.core.model.export.Quality

/** The few choices that belong to the app rather than to a project; kept on the device. */
data class AppSettings(
    val defaultQuality: Quality = Quality.Recommended,
    val lowerPreview: Boolean = false,
)
