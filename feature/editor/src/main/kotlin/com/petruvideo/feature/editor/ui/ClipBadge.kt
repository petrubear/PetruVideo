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
import com.petruvideo.core.model.AudioFx
import com.petruvideo.core.model.SPEED_UNIT
import com.petruvideo.core.model.VOLUME_DEFAULT_PERCENT

/** "2x", "0.5x" for a speed in thousandths, without trailing zeros. */
internal fun speedBadgeText(speedMilli: Int): String =
    (speedMilli / 1000f).toString().removeSuffix(".0") + "x"

private const val MAX_BADGES = 3

/** The badge labels of a clip, in order; speed, volume, reverb, then fade, noise, equalizer, and voice effects. */
internal fun badgeParts(
    speedMilli: Int,
    volumePercent: Int?,
    reverbOn: Boolean,
    fx: AudioFx?,
    filterOn: Boolean = false,
    effectOn: Boolean = false,
): List<String> =
    listOfNotNull(
        speedBadgeText(speedMilli).takeIf { speedMilli != SPEED_UNIT },
        "$volumePercent%".takeIf { volumePercent != null && volumePercent != VOLUME_DEFAULT_PERCENT },
        "Rv".takeIf { reverbOn },
        "Fd".takeIf { fx?.fade?.isNeutral == false },
        "Nr".takeIf { fx?.noise?.enabled == true },
        "Eq".takeIf { fx?.eq?.isFlat == false },
        "Vx".takeIf { fx?.voice?.anyOn == true },
        "Fl".takeIf { filterOn },
        "Fx".takeIf { effectOn },
    )

/** At most [MAX_BADGES] labels; the rest collapse into "+N" so the badge never outgrows a short clip. */
internal fun collapseBadges(parts: List<String>): List<String> =
    if (parts.size <= MAX_BADGES) parts else parts.take(MAX_BADGES - 1) + "+${parts.size - MAX_BADGES + 1}"

/** Small badge at a clip's top-left for its non-default settings; nothing when every setting is the default. */
@Composable
fun ClipBadge(
    speedMilli: Int,
    volumePercent: Int?,
    modifier: Modifier = Modifier,
    reverbOn: Boolean = false,
    fx: AudioFx? = null,
    filterOn: Boolean = false,
    effectOn: Boolean = false,
) {
    val all = badgeParts(speedMilli, volumePercent, reverbOn, fx, filterOn, effectOn)
    if (all.isEmpty()) return
    val text = collapseBadges(all).joinToString(" ")
    Text(
        text,
        style = PetruType.MonoSmall,
        color = PetruColors.OnBackground,
        modifier = modifier
            .background(PetruColors.Background.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp)
            .semantics { contentDescription = "Clip settings: ${all.joinToString(" ")}" },
    )
}
