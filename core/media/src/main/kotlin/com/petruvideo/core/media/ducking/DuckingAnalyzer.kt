package com.petruvideo.core.media.ducking

import com.petruvideo.core.media.waveform.WaveformPeaksGenerator
import com.petruvideo.core.model.DuckEnvelope
import com.petruvideo.core.model.DuckingPlanner
import com.petruvideo.core.model.Timeline

/**
 * Works out the ducking envelope of every ducked audio clip: reads the loudness (waveform peaks, cached) of the sounds
 * that can trigger it and hands them to the pure [DuckingPlanner]. Preview and export both use it, so they agree.
 */
class DuckingAnalyzer(private val peaks: WaveformPeaksGenerator) {
    /** The envelope of each ducked clip by clip id; clips that are not ducked are left out. */
    suspend fun envelopes(timeline: Timeline): Map<String, DuckEnvelope> {
        val ducked = timeline.audioLane.filter { it.ducking.enabled }
        if (ducked.isEmpty()) return emptyMap()
        val sources = timeline.videoLane.filter { it.attachedAudio }.map { it.mediaItemId } +
            timeline.audioLane.filter { !it.ducking.enabled }.map { it.mediaItemId }
        val loudness = sources.distinct().mapNotNull { id ->
            timeline.media[id]?.let { item -> peaks.peaks(item)?.let { id to it } }
        }.toMap()
        return ducked.mapNotNull { clip ->
            DuckingPlanner.envelopeFor(timeline, clip.id) { loudness[it] }?.let { clip.id to it }
        }.toMap()
    }
}
