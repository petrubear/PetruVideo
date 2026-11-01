package com.petruvideo.feature.editor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.designsystem.components.PillButton
import com.petruvideo.core.designsystem.components.SheetSurface

/** The progress of making a backwards copy, with Cancel, above the clip tool bar. */
@Composable
fun ReverseProgress(percent: Int?, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    if (percent == null) return
    val description = "Reversing, $percent percent"
    SheetSurface(modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Reversing $percent%",
                    style = PetruType.Title,
                    color = PetruColors.OnBackground,
                    modifier = Modifier.weight(1f),
                )
                PillButton("Cancel", onClick = onCancel, modifier = Modifier.defaultMinSize(minHeight = 56.dp))
            }
            LinearProgressIndicator(
                progress = { percent / PERCENT },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = description },
                color = PetruColors.Accent,
                trackColor = PetruColors.SurfaceHigh,
            )
        }
    }
}

private const val PERCENT = 100f
