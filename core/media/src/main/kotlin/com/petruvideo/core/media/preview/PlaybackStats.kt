package com.petruvideo.core.media.preview

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.video.VideoFrameMetadataListener

private const val TAG = "PetruPlayback"
private const val WINDOW_MS = 2_000L
private const val MS_PER_SECOND = 1_000.0
private const val NS_PER_MS = 1_000_000.0

/**
 * Debug-only playback measurement (research 1, spike 1): counts frames released to the screen and frames the player
 * dropped, and logs the rendered frame rate and the longest gap between frames every two seconds while playing.
 * Read it with `adb logcat -s PetruPlayback`.
 */
@OptIn(UnstableApi::class)
internal class PlaybackStats : VideoFrameMetadataListener, AnalyticsListener {
    private var windowStartNs = 0L
    private var lastFrameNs = 0L
    private var frames = 0
    private var dropped = 0
    private var maxGapMs = 0.0
    private var playRequestedAtMs = 0L

    /** Call when the user presses Play; the next rendered frame logs how long it took to appear. */
    fun playRequested() {
        playRequestedAtMs = android.os.SystemClock.elapsedRealtime()
    }

    override fun onVideoFrameAboutToBeRendered(
        presentationTimeUs: Long,
        releaseTimeNs: Long,
        format: Format,
        mediaFormat: android.media.MediaFormat?,
    ) {
        if (playRequestedAtMs != 0L) {
            val waited = android.os.SystemClock.elapsedRealtime() - playRequestedAtMs
            Log.i(TAG, "play to first frame: $waited ms")
            playRequestedAtMs = 0L
        }
        if (windowStartNs == 0L) windowStartNs = releaseTimeNs
        if (lastFrameNs != 0L) maxGapMs = maxOf(maxGapMs, (releaseTimeNs - lastFrameNs) / NS_PER_MS)
        lastFrameNs = releaseTimeNs
        frames++
        val elapsedMs = (releaseTimeNs - windowStartNs) / NS_PER_MS
        if (elapsedMs >= WINDOW_MS) {
            Log.i(
                TAG,
                "fps=%.1f frames=%d dropped=%d maxGapMs=%.1f".format(
                    frames * MS_PER_SECOND / elapsedMs,
                    frames,
                    dropped,
                    maxGapMs,
                ),
            )
            windowStartNs = releaseTimeNs
            frames = 0
            dropped = 0
            maxGapMs = 0.0
        }
    }

    override fun onDroppedVideoFrames(eventTime: AnalyticsListener.EventTime, droppedFrames: Int, elapsedMs: Long) {
        dropped += droppedFrames
    }
}
