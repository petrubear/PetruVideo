package com.petruvideo.core.media.thumbs

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.memory.MemoryCache
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import coil3.video.VideoFrameDecoder

/**
 * Grid and tray thumbnails for videos and photos (research 7). Coil loads off the main thread, cancels a request
 * when its cell leaves the composition, and decodes at a small fixed size; the memory cache is bounded.
 */
object GridThumbnailLoader {
    private const val THUMB_PX = 256
    private const val MEMORY_CACHE_PERCENT = 0.12

    @Volatile
    private var loader: ImageLoader? = null

    fun imageLoader(context: Context): ImageLoader = loader ?: synchronized(this) {
        loader ?: ImageLoader.Builder(context.applicationContext)
            .components { add(VideoFrameDecoder.Factory()) }
            .memoryCache {
                MemoryCache.Builder().maxSizePercent(context.applicationContext, MEMORY_CACHE_PERCENT).build()
            }
            .build()
            .also { loader = it }
    }

    fun request(context: Context, uri: String): ImageRequest =
        ImageRequest.Builder(context).data(uri).size(Size(THUMB_PX, THUMB_PX)).crossfade(false).build()
}

/** A thumbnail of the media at [uri]; shows nothing while loading or when the file has no picture. */
@Composable
fun GridThumbnail(uri: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val loader = remember(context) { GridThumbnailLoader.imageLoader(context) }
    val request = remember(uri) { GridThumbnailLoader.request(context, uri) }
    Box(modifier) {
        AsyncImage(
            model = request,
            imageLoader = loader,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
