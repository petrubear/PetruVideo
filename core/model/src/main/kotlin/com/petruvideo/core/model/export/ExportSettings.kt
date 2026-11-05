package com.petruvideo.core.model.export

/** Output resolution by the short side of the frame, so portrait and landscape projects share the names. */
enum class Resolution(val shortSide: Int, val label: String) {
    R720(720, "720p"),
    R1080(1080, "1080p"),
    R2K(1440, "2K"),
    R4K(2160, "4K"),
}

enum class FrameRate(val fps: Int) {
    F24(24),
    F30(30),
    F50(50),
    F60(60),
}

enum class Quality(val bitrateFactor: Double) {
    Smaller(0.6),
    Recommended(1.0),
    Higher(1.5),
}

data class ExportSettings(
    val resolution: Resolution,
    val frameRate: FrameRate,
    val quality: Quality = Quality.Recommended,
)

/** What the device's encoder can do: the largest short side and the highest frame rate for H.264. */
data class DeviceLimits(val maxShortSide: Int, val maxFps: Int)

/** Why an option cannot be chosen. */
enum class Unavailable {
    AboveFootage,
    NotSupported,
    NotForFootage,
}

data class Option<T>(val value: T, val unavailable: Unavailable? = null) {
    val allowed: Boolean get() = unavailable == null
}

/** Why a render stopped. */
sealed interface ExportFailure {
    data class MissingMedia(val name: String) : ExportFailure
    data class NotEnoughStorage(val neededBytes: Long) : ExportFailure
    data class EncoderError(val detail: String) : ExportFailure
    data object Interrupted : ExportFailure
    data object Pressure : ExportFailure
    data object SaveFailed : ExportFailure
}

sealed interface ExportState {
    data object Waiting : ExportState
    data class Rendering(val progress: Int) : ExportState
    data class Finished(val uri: String, val bytes: Long, val durationMs: Long) : ExportState
    data object Cancelled : ExportState
    data class Failed(val reason: ExportFailure) : ExportState

    val active: Boolean get() = this is Waiting || this is Rendering
}
