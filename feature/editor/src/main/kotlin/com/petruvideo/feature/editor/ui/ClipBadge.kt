package com.petruvideo.feature.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.model.SPEED_UNIT
import com.petruvideo.core.model.VOLUME_DEFAULT_PERCENT

/** "2x", "0.5x" for a speed in thousandths, without trailing zeros. */
internal fun speedBadgeText(speedMilli: Int): String =
    (speedMilli / 1000f).toString().removeSuffix(".0") + "x"

/** Small badge at a clip's top-left for a speed other than 1x and/or a volume other than 100%; nothing otherwise. */
@Composable
fun ClipBadge(speedMilli: Int, volumePercent: Int?, modifier: Modifier = Modifier) {
    val parts = listOfNotNull(
        speedBadgeText(speedMilli).takeIf { speedMilli != SPEED_UNIT },
        "$volumePercent%".takeIf { volumePercent != null && volumePercent != VOLUME_DEFAULT_PERCENT },
    )
    if (parts.isEmpty()) return
    val text = parts.joinToString(" ")
    Text(
        text,
        style = PetruType.MonoSmall,
        color = PetruColors.OnBackground,
        modifier = modifier
            .background(PetruColors.Background.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp)
            .semantics { contentDescription = "Speed and volume: $text" },
    )
}
