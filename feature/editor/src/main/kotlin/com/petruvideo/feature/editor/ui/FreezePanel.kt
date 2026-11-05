package com.petruvideo.feature.editor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.designsystem.components.PillButton
import com.petruvideo.core.designsystem.components.SheetSurface
import com.petruvideo.core.model.FREEZE_MAX_MS
import com.petruvideo.core.model.FREEZE_MIN_MS
import com.petruvideo.feature.editor.EditorEvent
import com.petruvideo.feature.editor.FreezePanelState
import kotlin.math.roundToLong
import androidx.compose.ui.res.stringResource
import com.petruvideo.feature.editor.R

/** The Freeze frame panel above the clip tool bar. */
@Composable
fun FreezePanel(panel: FreezePanelState?, onEvent: (EditorEvent) -> Unit, modifier: Modifier = Modifier) {
    if (panel == null) return
    SheetSurface(modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.editor_freeze_frame),
                    style = PetruType.Title,
                    color = PetruColors.OnBackground,
                    modifier = Modifier.weight(1f),
                )
                PillButton(stringResource(R.string.editor_close), onClick = { onEvent(EditorEvent.FreezeDismiss) })
            }
            FreezeControls(panel, onEvent)
        }
    }
}

/** The length slider and Insert; also hosted by the inspector on large screens. */
@Composable
fun FreezeControls(panel: FreezePanelState, onEvent: (EditorEvent) -> Unit, modifier: Modifier = Modifier) {
    var length by remember(panel) { mutableLongStateOf(panel.lengthMs) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SettingSlider(
            stringResource(R.string.editor_hold_for),
            formatSeconds(length),
            length.toFloat(),
            FREEZE_MIN_MS.toFloat()..FREEZE_MAX_MS.toFloat(),
            { length = (it / STEP).roundToLong() * STEP },
            {},
        )
        Text(
            stringResource(R.string.editor_the_picture_at_the_playhead),
            style = PetruType.Caption,
            color = PetruColors.OnSurfaceMuted,
        )
        PillButton(stringResource(R.string.editor_insert), onClick = { onEvent(EditorEvent.FreezeInsert(length)) })
    }
}

private const val STEP = 100L
