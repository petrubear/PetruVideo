package com.petruvideo.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalTextApi::class)
private fun variableFamily(resId: Int, vararg weights: Int) = FontFamily(
    weights.map { w ->
        Font(resId, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
    },
)

val Manrope = variableFamily(R.font.manrope, 400, 500, 600, 700, 800)
val JetBrainsMono = variableFamily(R.font.jetbrains_mono, 500)

private fun manrope(size: Int, weight: Int, letterSpacing: TextUnit = 0.em) = TextStyle(
    fontFamily = Manrope,
    fontSize = size.sp,
    fontWeight = FontWeight(weight),
    letterSpacing = letterSpacing,
)

/** Type scale read from the designer's screens. Sizes scale with the system font size (sp). */
object PetruType {
    val Display = manrope(30, 800, (-0.02).em)
    val Title = manrope(18, 700)
    val TitleSmall = manrope(17, 700)
    val Label = manrope(15, 700)
    val Body = manrope(14, 500)
    val BodySmall = manrope(13, 500)
    val Caption = manrope(12, 500)
    val Overline = manrope(12, 700, 0.06.em)
    val Tiny = manrope(11, 600)
    val Mono = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    val MonoSmall = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.sp, fontWeight = FontWeight.Medium)
}

internal val MaterialTypography = Typography(
    displaySmall = PetruType.Display,
    titleLarge = PetruType.Title,
    titleMedium = PetruType.TitleSmall,
    labelLarge = PetruType.Label,
    bodyLarge = PetruType.Body,
    bodyMedium = PetruType.Body,
    bodySmall = PetruType.BodySmall,
    labelMedium = PetruType.Caption,
    labelSmall = PetruType.Tiny,
)
