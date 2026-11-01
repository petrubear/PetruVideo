package com.petruvideo.feature.editor.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.designsystem.components.PetruChip
import com.petruvideo.core.designsystem.components.PillButton
import com.petruvideo.core.designsystem.components.SheetSurface
import com.petruvideo.core.media.captions.RecognitionAvailability
import com.petruvideo.feature.editor.CaptionsPanelState
import com.petruvideo.feature.editor.EditorEvent
import java.util.Locale

/** The Auto captions panel of the Text tab. */
@Composable
fun CaptionsPanel(panel: CaptionsPanelState?, onEvent: (EditorEvent) -> Unit, modifier: Modifier = Modifier) {
    if (panel == null) return
    SheetSurface(modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Auto captions",
                    style = PetruType.Title,
                    color = PetruColors.OnBackground,
                    modifier = Modifier.weight(1f),
                )
                PillButton("Close", onClick = { onEvent(EditorEvent.CaptionsDismiss) })
            }
            CaptionsBody(panel, onEvent)
        }
    }
}

private fun languageName(tag: String): String = Locale.forLanguageTag(tag).getDisplayName(Locale.getDefault())

@Composable
private fun CaptionsBody(panel: CaptionsPanelState, onEvent: (EditorEvent) -> Unit) {
    val progress = panel.progress
    when {
        progress != null -> Running(progress, onEvent)
        panel.availability == null -> Muted("Checking speech recognition on this phone...")
        panel.availability == RecognitionAvailability.Available -> Ready(panel, onEvent)
        else -> Unavailable(panel, onEvent)
    }
    panel.note?.let { Muted(it) }
}

@Composable
private fun Muted(text: String) {
    Text(text, style = PetruType.Caption, color = PetruColors.OnSurfaceMuted)
}

@Composable
private fun Running(percent: Int, onEvent: (EditorEvent) -> Unit) {
    val description = "Making captions, $percent percent"
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Making captions $percent%",
            style = PetruType.Body,
            color = PetruColors.OnBackground,
            modifier = Modifier.weight(1f).semantics {
                contentDescription = description
                liveRegion = LiveRegionMode.Polite
            },
        )
        PillButton("Cancel", onClick = { onEvent(EditorEvent.CaptionsCancel) })
    }
    LinearProgressIndicator(
        progress = { percent / PERCENT },
        modifier = Modifier.fillMaxWidth(),
        color = PetruColors.Accent,
        trackColor = PetruColors.SurfaceHigh,
    )
}

@Composable
private fun Ready(panel: CaptionsPanelState, onEvent: (EditorEvent) -> Unit) {
    Muted("Recognised on this phone only. Nothing is sent over the network.")
    if (panel.languages.size > 1) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            panel.languages.forEach { tag ->
                PetruChip(
                    languageName(tag), selected = tag == panel.language,
                    onClick = { onEvent(EditorEvent.CaptionsLanguage(tag)) },
                )
            }
        }
    } else {
        Muted("Language: ${languageName(panel.language)}")
    }
    FxSwitch("Replace earlier auto captions", panel.replace) { onEvent(EditorEvent.CaptionsReplace(it)) }
    PillButton("Start", onClick = { onEvent(EditorEvent.CaptionsStart) })
}

@Composable
private fun Unavailable(panel: CaptionsPanelState, onEvent: (EditorEvent) -> Unit) {
    val reason = when (val availability = panel.availability) {
        RecognitionAvailability.NeedsNewerAndroid -> "Auto captions need Android 13 or newer."
        RecognitionAvailability.NotOnThisPhone -> "This phone has no speech recognition that works without the network."
        is RecognitionAvailability.LanguageMissing ->
            "${languageName(availability.wanted)} is not installed for speech recognition on this phone."
        else -> ""
    }
    Text(reason, style = PetruType.Body, color = PetruColors.OnBackground)
    val installed = panel.languages
    if (installed.isNotEmpty()) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            installed.forEach { tag ->
                PetruChip(languageName(tag), selected = false, onClick = { onEvent(EditorEvent.CaptionsLanguage(tag)) })
            }
        }
    }
    if (panel.availability is RecognitionAvailability.LanguageMissing) {
        PillButton("Open speech settings", onClick = { onEvent(EditorEvent.CaptionsOpenSettings) })
    }
}

private const val PERCENT = 100f
