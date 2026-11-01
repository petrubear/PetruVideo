package com.petruvideo.core.media.text

import android.graphics.Bitmap
import android.util.Pair
import androidx.media3.common.util.Size
import androidx.annotation.OptIn
import androidx.media3.common.OverlaySettings
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import com.petruvideo.core.model.TextClip

/**
 * The overlay of one text clip. It reads [clip] each frame, so a live edit only replaces the value; the bitmap is
 * rendered again only when the words, style, size, or frame size change, and the same instance is returned
 * otherwise so Media3 uploads the texture once. Time is the composition's presentation time.
 */
@OptIn(UnstableApi::class)
class TextClipOverlay(initial: TextClip, private val fallbackWidth: Int, private val fallbackHeight: Int) :
    BitmapOverlay() {
    @Volatile var clip: TextClip = initial

    @Volatile private var frame = Size(fallbackWidth, fallbackHeight)
    private var cacheKey: Key? = null
    private var cached: Bitmap? = null

    private data class Key(val text: String, val style: com.petruvideo.core.model.TextStyle, val size: Int, val w: Int, val h: Int)

    override fun configure(videoSize: Size) {
        frame = videoSize
    }

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        val now = clip
        val size = frame
        val key = Key(now.text, now.style, now.sizePermille, size.width, size.height)
        val have = cached
        if (have != null && key == cacheKey) return have
        val bitmap = TextRenderer.render(now, size.width, size.height)
        cached = bitmap
        cacheKey = key
        return bitmap
    }

    override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings {
        val now = clip
        val size = frame
        val look = TextAnimator.at(now, presentationTimeUs / MICROS_PER_MS)
        val bitmap = getBitmap(presentationTimeUs)
        val box = TextRenderer.place(now, bitmap.width, bitmap.height, size.width, size.height)
        val ndcX = box.centerX / size.width * 2f - 1f
        val ndcY = 1f - (box.centerY / size.height + look.offsetY) * 2f
        val alpha = if (look.visible) look.alpha else 0f
        return object : OverlaySettings {
            override fun getAlphaScale() = alpha
            override fun getBackgroundFrameAnchor(): Pair<Float, Float> = Pair(ndcX, ndcY)
            override fun getOverlayFrameAnchor(): Pair<Float, Float> = Pair(0f, 0f)
            override fun getScale(): Pair<Float, Float> = Pair(look.scale, look.scale)
        }
    }

    private companion object {
        const val MICROS_PER_MS = 1_000L
    }
}

/** The text lane as one composition-level effect; [overlays] lets the preview change a clip live. */
@OptIn(UnstableApi::class)
class TextOverlaySet(clips: List<TextClip>, width: Int, height: Int) {
    val overlays: Map<String, TextClipOverlay> = clips.associate { it.id to TextClipOverlay(it, width, height) }

    val effect: OverlayEffect? = overlays.values.toList().takeIf { it.isNotEmpty() }?.let { OverlayEffect(it) }

    fun update(clip: TextClip) {
        overlays[clip.id]?.clip = clip
    }
}
