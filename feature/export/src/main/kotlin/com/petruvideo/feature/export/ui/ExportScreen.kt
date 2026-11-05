package com.petruvideo.feature.export.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.designsystem.adaptive.AdaptiveScaffold
import com.petruvideo.core.designsystem.adaptive.HingeBounds
import com.petruvideo.core.designsystem.adaptive.HingeSplit
import com.petruvideo.core.designsystem.adaptive.LocalArrangement
import com.petruvideo.core.designsystem.components.IconAction
import com.petruvideo.core.designsystem.icons.PetruIcons
import com.petruvideo.core.model.formatDuration
import com.petruvideo.feature.export.ExportEvent
import com.petruvideo.feature.export.ExportUiState
import androidx.compose.ui.res.stringResource
import com.petruvideo.feature.export.R

/**
 * The export screen, per the designer's Export artboard: preview, resolution, frame rate and quality, the file facts,
 * progress, and Save to gallery. One layout per arrangement; everything flows through [onEvent].
 */
@Composable
fun ExportScreen(
    state: ExportUiState,
    onEvent: (ExportEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(PetruColors.Background).statusBarsPadding()) {
        TopBar(onBack)
        AdaptiveScaffold(
            arrangement = LocalArrangement.current,
            modifier = Modifier.weight(1f),
            compact = {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                    PreviewCard(state)
                    Controls(state, onEvent)
                }
            },
            expanded = { hinge -> Expanded(state, onEvent, hinge) },
            tabletopTop = { Box(Modifier.fillMaxSize().padding(horizontal = 20.dp)) { PreviewCard(state) } },
            tabletopBottom = {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                    Controls(state, onEvent)
                }
            },
        )
    }
}

@Composable
private fun Expanded(state: ExportUiState, onEvent: (ExportEvent) -> Unit, hinge: HingeBounds?) {
    val preview: @Composable () -> Unit = {
        Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) { PreviewCard(state) }
    }
    val controls: @Composable () -> Unit = {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Controls(state, onEvent)
        }
    }
    if (hinge != null) {
        HingeSplit(hinge, Modifier.fillMaxSize(), first = preview, second = controls)
    } else {
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxHeight()) { preview() }
            Box(Modifier.weight(1f).fillMaxHeight()) { controls() }
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconAction(PetruIcons.Back, stringResource(R.string.export_back), onBack)
        Text(
            stringResource(R.string.export_export),
            style = PetruType.TitleSmall,
            color = PetruColors.OnBackground,
            modifier = Modifier.weight(1f).padding(end = 48.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun PreviewCard(state: ExportUiState) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(PREVIEW_RATIO)
            .clip(RoundedCornerShape(20.dp))
            .background(PetruColors.SurfaceHigh),
        contentAlignment = Alignment.Center,
    ) {
        state.thumbnail?.let {
            AsyncImage(model = it, contentDescription = null, modifier = Modifier.fillMaxSize())
        }
        Text(
            formatDuration(state.durationMs),
            style = PetruType.Mono,
            color = PetruColors.OnBackground,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .background(PetruColors.Background.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun Controls(state: ExportUiState, onEvent: (ExportEvent) -> Unit) {
    val settings = state.settings ?: return
    Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (state.interrupted) InterruptedNotice(onEvent)
        ExportSettingsPanel(state, settings, onEvent)
        ExportProgressCard(state, onEvent)
    }
}

@Composable
private fun InterruptedNotice(onEvent: (ExportEvent) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PetruColors.SurfaceHigh)
            .clickable { onEvent(ExportEvent.DismissInterrupted) }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            stringResource(R.string.export_the_last_export_did_not),
            style = PetruType.Body,
            color = PetruColors.OnBackground,
            modifier = Modifier.weight(1f),
        )
    }
}

private const val PREVIEW_RATIO = 16f / 9f
