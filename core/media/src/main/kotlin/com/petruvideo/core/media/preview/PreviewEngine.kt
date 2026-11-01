package com.petruvideo.core.media.preview

import androidx.media3.common.Player
import com.petruvideo.core.media.pressure.PreviewQuality
import com.petruvideo.core.model.AudioFx
import com.petruvideo.core.model.Availability
import com.petruvideo.core.model.ClipFilter
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.Reverb
import com.petruvideo.core.model.OverlayClip
import com.petruvideo.core.model.TextClip
import com.petruvideo.core.model.Timeline
import kotlinx.coroutines.flow.StateFlow

data class PreviewState(
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    /** Playback reached the end of the timeline. */
    val ended: Boolean = false,
    /** Set when playback stopped because it cannot continue (e.g. under heavy pressure); cleared on next play. */
    val error: String? = null,
)

/**
 * Plays a [Timeline] (video lane with attached sound, audio lane, gaps silent or black) for the editor. The
 * editor depends only on this interface so the playback technology can change (research 1).
 */
interface PreviewEngine {
    val state: StateFlow<PreviewState>

    /** The player to draw into a surface, or null before the first [load]. */
    val player: Player?

    /** Replaces the content, keeping the current position where it still exists. Missing media is skipped. */
    fun load(timeline: Timeline, quality: PreviewQuality)

    fun play()

    fun pause()

    fun seekTo(positionMs: Long)

    /** Changes the reverb of one clip right away, without a rebuild or an edit; room size waits for the next load. */
    fun setClipReverbLive(clipId: String, reverb: Reverb)

    /** Changes the fade, noise reduction, equalizer, and voice effects of one clip right away, without a rebuild. */
    fun setClipFxLive(clipId: String, fx: AudioFx, clipLengthMs: Long)

    /** Changes the place or opacity of one overlay clip right away; the current frame is redrawn. */
    fun setOverlayLive(clip: OverlayClip)

    /** Changes one text clip right away (words, style, place, timing); the current frame is redrawn. */
    fun setTextLive(clip: TextClip)

    /** Changes the look and adjustments of one clip right away; the current frame is redrawn. */
    fun setClipFilterLive(clipId: String, filter: ClipFilter)

    /** Changes the volume of one clip in the loaded composition right away, without a rebuild or an edit. */
    fun setClipVolumeLive(clipId: String, percent: Int)

    fun release()
}

/** Reports which media is missing; implemented by `MissingMediaChecker`. */
fun interface MediaAvailabilityChecker {
    suspend fun check(items: Collection<MediaItem>): Map<String, Availability>
}

/** Refreshes the project thumbnail when the first clip changes; implemented by `ProjectThumbnailUpdater`. */
fun interface ThumbnailRefresher {
    suspend fun refresh(projectId: String, timeline: Timeline)
}
