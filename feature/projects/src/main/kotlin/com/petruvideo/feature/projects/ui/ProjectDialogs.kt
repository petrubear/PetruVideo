package com.petruvideo.feature.projects.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.petruvideo.core.designsystem.PetruColors
import androidx.compose.ui.res.stringResource
import com.petruvideo.feature.projects.R

@Composable
fun RenameDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.projects_rename_project)) },
        text = { OutlinedTextField(value = name, onValueChange = { name = it.take(MAX_NAME) }, singleLine = true) },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text(stringResource(R.string.projects_rename)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.projects_cancel)) } },
    )
}

/** Deleting removes the project only; the original media files on the device are never touched. */
@Composable
fun DeleteConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.projects_delete_this_project)) },
        text = { Text(stringResource(R.string.projects_the_project_and_its_edits)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.projects_delete), color = PetruColors.Danger) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.projects_cancel)) } },
    )
}

private const val MAX_NAME = 80
