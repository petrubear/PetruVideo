package com.petruvideo.feature.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.Player
import com.petruvideo.core.designsystem.components.IconAction
import com.petruvideo.core.designsystem.icons.PetruIcons
import androidx.compose.ui.res.stringResource
import com.petruvideo.feature.editor.R

/**
 * Full-screen preview. It draws the same player, so playback continues from the same position and nothing is
 * paused or reloaded when it opens or closes.
 */
@Composable
fun FullscreenPreview(player: Player?, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            PreviewSurface(player, Modifier.fillMaxSize())
            IconAction(
                PetruIcons.Close,
                stringResource(R.string.editor_close_full_screen),
                onClose,
                Modifier.align(Alignment.TopStart).statusBarsPadding(),
                tint = Color.White,
            )
        }
    }
}
