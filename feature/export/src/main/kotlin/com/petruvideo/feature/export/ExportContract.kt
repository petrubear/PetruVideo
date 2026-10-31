package com.petruvideo.feature.export

import com.petruvideo.core.export.Blocker
import com.petruvideo.core.export.ExportJob
import com.petruvideo.core.model.export.ExportSettings
import com.petruvideo.core.model.export.FrameRate
import com.petruvideo.core.model.export.Option
import com.petruvideo.core.model.export.Quality
import com.petruvideo.core.model.export.Resolution

data class ExportUiState(
    val loaded: Boolean = false,
    val projectName: String = "",
    val durationMs: Long = 0,
    /** Cache path of the project thumbnail, if any. */
    val thumbnail: String? = null,
    val settings: ExportSettings? = null,
    val resolutions: List<Option<Resolution>> = emptyList(),
    val frameRates: List<Option<FrameRate>> = emptyList(),
    val qualities: List<Quality> = Quality.entries,
    val videoBitrate: Int = 0,
    val estimatedBytes: Long = 0,
    val blocker: Blocker? = null,
    val job: ExportJob? = null,
    /** The last export of this project was cut short by the app being closed. */
    val interrupted: Boolean = false,
) {
    val rendering: Boolean get() = job?.state?.active == true
}

sealed interface ExportEvent {
    data class SelectResolution(val value: Resolution) : ExportEvent
    data class SelectFrameRate(val value: FrameRate) : ExportEvent
    data class SelectQuality(val value: Quality) : ExportEvent
    data object Start : ExportEvent
    data object Cancel : ExportEvent
    data object DismissInterrupted : ExportEvent
    data object Open : ExportEvent
    data object Share : ExportEvent
}

sealed interface ExportEffect {
    data class OpenVideo(val uri: String) : ExportEffect
    data class ShareVideo(val uri: String) : ExportEffect
}
