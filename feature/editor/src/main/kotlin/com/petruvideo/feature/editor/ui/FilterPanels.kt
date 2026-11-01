package com.petruvideo.feature.editor.ui

import androidx.compose.foundation.Image
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
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
import com.petruvideo.core.model.ClipFilter
import com.petruvideo.core.model.ColorMath
import com.petruvideo.core.model.FILTER_ADJUST_MAX
import com.petruvideo.core.model.FilterLook
import com.petruvideo.core.model.MediaItem
import com.petruvideo.feature.editor.EditorEvent
import com.petruvideo.feature.editor.FilterPanelState
import com.petruvideo.feature.editor.FilterToolKind
import kotlin.math.roundToInt

private val MAX_BODY = 300.dp
private val MAX_BODY_TABLETOP = 120.dp
private val TILE = 72.dp
private const val DISABLED_ALPHA = 0.4f

/** Android's 4x5 color matrix (offsets 0..255) for the same transform the video gets, so tiles match the picture. */
internal fun lookColorMatrix(filter: ClipFilter): ColorMatrix {
    val m = ColorMath.matrix(filter)
    return ColorMatrix(
        floatArrayOf(
            m[0], m[4], m[8], 0f, m[12] * 255f,
            m[1], m[5], m[9], 0f, m[13] * 255f,
            m[2], m[6], m[10], 0f, m[14] * 255f,
            0f, 0f, 0f, 1f, 0f,
        ),
    )
}

/** The Looks or Adjust panel above the clip tool bar. */
@Composable
fun FilterPanel(panel: FilterPanelState?, onEvent: (EditorEvent) -> Unit, modifier: Modifier = Modifier) {
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
                PillButton("Done", onClick = { onEvent(EditorEvent.FilterDismiss) })
            }
            val tabletop = LocalArrangement.current is WindowArrangement.Tabletop
            val maxBody = if (tabletop) MAX_BODY_TABLETOP else MAX_BODY
            Column(Modifier.heightIn(max = maxBody).verticalScroll(rememberScrollState())) {
                when (panel.kind) {
                    FilterToolKind.Looks -> LooksControls(panel, onEvent)
                    FilterToolKind.Adjust -> AdjustControls(panel.filter, onEvent)
                }
            }
        }
    }
}

@Composable
fun LooksControls(panel: FilterPanelState, onEvent: (EditorEvent) -> Unit, modifier: Modifier = Modifier) {
    var shown by remember(panel.filter) { mutableStateOf(panel.filter) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterLook.entries.forEach { look ->
                LookTile(look, panel.media, panel.frameTimeMs, shown.look == look) {
                    onEvent(EditorEvent.FilterCommit(shown.copy(look = look)))
                }
            }
        }
        if (shown.look != FilterLook.None) {
            SettingSlider(
                "Intensity", "${shown.intensity}%", shown.intensity.toFloat(), 0f..PERCENT_MAX,
                {
                    shown = shown.copy(intensity = it.roundToInt())
                    onEvent(EditorEvent.FilterPreview(shown))
                },
                { onEvent(EditorEvent.FilterCommit(shown)) },
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.alpha(if (panel.canApplyAll) 1f else DISABLED_ALPHA)) {
                TextAction("Apply to all") { if (panel.canApplyAll) onEvent(EditorEvent.FilterApplyAll) }
            }
            TextAction("Remove filter") { onEvent(EditorEvent.FilterRemove) }
        }
    }
}

private const val PERCENT_MAX = 100f

@Composable
private fun LookTile(look: FilterLook, media: MediaItem?, frameTimeMs: Long, selected: Boolean, onClick: () -> Unit) {
    val loaders = LocalMediaLoaders.current
    val frame by produceState<ImageBitmap?>(null, media?.id, frameTimeMs) {
        value = media?.let { loaders.thumbnail(it, frameTimeMs, TILE_PX) }
    }
    val edge = if (selected) PetruColors.Accent else PetruColors.SurfaceHighest
    Column(
        Modifier
            .clickable(role = Role.Button, onClick = onClick)
            .padding(2.dp)
            .semantics {
                contentDescription = "Look ${look.label}"
                this.selected = selected
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier
                .size(TILE)
                .clip(RoundedCornerShape(8.dp))
                .background(PetruColors.SurfaceHigh)
                .border(if (selected) 3.dp else 1.dp, edge, RoundedCornerShape(8.dp)),
        ) {
            frame?.let {
                Image(
                    it, null, Modifier.size(TILE), contentScale = ContentScale.Crop,
                    colorFilter = ColorFilter.colorMatrix(lookColorMatrix(ClipFilter(look = look))),
                )
            }
        }
        Text(look.label, style = PetruType.Tiny, color = if (selected) PetruColors.Accent else PetruColors.OnBackground)
    }
}

private const val TILE_PX = 160

@Composable
fun AdjustControls(filter: ClipFilter, onEvent: (EditorEvent) -> Unit, modifier: Modifier = Modifier) {
    var shown by remember(filter) { mutableStateOf(filter) }
    val range = -FILTER_ADJUST_MAX.toFloat()..FILTER_ADJUST_MAX.toFloat()
    val finish = { onEvent(EditorEvent.FilterCommit(shown)) }
    val change = { next: ClipFilter ->
        shown = next
        onEvent(EditorEvent.FilterPreview(next))
    }
    Column(modifier.fillMaxWidth()) {
        SettingSlider("Brightness", signed(shown.brightness), shown.brightness.toFloat(), range,
            { change(shown.copy(brightness = it.roundToInt())) }, finish)
        SettingSlider("Contrast", signed(shown.contrast), shown.contrast.toFloat(), range,
            { change(shown.copy(contrast = it.roundToInt())) }, finish)
        SettingSlider("Saturation", signed(shown.saturation), shown.saturation.toFloat(), range,
            { change(shown.copy(saturation = it.roundToInt())) }, finish)
        SettingSlider("Warmth", signed(shown.warmth), shown.warmth.toFloat(), range,
            { change(shown.copy(warmth = it.roundToInt())) }, finish)
        SettingSlider("Vignette", "${shown.vignette}", shown.vignette.toFloat(), 0f..FILTER_ADJUST_MAX.toFloat(),
            { change(shown.copy(vignette = it.roundToInt())) }, finish)
        TextAction("Reset") { onEvent(EditorEvent.FilterResetAdjustments) }
    }
}

private fun signed(value: Int) = if (value > 0) "+$value" else "$value"

