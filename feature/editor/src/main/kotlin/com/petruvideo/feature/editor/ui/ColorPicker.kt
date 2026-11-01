package com.petruvideo.feature.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.components.PetruChip

/** Twelve swatches: white, black, grays, and the main hues. */
internal val COLOR_SWATCHES = listOf(
    0xFFFFFFFF, 0xFF000000, 0xFF9AA0A6, 0xFFE53935, 0xFFFB8C00, 0xFFFDD835,
    0xFF43A047, 0xFF00ACC1, 0xFF1E88E5, 0xFF5E35B1, 0xFFD81B60, 0xFF6D4C41,
).map { it.toInt() }

private val SWATCH_NAMES = listOf(
    "White", "Black", "Gray", "Red", "Orange", "Yellow", "Green", "Cyan", "Blue", "Purple", "Pink", "Brown",
)

private const val HUE_MAX = 360f

/** Swatches plus a Custom chip that reveals hue and brightness sliders; [onPreview] streams, [onCommit] finishes. */
@Composable
fun ColorPicker(
    label: String,
    colorArgb: Int,
    onPreview: (Int) -> Unit,
    onCommit: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var custom by remember { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
            COLOR_SWATCHES.forEachIndexed { index, swatch ->
                Swatch("$label ${SWATCH_NAMES[index]}", swatch, swatch == colorArgb) { onCommit(swatch) }
            }
            PetruChip("Custom", selected = custom, onClick = { custom = !custom })
        }
        if (custom) CustomColor(label, colorArgb, onPreview, onCommit)
    }
}

@Composable
private fun Swatch(description: String, color: Int, selected: Boolean, onClick: () -> Unit) {
    val edge = if (selected) PetruColors.Accent else PetruColors.SurfaceHighest
    Box(
        Modifier
            .size(48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = description
                this.selected = selected
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(32.dp)
                .background(Color(color), CircleShape)
                .border(if (selected) 3.dp else 1.dp, edge, CircleShape),
        )
    }
}

@Composable
private fun CustomColor(label: String, colorArgb: Int, onPreview: (Int) -> Unit, onCommit: (Int) -> Unit) {
    val hsv = remember(colorArgb) { FloatArray(3).also { android.graphics.Color.colorToHSV(colorArgb, it) } }
    var hue by remember(colorArgb) { mutableStateOf(hsv[0]) }
    var value by remember(colorArgb) { mutableStateOf(hsv[2]) }
    val current = { android.graphics.Color.HSVToColor(floatArrayOf(hue, if (hsv[1] < 0.05f) 0.9f else hsv[1], value)) }
    SettingSlider("$label hue", "${hue.toInt()}", hue, 0f..HUE_MAX, {
        hue = it
        onPreview(current())
    }, { onCommit(current()) })
    SettingSlider("$label brightness", "${(value * 100).toInt()}%", value, 0f..1f, {
        value = it
        onPreview(current())
    }, { onCommit(current()) })
}

internal fun Color.argb(): Int = toArgb()
