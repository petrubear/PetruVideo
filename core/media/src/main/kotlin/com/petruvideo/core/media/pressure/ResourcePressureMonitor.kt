package com.petruvideo.core.media.pressure

import android.content.ComponentCallbacks2
import android.content.Context
import android.os.Build
import android.os.PowerManager
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

@Suppress("DEPRECATION") // the trim levels are deprecated but still delivered; UI_HIDDEN is ignored on purpose
enum class PressureLevel {
    Normal,

    /** Lower quality, keep playing. */
    Reduced,

    /** Playback cannot continue safely; pause with a message. */
    Critical,
    ;

    companion object {
        /** Combines the latest memory trim level and thermal status (either may be unknown) into one level. */
        fun from(trimLevel: Int?, thermalStatus: Int?): PressureLevel {
            val memory = when {
                trimLevel == null -> Normal
                trimLevel >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL &&
                    trimLevel < ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> Critical
                trimLevel >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> Critical
                trimLevel >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW &&
                    trimLevel < ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> Reduced
                else -> Normal
            }
            val thermal = when {
                thermalStatus == null -> Normal
                thermalStatus >= PowerManager.THERMAL_STATUS_SEVERE -> Critical
                thermalStatus >= PowerManager.THERMAL_STATUS_MODERATE -> Reduced
                else -> Normal
            }
            return maxOf(memory, thermal)
        }
    }
}

/** Preview and thumbnail quality steps applied under pressure (FR-031). */
data class PreviewQuality(
    val previewScale: Float,
    val previewFps: Int,
    val thumbnailPx: Int,
    /** Whether the reverb effect is applied in the preview; it is paused under pressure (it costs CPU). */
    val reverb: Boolean = true,
    /** Whether noise reduction, pitch, and echo are applied in the preview; paused under pressure. */
    val voiceFx: Boolean = true,
) {
    companion object {
        val Full = PreviewQuality(previewScale = 1f, previewFps = 30, thumbnailPx = 256)
        val Reduced = PreviewQuality(
            previewScale = 0.5f, previewFps = 24, thumbnailPx = 128, reverb = false, voiceFx = false,
        )
    }
}

/** Non-blocking messages for the UI. */
enum class PressureNotice { QualityReduced, PlaybackPaused, QualityRestored }

/**
 * Listens to memory trim callbacks and thermal status and tells the preview engine and thumbnail generators to
 * step down quality (research 15). Edits are never affected: they live in the session and autosave.
 */
class ResourcePressureMonitor {
    private var trimLevel: Int? = null
    private var thermalStatus: Int? = null
    private val _level = MutableStateFlow(PressureLevel.Normal)
    private val _quality = MutableStateFlow(PreviewQuality.Full)
    private val _notices = MutableSharedFlow<PressureNotice>(
        extraBufferCapacity = NOTICE_BUFFER,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val level: StateFlow<PressureLevel> = _level
    val notices: SharedFlow<PressureNotice> = _notices

    val quality: StateFlow<PreviewQuality> = _quality

    fun onTrimMemory(level: Int) {
        trimLevel = level
        update()
    }

    fun onThermalStatus(status: Int) {
        thermalStatus = status
        update()
    }

    /** Registers for thermal updates (Android 10 and above). */
    fun start(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            power.addThermalStatusListener(context.mainExecutor) { onThermalStatus(it) }
        }
    }

    private fun update() {
        val previous = _level.value
        val next = PressureLevel.from(trimLevel, thermalStatus)
        if (next == previous) return
        _level.value = next
        _quality.value = if (next == PressureLevel.Normal) PreviewQuality.Full else PreviewQuality.Reduced
        _notices.tryEmit(
            when {
                next == PressureLevel.Critical -> PressureNotice.PlaybackPaused
                next == PressureLevel.Reduced -> PressureNotice.QualityReduced
                else -> PressureNotice.QualityRestored
            },
        )
    }

    private companion object {
        const val NOTICE_BUFFER = 8
    }
}
