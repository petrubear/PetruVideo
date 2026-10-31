package com.petruvideo.feature.editor

import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import com.petruvideo.core.media.probe.MediaProber
import com.petruvideo.core.media.thumbs.ThumbnailStripGenerator
import com.petruvideo.core.media.waveform.WaveformPeaksGenerator
import com.petruvideo.core.model.MediaItem
import com.petruvideo.feature.editor.ui.MediaLoaders
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject

/** Gives the timeline its thumbnails and waveforms, and reads a replacement file chosen for Relink. */
@HiltViewModel
class EditorMediaViewModel @Inject constructor(
    private val thumbnails: ThumbnailStripGenerator,
    private val waveforms: WaveformPeaksGenerator,
    private val prober: MediaProber,
) : ViewModel() {
    val loaders = object : MediaLoaders {
        override suspend fun thumbnail(item: MediaItem, timeMs: Long, heightPx: Int): ImageBitmap? =
            thumbnails.frame(item, timeMs, heightPx)?.asImageBitmap()

        override suspend fun peaks(item: MediaItem): FloatArray? = waveforms.peaks(item)
    }

    suspend fun probe(uri: String): MediaItem? = prober.probe(Uri.parse(uri), UUID.randomUUID().toString()).getOrNull()
}
