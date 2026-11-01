package com.petruvideo.feature.editor.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.designsystem.adaptive.Arrangement as WindowArrangement
import com.petruvideo.core.designsystem.adaptive.LocalArrangement
import com.petruvideo.core.designsystem.components.PetruChip
import com.petruvideo.core.designsystem.components.PillButton
import com.petruvideo.core.designsystem.components.SheetSurface
import com.petruvideo.core.model.ANIM_IN_KINDS
import com.petruvideo.core.model.ANIM_MAX_MS
import com.petruvideo.core.model.ANIM_MIN_MS
import com.petruvideo.core.model.ANIM_OUT_KINDS
import com.petruvideo.core.model.AnimKind
import com.petruvideo.core.model.TEXT_MAX_CHARS
import com.petruvideo.core.model.TEXT_OUTLINE_MAX
import com.petruvideo.core.model.TEXT_SIZE_MAX
import com.petruvideo.core.model.TEXT_SIZE_MIN
import com.petruvideo.core.model.TextAlign
import com.petruvideo.core.model.TextAnimation
import com.petruvideo.core.model.TextClip
import com.petruvideo.core.model.TextFont
import com.petruvideo.feature.editor.EditorEvent
import com.petruvideo.feature.editor.TextPanelState
import com.petruvideo.feature.editor.TextToolKind
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val MAX_BODY = 300.dp
private val MAX_BODY_TABLETOP = 120.dp
private const val TYPING_PAUSE_MS = 800L

/** The Edit, Style, or Animation panel of the selected text clip, above the clip tool bar. */
@Composable
fun TextPanel(panel: TextPanelState?, onEvent: (EditorEvent) -> Unit, modifier: Modifier = Modifier) {
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
                PillButton("Done", onClick = { onEvent(EditorEvent.TextDismiss) })
            }
            val tabletop = LocalArrangement.current is WindowArrangement.Tabletop
            val maxBody = if (tabletop) MAX_BODY_TABLETOP else MAX_BODY
            Column(Modifier.heightIn(max = maxBody).verticalScroll(rememberScrollState())) {
                TextPanelBody(panel, onEvent)
            }
        }
    }
}

/** The body of one text tool; the inspector uses these directly. */
@Composable
fun TextPanelBody(panel: TextPanelState, onEvent: (EditorEvent) -> Unit) {
    when (panel.kind) {
        TextToolKind.Edit -> TextEditControls(panel.clip, onEvent)
        TextToolKind.Style -> TextStyleControls(panel.clip, onEvent)
        TextToolKind.Animation -> TextAnimationControls(panel.clip, onEvent)
    }
}

@Composable
fun TextEditControls(clip: TextClip, onEvent: (EditorEvent) -> Unit, modifier: Modifier = Modifier) {
    var text by remember(clip.id) { mutableStateOf(clip.text) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(clip.id) { focus.requestFocus() }
    LaunchedEffect(text) {
        if (text != clip.text) {
            delay(TYPING_PAUSE_MS)
            onEvent(EditorEvent.TextCommit(clip.copy(text = text)))
        }
    }
    Column(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it.take(TEXT_MAX_CHARS)
                onEvent(EditorEvent.TextPreview(clip.copy(text = text)))
            },
            modifier = Modifier.fillMaxWidth().focusRequester(focus).semantics { contentDescription = "Text" },
            minLines = 2,
            maxLines = 5,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = PetruColors.SurfaceHigh,
                unfocusedContainerColor = PetruColors.SurfaceHigh,
                focusedTextColor = PetruColors.OnBackground,
                unfocusedTextColor = PetruColors.OnBackground,
                cursorColor = PetruColors.Accent,
            ),
        )
        Text("${text.length} / $TEXT_MAX_CHARS", style = PetruType.Caption, color = PetruColors.OnSurfaceMuted)
    }
}

@Composable
fun TextStyleControls(clip: TextClip, onEvent: (EditorEvent) -> Unit, modifier: Modifier = Modifier) {
    var shown by remember(clip) { mutableStateOf(clip) }
    val preview = { next: TextClip ->
        shown = next
        onEvent(EditorEvent.TextPreview(next))
    }
    val commit = { next: TextClip ->
        shown = next
        onEvent(EditorEvent.TextCommit(next))
    }
    val style = shown.style
    Column(modifier.fillMaxWidth()) {
        FontAndAlign(shown, commit)
        SettingSlider(
            "Size", "${shown.sizePermille / PERCENT_DIV}%", shown.sizePermille.toFloat(),
            TEXT_SIZE_MIN.toFloat()..TEXT_SIZE_MAX.toFloat(),
            { preview(shown.copy(sizePermille = it.roundToInt())) }, { commit(shown) },
        )
        Text("Color", style = PetruType.Overline, color = PetruColors.OnSurfaceMuted)
        ColorPicker(
            "Text color", style.colorArgb,
            { preview(shown.copy(style = style.copy(colorArgb = it))) },
            { commit(shown.copy(style = style.copy(colorArgb = it))) },
        )
        BackgroundControls(shown, preview, commit)
        OutlineControls(shown, preview, commit)
        TextAction("Reset style") { onEvent(EditorEvent.TextReset(TextToolKind.Style)) }
    }
}

private const val PERCENT_DIV = 10f

@Composable
private fun FontAndAlign(clip: TextClip, commit: (TextClip) -> Unit) {
    val style = clip.style
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextFont.entries.forEach { font ->
            PetruChip(
                font.label,
                selected = style.font == font,
                onClick = { commit(clip.copy(style = style.copy(font = font))) },
            )
        }
    }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextAlign.entries.forEach { align ->
            PetruChip(
                align.label,
                selected = style.align == align,
                onClick = { commit(clip.copy(style = style.copy(align = align))) },
            )
        }
    }
}

@Composable
private fun BackgroundControls(clip: TextClip, preview: (TextClip) -> Unit, commit: (TextClip) -> Unit) {
    val bg = clip.style.background
    FxSwitch("Background", bg.enabled) {
        commit(clip.copy(style = clip.style.copy(background = bg.copy(enabled = it))))
    }
    if (!bg.enabled) return
    ColorPicker(
        "Background color", bg.colorArgb,
        { preview(clip.copy(style = clip.style.copy(background = bg.copy(colorArgb = it)))) },
        { commit(clip.copy(style = clip.style.copy(background = bg.copy(colorArgb = it)))) },
    )
    SettingSlider(
        "Opacity", "${bg.opacityPercent}%", bg.opacityPercent.toFloat(), 0f..PERCENT_MAX,
        { preview(clip.copy(style = clip.style.copy(background = bg.copy(opacityPercent = it.roundToInt())))) },
        { commit(clip) },
    )
}

@Composable
private fun OutlineControls(clip: TextClip, preview: (TextClip) -> Unit, commit: (TextClip) -> Unit) {
    val outline = clip.style.outline
    FxSwitch("Outline", outline.enabled) {
        commit(clip.copy(style = clip.style.copy(outline = outline.copy(enabled = it))))
    }
    if (!outline.enabled) return
    ColorPicker(
        "Outline color", outline.colorArgb,
        { preview(clip.copy(style = clip.style.copy(outline = outline.copy(colorArgb = it)))) },
        { commit(clip.copy(style = clip.style.copy(outline = outline.copy(colorArgb = it)))) },
    )
    SettingSlider(
        "Outline width", "${outline.widthPermille / PERCENT_DIV}%", outline.widthPermille.toFloat(),
        0f..TEXT_OUTLINE_MAX.toFloat(),
        { preview(clip.copy(style = clip.style.copy(outline = outline.copy(widthPermille = it.roundToInt())))) },
        { commit(clip) },
    )
}

private const val PERCENT_MAX = 100f

@Composable
fun TextAnimationControls(clip: TextClip, onEvent: (EditorEvent) -> Unit, modifier: Modifier = Modifier) {
    var shown by remember(clip) { mutableStateOf(clip) }
    val preview = { next: TextClip ->
        shown = next
        onEvent(EditorEvent.TextPreview(next))
    }
    val commit = { next: TextClip ->
        shown = next
        onEvent(EditorEvent.TextCommit(next))
    }
    Column(modifier.fillMaxWidth()) {
        Text("In", style = PetruType.Overline, color = PetruColors.OnSurfaceMuted)
        AnimationRow(
            ANIM_IN_KINDS, shown.animIn,
            { commit(shown.copy(animIn = it)) }, { preview(shown.copy(animIn = it)) }, { commit(shown) }, "In",
        )
        Text("Out", style = PetruType.Overline, color = PetruColors.OnSurfaceMuted)
        AnimationRow(
            ANIM_OUT_KINDS, shown.animOut,
            { commit(shown.copy(animOut = it)) }, { preview(shown.copy(animOut = it)) }, { commit(shown) }, "Out",
        )
        TextAction("Reset animation") { onEvent(EditorEvent.TextReset(TextToolKind.Animation)) }
    }
}

@Composable
private fun AnimationRow(
    kinds: List<AnimKind>,
    value: TextAnimation,
    onKind: (TextAnimation) -> Unit,
    onLength: (TextAnimation) -> Unit,
    onFinished: () -> Unit,
    name: String,
) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        kinds.forEach { kind ->
            PetruChip(kind.label, selected = value.kind == kind, onClick = { onKind(value.copy(kind = kind)) })
        }
    }
    if (value.kind != AnimKind.None) {
        SettingSlider(
            "$name length", "%.1f s".format(java.util.Locale.US, value.lengthMs / 1000f), value.lengthMs.toFloat(),
            ANIM_MIN_MS.toFloat()..ANIM_MAX_MS.toFloat(),
            { onLength(value.copy(lengthMs = it.roundToInt())) }, onFinished,
        )
    }
}
