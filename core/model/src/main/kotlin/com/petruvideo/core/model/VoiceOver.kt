package com.petruvideo.core.model

const val VOICE_OVER_MIN_MS = 500L
const val VOICE_OVER_MAX_MS = 30L * 60 * 1000
const val VOICE_OVER_BITRATE = 96_000
const val VOICE_OVER_MIN_FREE_BYTES = 20L * 1024 * 1024
// 96 000 bits a second is 12 000 bytes a second, so exactly 12 bytes for each millisecond.
private const val BYTES_PER_MS = VOICE_OVER_BITRATE / 8 / 1000

/** Why a recording ended; every reason keeps what was recorded. */
enum class VoiceOverStop { User, NextClip, TimeLimit, Storage, Interrupted }

/** Pure limits of a voice-over take. */
object VoiceOverLimit {
    /**
     * The longest take that can start at [startMs]: 30 minutes, until the next audio clip, and what [freeBytes] of
     * storage holds above the reserve. Below [VOICE_OVER_MIN_MS] there is no room to record.
     */
    fun maxDurationMs(startMs: Long, audioLane: List<AudioClip>, freeBytes: Long): Long {
        val nextStart = nextClipStart(startMs, audioLane)
        val untilClip = if (nextStart == null) VOICE_OVER_MAX_MS else nextStart - startMs
        val usable = (freeBytes - VOICE_OVER_MIN_FREE_BYTES).coerceAtLeast(0)
        val byStorage = durationOfBytes(usable)
        return minOf(VOICE_OVER_MAX_MS, untilClip, byStorage)
    }

    /** Which limit ends a take of [maxMs] when it runs full length. */
    fun stopReason(startMs: Long, audioLane: List<AudioClip>, freeBytes: Long): VoiceOverStop {
        val max = maxDurationMs(startMs, audioLane, freeBytes)
        val nextStart = nextClipStart(startMs, audioLane)
        return when {
            nextStart != null && max == nextStart - startMs -> VoiceOverStop.NextClip
            max == VOICE_OVER_MAX_MS -> VoiceOverStop.TimeLimit
            else -> VoiceOverStop.Storage
        }
    }

    /** Where the next audio clip begins at or after [startMs] (right away when [startMs] is inside one), or null. */
    private fun nextClipStart(startMs: Long, audioLane: List<AudioClip>): Long? =
        audioLane.filter { it.timelineEndMs > startMs }.minOfOrNull { maxOf(it.timelineStartMs, startMs) }

    /** The length of a recording file of [bytes] at the fixed bitrate. */
    fun durationOfBytes(bytes: Long): Long = bytes / BYTES_PER_MS
}
