package com.petruvideo.core.media.text

import com.petruvideo.core.model.AnimKind
import com.petruvideo.core.model.TextClip

/** What a text clip looks like at one moment: if it shows, its opacity, vertical shift (frame share), and scale. */
data class TextLook(val visible: Boolean, val alpha: Float, val offsetY: Float, val scale: Float) {
    companion object {
        val Hidden = TextLook(false, 0f, 0f, 1f)
    }
}

/** Pure animation math for text clips (research 2): no bitmap changes over time, only these four numbers. */
object TextAnimator {
    private const val SLIDE_SHARE = 0.08f
    private const val POP_FROM = 0.6f

    fun at(clip: TextClip, timeMs: Long): TextLook {
        if (timeMs < clip.startMs || timeMs >= clip.endMs) return TextLook.Hidden
        val (inMs, outMs) = clip.effectiveAnimMs()
        var look = TextLook(true, 1f, 0f, 1f)
        val sinceStart = timeMs - clip.startMs
        if (inMs > 0 && sinceStart < inMs) look = enter(clip.animIn.kind, ease(sinceStart.toFloat() / inMs))
        val untilEnd = clip.endMs - timeMs
        if (outMs > 0 && untilEnd < outMs) look = leave(clip.animOut.kind, ease(untilEnd.toFloat() / outMs), look)
        return look
    }

    private fun ease(t: Float): Float {
        val x = t.coerceIn(0f, 1f)
        return x * x * (3 - 2 * x)
    }

    private fun enter(kind: AnimKind, p: Float) = when (kind) {
        AnimKind.Fade -> TextLook(true, p, 0f, 1f)
        AnimKind.SlideUp -> TextLook(true, p, (1 - p) * SLIDE_SHARE, 1f)
        AnimKind.Pop -> TextLook(true, p, 0f, POP_FROM + (1 - POP_FROM) * p)
        else -> TextLook(true, 1f, 0f, 1f)
    }

    /** [q] runs from 1 (start of the out animation) to 0 (the clip's end). */
    private fun leave(kind: AnimKind, q: Float, current: TextLook) = when (kind) {
        AnimKind.Fade -> current.copy(alpha = current.alpha * q)
        AnimKind.SlideDown -> current.copy(alpha = current.alpha * q, offsetY = current.offsetY + (1 - q) * SLIDE_SHARE)
        AnimKind.Pop -> current.copy(alpha = current.alpha * q, scale = current.scale * (POP_FROM + (1 - POP_FROM) * q))
        else -> current
    }
}
