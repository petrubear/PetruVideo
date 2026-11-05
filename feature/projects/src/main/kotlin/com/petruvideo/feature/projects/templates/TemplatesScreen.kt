package com.petruvideo.feature.projects.templates

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.designsystem.components.IconAction
import com.petruvideo.core.designsystem.components.PillButton
import com.petruvideo.core.designsystem.icons.PetruIcons
import com.petruvideo.core.model.FilterLook
import com.petruvideo.core.model.template.Template
import kotlinx.coroutines.delay
import androidx.compose.ui.res.stringResource
import com.petruvideo.feature.projects.R
import com.petruvideo.core.designsystem.keyedText

private val CARD_MIN_WIDTH = 360.dp
private const val MESSAGE_MS = 3_000L
private const val MS_PER_SECOND = 1000f

/** The list of built-in templates: one column on a phone, two from 840 dp. */
@Composable
fun TemplatesScreen(
    state: TemplatesState,
    onUse: (templateId: String) -> Unit,
    onDismissMessage: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().background(PetruColors.Background).statusBarsPadding()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconAction(PetruIcons.Back, stringResource(R.string.projects_back), onBack)
                Text(
                    stringResource(R.string.projects_templates),
                    style = PetruType.Title,
                    color = PetruColors.OnBackground,
                )
            }
            LazyVerticalGrid(
                GridCells.Adaptive(CARD_MIN_WIDTH),
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.templates, key = { it.id }) { TemplateCard(it, !state.busy, onUse) }
            }
        }
        state.message?.let {
            LaunchedEffect(it) { delay(MESSAGE_MS); onDismissMessage() }
            Text(
                it.asString(),
                style = PetruType.Body,
                color = PetruColors.OnBackground,
                modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp)
                    .background(PetruColors.SurfaceHigh, RoundedCornerShape(12.dp)).padding(16.dp),
            )
        }
    }
}

@Composable
private fun TemplateCard(template: Template, enabled: Boolean, onUse: (String) -> Unit) {
    val seconds = "%.0f s".format(template.lengthMs / MS_PER_SECOND)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(PetruColors.SurfaceHigh).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SlotSample(template)
        Text(keyedText("template_${template.id}_name"), style = PetruType.Title, color = PetruColors.OnBackground)
        Text(
            keyedText("template_${template.id}_summary"),
            style = PetruType.Caption,
            color = PetruColors.OnSurfaceMuted,
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${template.slotCount} clips · $seconds",
                style = PetruType.Caption,
                color = PetruColors.OnSurfaceMuted,
                modifier = Modifier.weight(1f),
            )
            PillButton(stringResource(R.string.projects_use), onClick = { if (enabled) onUse(template.id) })
        }
    }
}

/** A drawn sample of the template: one block per slot, as long as the slot, tinted by the look. */
@Composable
private fun SlotSample(template: Template) {
    val tint = lookTint(template.look)
    Row(
        Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(8.dp))
            .semantics { contentDescription = "${template.slotCount} slots" },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        template.slotsMs.forEachIndexed { i, ms ->
            Box(
                Modifier.weight(ms.toFloat()).fillMaxSize()
                    .background(tint.copy(alpha = if (i % 2 == 0) 1f else 0.7f)),
            )
        }
    }
}

private fun lookTint(look: FilterLook): Color = when (look) {
    FilterLook.Vivid -> Color(0xFFE5486B)
    FilterLook.Warm -> Color(0xFFE59A48)
    FilterLook.Cool -> Color(0xFF48A5E5)
    FilterLook.BlackAndWhite -> Color(0xFF8A8A8A)
    FilterLook.Vintage -> Color(0xFFB08A5A)
    FilterLook.Faded -> Color(0xFF9AA7B8)
    FilterLook.None -> Color(0xFF6B7280)
}
