package com.petruvideo.core.model.export

import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.Timeline
import kotlin.math.abs
import kotlin.math.min

/**
 * The highest output the footage in use justifies: no upscaling and no invented frames (spec FR-005a). A lower frame
 * rate is offered only when it divides the footage's rate evenly (60 to 30), because frames are dropped in whole steps.
 */
data class ExportCaps(val maxResolution: Resolution, val maxFrameRate: FrameRate) {
    /** The options for the segmented controls, each with the reason it is unavailable. */
    fun resolutionOptions(device: DeviceLimits): List<Option<Resolution>> = Resolution.entries.map {
        Option(
            it,
            when {
                it.shortSide > maxResolution.shortSide -> Unavailable.AboveFootage
                it.shortSide > device.maxShortSide -> Unavailable.NotSupported
                else -> null
            },
        )
    }

    fun frameRateOptions(device: DeviceLimits): List<Option<FrameRate>> = FrameRate.entries.map {
        Option(
            it,
            when {
                it.fps > maxFrameRate.fps -> Unavailable.AboveFootage
                maxFrameRate.fps % it.fps != 0 -> Unavailable.NotForFootage
                it.fps > device.maxFps -> Unavailable.NotSupported
                else -> null
            },
        )
    }

    /** Defaults: up to 1080p within the caps, the frame rate nearest to the project's, Recommended quality. */
    fun defaults(projectFps: Int, device: DeviceLimits): ExportSettings {
        val resolution = resolutionOptions(device)
            .filter { it.allowed && it.value.shortSide <= DEFAULT_MAX_SHORT_SIDE }
            .maxByOrNull { it.value.shortSide }?.value ?: Resolution.R720
        val frameRate = frameRateOptions(device).filter { it.allowed }
            .minByOrNull { abs(it.value.fps - projectFps) }?.value ?: FrameRate.F24
        return ExportSettings(resolution, frameRate)
    }

    /** A saved choice lowered to the nearest allowed value when the footage or device no longer allows it. */
    fun clamp(settings: ExportSettings, device: DeviceLimits): ExportSettings {
        val resolution = resolutionOptions(device)
            .filter { it.allowed && it.value.shortSide <= settings.resolution.shortSide }
            .maxByOrNull { it.value.shortSide }?.value ?: Resolution.R720
        val frameRate = frameRateOptions(device).filter { it.allowed && it.value.fps <= settings.frameRate.fps }
            .maxByOrNull { it.value.fps }?.value ?: FrameRate.F24
        return settings.copy(resolution = resolution, frameRate = frameRate)
    }

    companion object {
        private const val DEFAULT_MAX_SHORT_SIDE = 1080
        private const val SIZE_TOLERANCE = 0.95
        private const val FPS_TOLERANCE = 0.5

        /** Caps from the video clips and photos on the timeline; a photo counts at the project's frame rate. */
        fun from(timeline: Timeline): ExportCaps {
            val projectFps = (timeline.format?.fps ?: FrameRate.F30.fps).toFloat()
            val sources = timeline.videoLane.mapNotNull { timeline.media[it.mediaItemId] }
            val shortSide = sources.maxOfOrNull { min(it.width ?: 0, it.height ?: 0) } ?: 0
            val fps = sources.maxOfOrNull { sourceFps(it.kind, it.frameRate, projectFps) } ?: 0f
            val resolution = Resolution.entries.filter { shortSide >= it.shortSide * SIZE_TOLERANCE }
                .maxByOrNull { it.shortSide } ?: Resolution.R720
            val frameRate = FrameRate.entries.filter { fps + FPS_TOLERANCE >= it.fps }
                .maxByOrNull { it.fps } ?: FrameRate.F24
            return ExportCaps(resolution, frameRate)
        }

        private fun sourceFps(kind: MediaKind, frameRate: Float?, projectFps: Float) =
            if (kind == MediaKind.Photo) projectFps else frameRate ?: projectFps
    }
}
