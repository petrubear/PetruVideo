package com.petruvideo.core.media.preview

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.annotation.OptIn
import androidx.media3.common.util.ExperimentalApi
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.CompositionPlayer
import com.petruvideo.core.media.composition.BuildSpec
import com.petruvideo.core.media.composition.CompositionBuilder
import com.petruvideo.core.media.composition.usesMultipleInputs
import com.petruvideo.core.media.pressure.PreviewQuality
import com.petruvideo.core.model.AudioFx
import com.petruvideo.core.model.ClipFilter
import com.petruvideo.core.model.OverlayClip
import com.petruvideo.core.model.Reverb
import com.petruvideo.core.model.TextClip
import com.petruvideo.core.model.Timeline
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import com.petruvideo.core.media.ducking.DuckingAnalyzer
import com.petruvideo.core.media.reverse.ReversePreparer
import com.petruvideo.core.media.reverse.ReverseService
import com.petruvideo.core.media.waveform.WaveformPeaksGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Preview through Media3's [CompositionPlayer] (research 1): the video lane is one sequence (photos as timed
 * images, attached sound kept or removed per clip), the audio lane is a second sequence whose gaps are silent.
 * Missing clips and muted audio clips become gaps, so the timeline keeps its length and playback never fails.
 * All calls must come from the main thread, which is also the player's application thread.
 */
@OptIn(UnstableApi::class, ExperimentalApi::class)
@Singleton
class Media3PreviewEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reverse: ReversePreparer,
) : PreviewEngine {
    constructor(context: Context) : this(context, ReverseService.forContext(context))

    private val handler = Handler(Looper.getMainLooper())
    private val _state = MutableStateFlow(PreviewState())
    private var compositionPlayer: CompositionPlayer? = null
    private var totalMs = 0L
    private var hasComposition = false
    private var polling = false
    private var gains = mutableMapOf<String, GainAudioProcessor>()
    private var reverbs = mutableMapOf<String, ReverbAudioProcessor>()
    private var voiceFxs = mutableMapOf<String, VoiceFxProcessor>()
    private var voiceFxAllowed = true
    private var chromas: Map<String, com.petruvideo.core.media.effects.ChromaKeyHandle> = emptyMap()
    private var chromaChecker = false
    private var masks: Map<String, com.petruvideo.core.media.effects.OverlayMaskHandle> = emptyMap()
    private var overlayLayout: com.petruvideo.core.media.composition.OverlayLayout? = null
    private var filters = mutableMapOf<String, com.petruvideo.core.media.filter.ClipFilterEffects>()
    private var clipEffects = mutableMapOf<String, List<com.petruvideo.core.media.effects.ClipEffectHandle>>()
    private val analyzer = DuckingAnalyzer(WaveformPeaksGenerator(context))
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var duckJob: Job? = null
    private var reverseJob: Job? = null
    private val armedFraming = mutableSetOf<String>()
    private var lastTimeline: Timeline? = null
    private var lastQuality: PreviewQuality? = null
    private var framings = mutableMapOf<String, com.petruvideo.core.media.effects.ClipFramingHandle>()
    private var textOverlays: com.petruvideo.core.media.text.TextOverlaySet? = null
    private val refresh = Runnable { refreshFrame() }
    private var reverbAllowed = true

    override val state: StateFlow<PreviewState> = _state
    override val player: Player? get() = compositionPlayer

    private val poll = object : Runnable {
        override fun run() {
            publish()
            if (polling) handler.postDelayed(this, POLL_MS)
        }
    }

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            polling = isPlaying
            if (isPlaying) handler.post(poll) else publish()
        }

        override fun onPlaybackStateChanged(playbackState: Int) = publish()

        override fun onPlayerError(error: PlaybackException) {
            android.util.Log.e("PreviewEngine", "Playback error ${error.errorCodeName}", error)
            publish(error = "Playback stopped: ${error.errorCodeName}")
        }
    }

    private var playbackStats: PlaybackStats? = null

    private fun isDebuggable() =
        context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0

    private var multipleInputs = false
    private var endedByPosition = false

    private fun playerBuilder(multi: Boolean): CompositionPlayer.Builder {
        val builder = CompositionPlayer.Builder(context)
        if (multi) builder.setVideoGraphFactory(androidx.media3.effect.MultipleInputVideoGraph.Factory())
        return builder
    }

    private fun discardPlayer() {
        polling = false
        handler.removeCallbacks(poll)
        compositionPlayer?.release()
        compositionPlayer = null
        hasComposition = false
    }

    override fun load(timeline: Timeline, quality: PreviewQuality) {
        totalMs = timeline.totalDurationMs
        lastTimeline = timeline
        lastQuality = quality
        val sources = reverse.sources(timeline)
        val builder = CompositionBuilder(BuildSpec.forPreview(timeline, quality), armedFraming, reversed = sources)
        val composition = builder.build(timeline)
        gains = builder.gains
        reverbs = builder.reverbs
        voiceFxs = builder.voiceFxs
        textOverlays = builder.textOverlays
        overlayLayout = builder.overlayLayout
        adoptChromas(builder.chromas)
        masks = builder.masks.toMap()
        filters = builder.filters
        clipEffects = builder.clipEffects
        framings = builder.framings
        applyDucking(timeline)
        prepareReversed(timeline, sources.keys)
        voiceFxAllowed = quality.voiceFx
        reverbAllowed = quality.reverb
        // Overlays and transitions are separate video inputs, which needs the multiple-input video graph. That player
        // does not take a second composition, so it is made again for every load (keeping position and play state);
        // it is also made again when the need itself changes.
        if (composition == null) {
            // Nothing to show: drop the player so the last picture of an earlier project does not stay on screen.
            discardPlayer()
            publish()
            return
        }
        val previous = compositionPlayer
        val resumeBefore = previous?.isPlaying == true
        val positionBefore = previous?.currentPosition ?: 0L
        val multi = usesMultipleInputs(timeline)
        if (previous != null && (multi || multi != multipleInputs)) discardPlayer()
        multipleInputs = multi
        val player = compositionPlayer ?: playerBuilder(multi).build()
            .also {
                it.addListener(listener)
                if (isDebuggable()) PlaybackStats().let { stats ->
                    playbackStats = stats
                    it.setVideoFrameMetadataListener(stats)
                    it.addAnalyticsListener(stats)
                }
                compositionPlayer = it
            }
        hasComposition = true
        val reused = previous != null && player === previous
        val resume = if (reused) player.isPlaying else resumeBefore
        val position = (if (reused) player.currentPosition else positionBefore).coerceIn(0, totalMs)
        player.setComposition(composition, position)
        player.prepare()
        if (resume) player.play()
        publish()
    }

    // With nothing to play (an empty project) the player has no composition and rejects play and seek.
    override fun play() {
        // A player that failed ignores play until it is prepared again, so it is built again from the timeline.
        val failed = compositionPlayer?.playerError != null
        val timeline = lastTimeline
        val quality = lastQuality
        if (failed && timeline != null && quality != null) {
            _state.value = _state.value.copy(error = null)
            load(timeline, quality)
        }
        if (hasComposition) {
            endedByPosition = false
            playbackStats?.playRequested()
            compositionPlayer?.play()
        }
    }

    override fun pause() {
        compositionPlayer?.pause()
    }

    override fun seekTo(positionMs: Long) {
        endedByPosition = false
        if (hasComposition) compositionPlayer?.seekTo(positionMs.coerceIn(0, totalMs))
        publish()
    }

    override fun setClipReverbLive(clipId: String, reverb: Reverb) {
        reverbs[clipId]?.update(reverb.copy(enabled = reverb.enabled && reverbAllowed))
    }

    /** The loaded clip's gain and fade processor, for tests. */
    internal fun gainOf(clipId: String): GainAudioProcessor? = gains[clipId]

    override fun setClipFilterLive(clipId: String, filter: ClipFilter) {
        filters[clipId]?.update(filter)
        if (!polling) {
            handler.removeCallbacks(refresh)
            handler.postDelayed(refresh, REFRESH_MS)
        }
    }

    override fun setClipEffectLive(clipId: String, effect: com.petruvideo.core.model.ClipEffect) {
        clipEffects[clipId]?.firstOrNull { it.kind == effect.kind }?.update(effect)
        if (!polling) {
            handler.removeCallbacks(refresh)
            handler.postDelayed(refresh, REFRESH_MS)
        }
    }

    /**
     * A reversed clip without a prepared copy plays forwards until its copy is made in the background; when it is done
     * the latest content is loaded again. One preparation runs at a time and a failure is not retried.
     */
    private fun prepareReversed(timeline: Timeline, ready: Set<String>) {
        if (timeline.videoLane.none { it.reversed && it.id !in ready } || reverseJob?.isActive == true) return
        reverseJob = scope.launch {
            val ok = reverse.ensure(timeline)
            if (ok) handler.post { lastTimeline?.let { load(it, lastQuality ?: return@post) } }
        }
    }

    /** Computes the ducking envelopes in the background and hands each to its clip's processor when ready. */
    private fun applyDucking(timeline: Timeline) {
        duckJob?.cancel()
        val processors = gains
        duckJob = scope.launch {
            val envelopes = analyzer.envelopes(timeline)
            processors.forEach { (id, processor) -> processor.setEnvelope(envelopes[id]) }
        }
    }

    override fun armClipFraming(clipId: String) {
        val loaded = lastTimeline ?: return
        if (armedFraming.add(clipId)) load(loaded, lastQuality ?: return)
    }

    override fun setClipFramingLive(clip: com.petruvideo.core.model.VideoClip) {
        framings[clip.id]?.update(clip)
        if (!polling) {
            handler.removeCallbacks(refresh)
            handler.postDelayed(refresh, REFRESH_MS)
        }
    }

    override fun setChromaChecker(on: Boolean) {
        chromaChecker = on
        setChecker(on)
        refreshSoon()
    }

    override fun setOverlayLive(clip: OverlayClip) {
        overlayLayout?.update(clip)
        chromas[clip.id]?.update(clip.chroma)
        masks[clip.id]?.update(clip.mask)
        if (!polling) {
            handler.removeCallbacks(refresh)
            handler.postDelayed(refresh, REFRESH_MS)
        }
    }

    private fun adoptChromas(found: Map<String, com.petruvideo.core.media.effects.ChromaKeyHandle>) {
        chromas = found.toMap()
        setChecker(chromaChecker)
    }

    private fun setChecker(on: Boolean) = chromas.values.forEach { it.checker = on }

    private fun refreshSoon() {
        if (polling) return
        handler.removeCallbacks(refresh)
        handler.postDelayed(refresh, REFRESH_MS)
    }

    override fun setTextLive(clip: TextClip) {
        textOverlays?.update(clip)
        if (!polling) {
            handler.removeCallbacks(refresh)
            handler.postDelayed(refresh, REFRESH_MS)
        }
    }

    /** A paused player draws nothing new, so a seek to the same place makes it draw the frame again. */
    private fun refreshFrame() {
        val player = compositionPlayer ?: return
        if (hasComposition && !player.isPlaying) player.seekTo(player.currentPosition)
    }

    override fun setClipFxLive(clipId: String, fx: AudioFx, clipLengthMs: Long) {
        voiceFxs[clipId]?.update(fx, voiceFxAllowed)
        gains[clipId]?.setFade(fx.fade, clipLengthMs)
    }

    override fun setClipVolumeLive(clipId: String, percent: Int) {
        gains[clipId]?.setPercent(percent)
    }

    override fun release() {
        duckJob?.cancel()
        polling = false
        handler.removeCallbacks(poll)
        compositionPlayer?.release()
        compositionPlayer = null
        hasComposition = false
        _state.value = PreviewState()
    }

    private fun publish(error: String? = null) {
        val player = compositionPlayer
        // With several video inputs the player can reach the end of the timeline without reporting it, so the end is
        // taken from the position: the player is paused there and the preview reports the end.
        val atEnd = multipleInputs && totalMs > 0 && player != null && player.isPlaying &&
            player.currentPosition >= totalMs
        if (atEnd) player?.pause()
        val reachedEnd = player?.playbackState == Player.STATE_ENDED || atEnd || (multipleInputs && endedByPosition)
        endedByPosition = reachedEnd && multipleInputs
        _state.value = PreviewState(
            isPlaying = player?.isPlaying == true,
            positionMs = player?.currentPosition?.coerceIn(0, totalMs) ?: 0,
            durationMs = totalMs,
            ended = reachedEnd,
            error = error ?: _state.value.error.takeIf { player?.playerError != null },
        )
    }

    private companion object {
        const val POLL_MS = 33L
        const val REFRESH_MS = 40L
    }
}
