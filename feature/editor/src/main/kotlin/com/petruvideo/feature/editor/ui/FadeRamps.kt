package com.petruvideo.feature.editor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.model.Fade

/** Widths of the fade-in and fade-out ramps as fractions of a clip of [clipLengthMs], after limiting to half each. */
internal fun fadeRampFractions(fade: Fade, clipLengthMs: Long): Pair<Float, Float> {
    if (clipLengthMs <= 0) return 0f to 0f
    val limited = fade.limitedTo(clipLengthMs)
    return limited.inMs.toFloat() / clipLengthMs to limited.outMs.toFloat() / clipLengthMs
}

/** Diagonal ramps over the part of a clip that fades: the area above the line is dimmed, as in audio editors. */
@Composable
fun FadeRamps(fade: Fade, clipLengthMs: Long, modifier: Modifier = Modifier) {
    val (inFraction, outFraction) = fadeRampFractions(fade, clipLengthMs)
    if (inFraction == 0f && outFraction == 0f) return
    Canvas(modifier.fillMaxSize()) {
        val dim = Color.Black.copy(alpha = 0.45f)
        val line = Stroke(width = 1.5f)
        if (inFraction > 0f) {
            val w = size.width * inFraction
            drawPath(Path().apply { moveTo(0f, 0f); lineTo(w, 0f); lineTo(0f, size.height); close() }, dim)
            drawLine(PetruColors.Accent, Offset(0f, size.height), Offset(w, 0f), strokeWidth = line.width)
        }
        if (outFraction > 0f) {
            val w = size.width * outFraction
            val x = size.width
            drawPath(Path().apply { moveTo(x, 0f); lineTo(x - w, 0f); lineTo(x, size.height); close() }, dim)
            drawLine(PetruColors.Accent, Offset(x - w, 0f), Offset(x, size.height), strokeWidth = line.width)
        }
    }
}
