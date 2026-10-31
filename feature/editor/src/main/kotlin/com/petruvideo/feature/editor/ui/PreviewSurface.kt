package com.petruvideo.feature.editor.ui

import android.view.View
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

/** Draws the preview player on black, fitted inside the frame (clips of another size get bars, never stretch). */
@OptIn(UnstableApi::class)
@Composable
fun PreviewSurface(player: Player?, modifier: Modifier = Modifier) {
    Box(modifier.background(Color.Black)) {
        if (player != null) {
            AndroidView(
                factory = { context ->
                    PlayerView(context).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        setShutterBackgroundColor(android.graphics.Color.BLACK)
                        setKeepContentOnPlayerReset(true)
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    }
                },
                update = { it.player = player },
                onRelease = { it.player = null },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
