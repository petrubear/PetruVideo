package com.petruvideo.feature.editor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.designsystem.components.PetruChip
import com.petruvideo.core.designsystem.components.PillButton
import com.petruvideo.core.designsystem.components.SheetSurface
import com.petruvideo.core.model.ARGB_WHITE
import com.petruvideo.core.model.BlurStrength
import com.petruvideo.core.model.CanvasBackground
import com.petruvideo.feature.editor.EditorEvent
import com.petruvideo.core.designsystem.enumLabel
import androidx.compose.ui.res.stringResource
import com.petruvideo.feature.editor.R

/** The Background panel of the Effects tab: what fills the frame behind clips that do not cover it. */
@Composable
fun CanvasPanel(canvas: CanvasBackground?, onEvent: (EditorEvent) -> Unit, modifier: Modifier = Modifier) {
    if (canvas == null) return
    SheetSurface(modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.editor_background),
                    style = PetruType.Title,
                    color = PetruColors.OnBackground,
                    modifier = Modifier.weight(1f),
                )
                PillButton(stringResource(R.string.editor_close), onClick = { onEvent(EditorEvent.CanvasDismiss) })
            }
            CanvasControls(canvas, onEvent)
        }
    }
}

/** Black, Colour, and Blur choices with the colour picker or the blur strengths under the active one. */
@Composable
fun CanvasControls(canvas: CanvasBackground, onEvent: (EditorEvent) -> Unit, modifier: Modifier = Modifier) {
    val set = { next: CanvasBackground -> onEvent(EditorEvent.CanvasSet(next)) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            stringResource(R.string.editor_fills_the_frame_behind_a),
            style = PetruType.Caption,
            color = PetruColors.OnSurfaceMuted,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PetruChip(
                stringResource(R.string.editor_black),
                selected = canvas == CanvasBackground.Black,
                onClick = { set(CanvasBackground.Black) },
            )
            PetruChip(
                stringResource(R.string.editor_colour), selected = canvas is CanvasBackground.Colour,
                onClick = { if (canvas !is CanvasBackground.Colour) set(CanvasBackground.Colour(ARGB_WHITE)) },
            )
            PetruChip(
                stringResource(R.string.editor_blur), selected = canvas is CanvasBackground.Blur,
                onClick = { if (canvas !is CanvasBackground.Blur) set(CanvasBackground.Blur(BlurStrength.Medium)) },
            )
        }
        when (canvas) {
            is CanvasBackground.Colour -> ColorPicker(
                label = stringResource(R.string.editor_background), colorArgb = canvas.argb,
                onPreview = {}, onCommit = { set(CanvasBackground.Colour(it)) },
            )
            is CanvasBackground.Blur -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BlurStrength.entries.forEach {
                    PetruChip(
                        enumLabel("blur", it),
                        selected = it == canvas.strength,
                        onClick = { set(CanvasBackground.Blur(it)) },
                    )
                }
            }
            CanvasBackground.Black -> Unit
        }
    }
}
