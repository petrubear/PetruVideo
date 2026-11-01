package com.petruvideo.feature.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.designsystem.adaptive.Arrangement as WindowArrangement
import com.petruvideo.core.designsystem.adaptive.LocalArrangement
import com.petruvideo.core.designsystem.components.PillButton
import com.petruvideo.core.designsystem.components.SheetSurface
import com.petruvideo.core.model.ClipEffect
import com.petruvideo.core.model.EFFECT_MAX
import com.petruvideo.core.model.EFFECT_STRENGTH_DEFAULT
import com.petruvideo.core.model.EffectKind
import com.petruvideo.core.model.TRANSITION_MIN_MS
import com.petruvideo.core.model.TRANSITION_STEP_MS
import com.petruvideo.core.model.Transition
import com.petruvideo.core.model.TransitionKind
import com.petruvideo.feature.editor.EditorEvent
import com.petruvideo.feature.editor.EffectPanelState
import com.petruvideo.feature.editor.EffectToolKind
import kotlin.math.roundToInt

private val MAX_BODY = 260.dp
private val MAX_BODY_TABLETOP = 120.dp
private val TILE_WIDTH = 92.dp
private val TILE_HEIGHT = 56.dp
private const val PERCENT_MAX = 100f
private const val MILLIS = 1000f

/** The Transition or Effect panel above the clip tool bar. */
@Composable
fun EffectPanel(panel: EffectPanelState?, onEvent: (EditorEvent) -> Unit, modifier: Modifier = Modifier) {
    if (panel == null) return
    SheetSurface(modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    panel.kind.label,
                    style = PetruType.Title,
                    color = PetruColors.OnBackground,
                    modifier = Modifier.weight(1f),
                )
                PillButton("Done", onClick = { onEvent(EditorEvent.EffectDismiss) })
            }
            val tabletop = LocalArrangement.current is WindowArrangement.Tabletop
            Column(
                Modifier.heightIn(max = if (tabletop) MAX_BODY_TABLETOP else MAX_BODY)
                    .verticalScroll(rememberScrollState()),
            ) {
                when (panel.kind) {
                    EffectToolKind.Transition -> TransitionControls(panel, onEvent)
                    EffectToolKind.Effect -> EffectControls(panel.effects, onEvent)
                }
            }
        }
    }
}

@Composable
fun TransitionControls(panel: EffectPanelState, onEvent: (EditorEvent) -> Unit, modifier: Modifier = Modifier) {
    val current = panel.transition
    var length by remember(current, panel.limitMs) {
        mutableStateOf((current?.lengthMs ?: DEFAULT_LENGTH_MS).coerceAtMost(panel.limitMs))
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TransitionKind.entries.forEach { kind ->
                KindTile("Transition ${kind.label}", kind.label, current?.kind == kind) {
                    onEvent(EditorEvent.TransitionSet(kind, length))
                }
            }
        }
        if (current != null) {
            SettingSlider(
                "Length", "%.1f s".format(length / MILLIS), length.toFloat(),
                TRANSITION_MIN_MS.toFloat()..panel.limitMs.toFloat().coerceAtLeast(TRANSITION_MIN_MS + 1f),
                { length = (it / TRANSITION_STEP_MS).roundToInt() * TRANSITION_STEP_MS },
                { onEvent(EditorEvent.TransitionSet(current.kind, length)) },
            )
            TextAction("Remove transition") { onEvent(EditorEvent.TransitionRemove) }
        }
    }
}

@Composable
fun EffectControls(effects: List<ClipEffect>, onEvent: (EditorEvent) -> Unit, modifier: Modifier = Modifier) {
    var selected by remember { mutableStateOf<EffectKind?>(null) }
    val effect = effects.firstOrNull { it.kind == selected } ?: effects.lastOrNull()
    var shown by remember(effect) { mutableStateOf(effect ?: ClipEffect(EffectKind.Blur, EFFECT_STRENGTH_DEFAULT)) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EffectKind.entries.forEach { kind ->
                KindTile("Effect ${kind.label}", kind.label, effects.any { it.kind == kind }) {
                    selected = kind
                    if (effects.none { it.kind == kind }) {
                        onEvent(EditorEvent.EffectCommit(ClipEffect(kind, EFFECT_STRENGTH_DEFAULT)))
                    }
                }
            }
        }
        Text(
            "Up to $EFFECT_MAX effects per clip, drawn in the order they were added.",
            style = PetruType.Caption,
            color = PetruColors.OnSurfaceMuted,
        )
        if (effect != null) {
            SettingSlider(
                "${effect.kind.label} strength", "${shown.strength}%", shown.strength.toFloat(), 0f..PERCENT_MAX,
                {
                    shown = shown.copy(strength = it.roundToInt())
                    onEvent(EditorEvent.EffectPreview(shown))
                },
                { onEvent(EditorEvent.EffectCommit(shown)) },
            )
            TextAction("Remove ${effect.kind.label}") { onEvent(EditorEvent.EffectRemove(effect.kind)) }
        }
    }
}

@Composable
private fun KindTile(description: String, label: String, selected: Boolean, onClick: () -> Unit) {
    val edge = if (selected) PetruColors.Accent else PetruColors.SurfaceHighest
    Box(
        Modifier
            .size(width = TILE_WIDTH, height = TILE_HEIGHT)
            .clip(RoundedCornerShape(8.dp))
            .background(PetruColors.SurfaceHigh)
            .border(if (selected) 3.dp else 1.dp, edge, RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = description
                this.selected = selected
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = PetruType.Tiny, color = if (selected) PetruColors.Accent else PetruColors.OnBackground)
    }
}

private const val DEFAULT_LENGTH_MS = 1_000L
