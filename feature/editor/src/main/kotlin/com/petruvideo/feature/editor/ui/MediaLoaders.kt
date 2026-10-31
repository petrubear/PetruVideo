package com.petruvideo.feature.editor.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import com.petruvideo.core.model.MediaItem

/** Source of timeline pictures, provided at the editor root; faked in screenshot tests. */
interface MediaLoaders {
    suspend fun thumbnail(item: MediaItem, timeMs: Long, heightPx: Int): ImageBitmap?

    suspend fun peaks(item: MediaItem): FloatArray?
}

object NoMediaLoaders : MediaLoaders {
    override suspend fun thumbnail(item: MediaItem, timeMs: Long, heightPx: Int): ImageBitmap? = null

    override suspend fun peaks(item: MediaItem): FloatArray? = null
}

val LocalMediaLoaders = staticCompositionLocalOf<MediaLoaders> { NoMediaLoaders }
