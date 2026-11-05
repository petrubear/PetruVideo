package com.petruvideo.feature.projects.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.designsystem.components.IconAction
import com.petruvideo.core.designsystem.components.SegmentedControl
import com.petruvideo.core.designsystem.icons.PetruIcons
import com.petruvideo.core.model.AppSettings
import com.petruvideo.core.model.ThemeChoice
import com.petruvideo.core.model.export.Quality
import androidx.compose.ui.res.stringResource
import com.petruvideo.feature.projects.R
import com.petruvideo.core.designsystem.enumLabel

private val MAX_WIDTH = 560.dp

/** The Settings screen: default export quality, lower preview quality, and About. */
@Composable
fun SettingsScreen(
    settings: AppSettings,
    version: String,
    onQuality: (Quality) -> Unit,
    onLowerPreview: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onTheme: (ThemeChoice) -> Unit = {},
) {
    Column(modifier.fillMaxSize().background(PetruColors.Background).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconAction(PetruIcons.Back, stringResource(R.string.projects_back), onBack)
            Text(stringResource(R.string.projects_settings), style = PetruType.Title, color = PetruColors.OnBackground)
        }
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = MAX_WIDTH).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                ThemeSection(settings.theme, onTheme)
                QualitySection(settings.defaultQuality, onQuality)
                PreviewSection(settings.lowerPreview, onLowerPreview)
                AboutSection(version)
            }
        }
    }
}

@Composable
private fun ThemeSection(selected: ThemeChoice, onTheme: (ThemeChoice) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.projects_theme), style = PetruType.Label, color = PetruColors.OnBackground)
        SegmentedControl(
            options = ThemeChoice.entries.map { enumLabel("themechoice", it) },
            selectedIndex = ThemeChoice.entries.indexOf(selected),
            onSelect = { onTheme(ThemeChoice.entries[it]) },
        )
        Text(
            stringResource(R.string.projects_theme_hint),
            style = PetruType.Caption,
            color = PetruColors.OnSurfaceMuted,
        )
    }
}

@Composable
private fun QualitySection(selected: Quality, onQuality: (Quality) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.projects_default_export_quality),
            style = PetruType.Label,
            color = PetruColors.OnBackground,
        )
        SegmentedControl(
            options = Quality.entries.map { enumLabel("quality", it) },
            selectedIndex = Quality.entries.indexOf(selected),
            onSelect = { onQuality(Quality.entries[it]) },
        )
        Text(
            stringResource(R.string.projects_used_for_projects_you_have),
            style = PetruType.Caption,
            color = PetruColors.OnSurfaceMuted,
        )
    }
}

@Composable
private fun PreviewSection(on: Boolean, onChange: (Boolean) -> Unit) {
    val lowerQualityDescription = stringResource(R.string.projects_lower_preview_quality)
    val onText = stringResource(R.string.projects_on)
    val offText = stringResource(R.string.projects_off)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.projects_lower_preview_quality),
                style = PetruType.Label,
                color = PetruColors.OnBackground,
            )
            Switch(
                checked = on,
                onCheckedChange = onChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PetruColors.OnAccent,
                    checkedTrackColor = PetruColors.Accent,
                    uncheckedThumbColor = PetruColors.OnSurfaceMuted,
                    uncheckedTrackColor = PetruColors.SurfaceHighest,
                ),
                modifier = Modifier.semantics {
                    contentDescription = lowerQualityDescription
                    stateDescription = if (on) onText else offText
                },
            )
        }
        Text(
            stringResource(R.string.projects_plays_the_editor_preview_smaller),
            style = PetruType.Caption,
            color = PetruColors.OnSurfaceMuted,
        )
    }
}

@Composable
private fun AboutSection(version: String) {
    val versionDescription = stringResource(R.string.projects_version_description, version)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.projects_about), style = PetruType.Label, color = PetruColors.OnBackground)
        Text(
            stringResource(R.string.projects_version_named, version),
            style = PetruType.Body,
            color = PetruColors.OnSurfaceMuted,
            modifier = Modifier.semantics { contentDescription = versionDescription },
        )
    }
}
