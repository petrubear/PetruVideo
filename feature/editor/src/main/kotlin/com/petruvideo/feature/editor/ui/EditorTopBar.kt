package com.petruvideo.feature.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.petruvideo.core.data.autosave.SaveStatus
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.designsystem.Spacing
import com.petruvideo.core.designsystem.components.IconAction
import com.petruvideo.core.designsystem.icons.PetruIcons
import com.petruvideo.feature.editor.EditorHeader
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.stringResource
import com.petruvideo.feature.editor.R

/** Back button, project name with format and save status, and the Export button. */
@Composable
fun EditorTopBar(
    header: EditorHeader?,
    onBack: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().height(56.dp).padding(start = 4.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconAction(PetruIcons.Back, stringResource(R.string.editor_back), onBack)
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
            Text(
                header?.name.orEmpty(),
                style = PetruType.Body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                color = PetruColors.OnBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(statusLine(header), style = PetruType.MonoSmall, color = PetruColors.OnSurfaceMuted)
        }
        ExportButton(onExport)
    }
}

private fun statusLine(header: EditorHeader?): String {
    if (header == null) return ""
    val save = when (header.saveStatus) {
        SaveStatus.Saved -> "saved"
        SaveStatus.Saving -> "saving"
        is SaveStatus.Failed -> "not saved"
        is SaveStatus.LowStorage -> "storage full: free space to keep saving"
    }
    return listOfNotNull(header.formatLabel, save).joinToString(" · ")
}

@Composable
private fun ExportButton(onClick: () -> Unit) {
    Box(
        Modifier
            .height(Spacing.MinTouchTarget)
            .clip(RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(R.string.editor_export),
            style = PetruType.BodySmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold),
            color = PetruColors.OnAccent,
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(PetruColors.Accent)
                .padding(horizontal = 16.dp, vertical = 9.dp),
        )
    }
}
