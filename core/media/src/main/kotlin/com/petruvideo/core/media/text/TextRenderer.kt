package com.petruvideo.core.media.text

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.petruvideo.core.model.TEXT_PERMILLE
import com.petruvideo.core.model.TextAlign
import com.petruvideo.core.model.TextClip
import com.petruvideo.core.model.TextFont
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

/** Where a rendered text box sits on a frame: its pixel size and the center clamped inside the safe margin. */
data class TextBox(val width: Int, val height: Int, val centerX: Float, val centerY: Float)

/**
 * Draws one text clip to a bitmap (words, wrapping, font, color, optional rounded background and outline) at the
 * output frame size; preview and export use the same code (research 1). Sizes are shares of the frame, so the same
 * clip looks the same at any resolution.
 */
object TextRenderer {
    const val MARGIN_SHARE = 0.05f
    private const val PAD_SHARE = 0.35f
    private const val RADIUS_SHARE = 0.3f

    internal fun typeface(font: TextFont): Typeface = when (font) {
        TextFont.Sans -> Typeface.create("sans-serif", Typeface.NORMAL)
        TextFont.Serif -> Typeface.create("serif", Typeface.NORMAL)
        TextFont.Mono -> Typeface.create("monospace", Typeface.NORMAL)
        TextFont.Display -> Typeface.create("sans-serif-black", Typeface.NORMAL)
        TextFont.Handwritten -> Typeface.create("cursive", Typeface.NORMAL)
        TextFont.Bold -> Typeface.create("sans-serif", Typeface.BOLD)
    }

    private fun alignment(align: TextAlign) = when (align) {
        TextAlign.Left -> Layout.Alignment.ALIGN_NORMAL
        TextAlign.Center -> Layout.Alignment.ALIGN_CENTER
        TextAlign.Right -> Layout.Alignment.ALIGN_OPPOSITE
    }

    private fun layout(text: String, paint: TextPaint, width: Int, align: TextAlign) =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(alignment(align))
            .setIncludePad(false)
            .build()

    /** The bitmap for [clip] on a frame of [frameWidth] by [frameHeight]; a clip without words gives 1 by 1. */
    fun render(clip: TextClip, frameWidth: Int, frameHeight: Int): Bitmap {
        if (clip.text.isEmpty() || frameWidth <= 0 || frameHeight <= 0) {
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }
        val textSize = max(1f, clip.sizePermille / TEXT_PERMILLE.toFloat() * frameHeight)
        val style = clip.style
        val pad = if (style.background.enabled) textSize * PAD_SHARE else 0f
        val stroke = if (style.outline.enabled) textSize * style.outline.widthPermille / TEXT_PERMILLE else 0f
        val edge = ceil(pad + stroke).toInt()
        val maxWidth = max(1, (frameWidth * (1 - 2 * MARGIN_SHARE)).toInt() - 2 * edge)
        val fill = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = textSize
            color = style.colorArgb
            typeface = typeface(style.font)
        }
        val measured = layout(clip.text, fill, maxWidth, style.align)
        var widest = 0f
        for (line in 0 until measured.lineCount) widest = max(widest, measured.getLineWidth(line))
        // Lay out again at the widest line so centered or right-aligned text sits inside the cropped box.
        val contentWidth = (ceil(widest).toInt() + 1).coerceIn(1, maxWidth)
        val fillLayout = layout(clip.text, fill, contentWidth, style.align)
        val width = max(1, contentWidth + 2 * edge)
        val maxHeight = max(1, (frameHeight * (1 - 2 * MARGIN_SHARE)).toInt())
        val height = (fillLayout.height + 2 * edge).coerceIn(1, maxHeight)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        if (style.background.enabled) {
            val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = style.background.colorArgb
                alpha = (255 * style.background.opacityPercent / 100f).roundToInt()
            }
            val radius = textSize * RADIUS_SHARE
            canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), radius, radius, bg)
        }
        canvas.save()
        canvas.translate(edge.toFloat(), edge.toFloat())
        if (stroke > 0f) {
            val outline = TextPaint(fill).apply {
                this.style = Paint.Style.STROKE
                strokeWidth = stroke * 2
                strokeJoin = Paint.Join.ROUND
                color = style.outline.colorArgb
            }
            layout(clip.text, outline, contentWidth, style.align).draw(canvas)
        }
        fillLayout.draw(canvas)
        canvas.restore()
        return bitmap
    }

    /** The box of a bitmap of [bitmapWidth] by [bitmapHeight] at the clip's place, kept inside the safe margin. */
    fun place(clip: TextClip, bitmapWidth: Int, bitmapHeight: Int, frameWidth: Int, frameHeight: Int): TextBox {
        val marginX = frameWidth * MARGIN_SHARE
        val marginY = frameHeight * MARGIN_SHARE
        val x = clip.xPermille / TEXT_PERMILLE.toFloat() * frameWidth
        val y = clip.yPermille / TEXT_PERMILLE.toFloat() * frameHeight
        return TextBox(
            bitmapWidth,
            bitmapHeight,
            clampCenter(x, bitmapWidth / 2f, marginX, frameWidth.toFloat()),
            clampCenter(y, bitmapHeight / 2f, marginY, frameHeight.toFloat()),
        )
    }

    private fun clampCenter(center: Float, half: Float, margin: Float, frame: Float): Float {
        val low = margin + half
        val high = frame - margin - half
        return if (low >= high) frame / 2 else center.coerceIn(low, high)
    }
}
