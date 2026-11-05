package com.petruvideo.feature.projects.ui

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.components.IconAction
import com.petruvideo.core.designsystem.icons.PetruIcons
import androidx.compose.ui.res.stringResource
import com.petruvideo.feature.projects.R

@Composable
fun ProjectOptionsMenu(
    projectName: String,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    IconAction(
        icon = PetruIcons.More,
        contentDescription = stringResource(R.string.projects_options_named, projectName),
        onClick = { expanded = true },
        modifier = modifier,
        tint = PetruColors.OnSurfaceMuted,
    )
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(text = { Text(stringResource(R.string.projects_rename)) }, onClick = { expanded = false; onRename() })
        DropdownMenuItem(text = { Text(stringResource(R.string.projects_duplicate)) }, onClick = { expanded = false; onDuplicate() })
        DropdownMenuItem(
            text = { Text(stringResource(R.string.projects_delete), color = PetruColors.Danger) },
            onClick = { expanded = false; onDelete() },
        )
    }
}
