package com.petruvideo.core.export

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import com.petruvideo.core.model.export.DeviceLimits
import com.petruvideo.core.model.export.FrameRate
import com.petruvideo.core.model.export.Resolution

/** What the device's H.264 encoders can do, behind an interface so the rest can be tested with a fake. */
interface EncoderCapabilities {
    val limits: DeviceLimits

    /** The bitrates (bit/s) the best encoder accepts. */
    val bitrateRange: IntRange
}

class MediaCodecEncoderCapabilities : EncoderCapabilities {
    private val encoders: List<MediaCodecInfo> by lazy {
        MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.filter {
            it.isEncoder && it.supportedTypes.any { type -> type.equals(AVC, ignoreCase = true) }
        }
    }

    private fun videoCaps(info: MediaCodecInfo) = info.getCapabilitiesForType(AVC).videoCapabilities

    override val limits: DeviceLimits by lazy {
        val caps = encoders.mapNotNull(::videoCaps)
        val shortSide = Resolution.entries.filter { r ->
            caps.any { it.areSizeAndRateSupported(r.shortSide * WIDE / TALL, r.shortSide, REFERENCE_FPS) }
        }.maxOfOrNull { it.shortSide } ?: Resolution.R720.shortSide
        val fps = FrameRate.entries.filter { f ->
            caps.any { it.areSizeAndRateSupported(HD_WIDTH, HD_HEIGHT, f.fps.toDouble()) }
        }.maxOfOrNull { it.fps } ?: FrameRate.F24.fps
        DeviceLimits(shortSide, fps)
    }

    override val bitrateRange: IntRange by lazy {
        val ranges = encoders.mapNotNull { videoCaps(it)?.bitrateRange }
        if (ranges.isEmpty()) DEFAULT_RANGE else ranges.minOf { it.lower }..ranges.maxOf { it.upper }
    }

    private companion object {
        const val AVC = MediaFormat.MIMETYPE_VIDEO_AVC
        const val WIDE = 16
        const val TALL = 9
        const val REFERENCE_FPS = 30.0
        const val HD_WIDTH = 1280
        const val HD_HEIGHT = 720
        val DEFAULT_RANGE = 64_000..100_000_000
    }
}
