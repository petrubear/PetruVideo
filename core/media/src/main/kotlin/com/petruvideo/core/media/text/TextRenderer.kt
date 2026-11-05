package com.petruvideo.core.media.text

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.petruvideo.core.model.TEXT_LINE_SPACING_DEFAULT
import com.petruvideo.core.model.TEXT_PERMILLE
import com.petruvideo.core.model.TextAlign
import com.petruvideo.core.model.TextClip
import com.petruvideo.core.model.TextFont
import com.petruvideo.core.model.TextShadow
import com.petruvideo.core.model.TextStyle
import kotlin.math.abs
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
    private const val PERCENT = 100f
    private const val MIN_SHADOW_RADIUS = 0.5f
    private const val RGB_MASK = 0x00FFFFFF
    private const val ALPHA_SHIFT = 24

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

    private fun layout(text: String, paint: TextPaint, width: Int, style: TextStyle) =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(alignment(style.align))
            .setIncludePad(false)
            .apply {
                if (style.lineSpacingPercent != TEXT_LINE_SPACING_DEFAULT) {
                    setLineSpacing(0f, style.lineSpacingPercent / PERCENT)
                }
            }
            .build()

    /** The shadow's paint settings for text of [textSize] pixels. */
    private fun Paint.shadowed(textSize: Float, shadow: TextShadow) {
        val (dx, dy) = TextGeometry.shadowOffset(textSize, shadow)
        val alpha = (255 * shadow.opacityPercent / PERCENT).roundToInt()
        val radius = max(MIN_SHADOW_RADIUS, TextGeometry.shadowBlur(textSize, shadow))
        setShadowLayer(radius, dx, dy, (shadow.colorArgb and RGB_MASK) or (alpha shl ALPHA_SHIFT))
    }

    /** The bitmap for [clip] on a frame of [frameWidth] by [frameHeight]; a clip without words gives 1 by 1. */
    fun render(clip: TextClip, frameWidth: Int, frameHeight: Int): Bitmap {
        if (clip.text.isEmpty() || frameWidth <= 0 || frameHeight <= 0) {
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }
        val textSize = max(1f, clip.sizePermille / TEXT_PERMILLE.toFloat() * frameHeight)
        val style = clip.style
        val pad = if (style.background.enabled) textSize * PAD_SHARE else 0f
        val stroke = if (style.outline.enabled) textSize * style.outline.widthPermille / TEXT_PERMILLE else 0f
        val shadow = style.shadow
        val reach = TextGeometry.shadowReach(textSize, shadow)
        val edge = ceil(pad + stroke + reach).toInt()
        val maxWidth = max(1, (frameWidth * (1 - 2 * MARGIN_SHARE)).toInt() - 2 * edge)
        val fill = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = textSize
            color = style.colorArgb
            typeface = typeface(style.font)
            if (style.letterSpacingPercent != 0) letterSpacing = style.letterSpacingPercent / PERCENT
        }
        val measured = layout(clip.text, fill, maxWidth, style)
        var widest = 0f
        for (line in 0 until measured.lineCount) widest = max(widest, measured.getLineWidth(line))
        // Lay out again at the widest line so centered or right-aligned text sits inside the cropped box.
        // With letter spacing the line breaker and the measure can differ by about one gap, which would split a word.
        val slack = ceil(textSize * abs(style.letterSpacingPercent) / PERCENT).toInt()
        val contentWidth = (ceil(widest).toInt() + 1 + slack).coerceIn(1, maxWidth)
        val shadowBehindLetters = shadow.enabled && !style.background.enabled && stroke <= 0f
        if (shadowBehindLetters) fill.shadowed(textSize, shadow)
        val fillLayout = layout(clip.text, fill, contentWidth, style)
        val width = max(1, contentWidth + 2 * edge)
        val maxHeight = max(1, (frameHeight * (1 - 2 * MARGIN_SHARE)).toInt())
        val height = (fillLayout.height + 2 * edge).coerceIn(1, maxHeight)
        val metrics = Metrics(textSize, edge, stroke, contentWidth, width, height)
        // A turned block gets a bitmap as large as its turned outline; the drawing is the same, on a turned canvas.
        val turned = style.rotationDegrees != 0
        val (bitmapWidth, bitmapHeight) =
            if (turned) TextGeometry.turnedSize(width, height, style.rotationDegrees) else width to height
        val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        if (turned) {
            canvas.translate(bitmapWidth / 2f, bitmapHeight / 2f)
            canvas.rotate(style.rotationDegrees.toFloat())
            canvas.translate(-width / 2f, -height / 2f)
        }
        drawBackground(canvas, style, metrics)
        drawLetters(canvas, clip, fill, fillLayout, metrics)
        return bitmap
    }

    /** The sizes of one text as drawn, in pixels. */
    private class Metrics(
        val textSize: Float,
        val edge: Int,
        val stroke: Float,
        val contentWidth: Int,
        val width: Int,
        val height: Int,
    )

    /** The rounded box behind the text, with the shadow when the shadow follows the box. */
    private fun drawBackground(canvas: Canvas, style: TextStyle, m: Metrics) {
        if (!style.background.enabled) return
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = style.background.colorArgb
            alpha = (255 * style.background.opacityPercent / 100f).roundToInt()
            if (style.shadow.enabled) shadowed(m.textSize, style.shadow)
        }
        val radius = m.textSize * RADIUS_SHARE
        canvas.drawRoundRect(0f, 0f, m.width.toFloat(), m.height.toFloat(), radius, radius, bg)
    }

    /** The outline (with the shadow when it has no box) and the letters. */
    private fun drawLetters(canvas: Canvas, clip: TextClip, fill: TextPaint, fillLayout: StaticLayout, m: Metrics) {
        val style = clip.style
        canvas.save()
        canvas.translate(m.edge.toFloat(), m.edge.toFloat())
        if (m.stroke > 0f) {
            val outline = TextPaint(fill).apply {
                this.style = Paint.Style.STROKE
                strokeWidth = m.stroke * 2
                strokeJoin = Paint.Join.ROUND
                color = style.outline.colorArgb
                if (style.shadow.enabled && !style.background.enabled) shadowed(m.textSize, style.shadow)
            }
            layout(clip.text, outline, m.contentWidth, style).draw(canvas)
        }
        fillLayout.draw(canvas)
        canvas.restore()
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
