package com.petruvideo.feature.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.petruvideo.core.designsystem.PetruColors

private val HANDLE_SHAPE = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp, bottomStart = 8.dp, bottomEnd = 8.dp)

/** The fixed center playhead: a 2 dp accent line with a handle on top. Place it in the middle of the viewport. */
@Composable
fun Playhead(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxHeight().width(16.dp), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.offset(y = 14.dp).fillMaxHeight().width(2.dp).background(PetruColors.Accent))
        Box(
            Modifier
                .height(10.dp)
                .width(16.dp)
                .background(PetruColors.Accent, HANDLE_SHAPE),
        )
    }
}
