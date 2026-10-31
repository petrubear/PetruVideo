package com.petruvideo.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object PetruShapes {
    val Chip = RoundedCornerShape(16.dp)
    val Button = RoundedCornerShape(14.dp)
    val Thumb = RoundedCornerShape(10.dp)
    val Clip = RoundedCornerShape(7.dp)
    val Card = RoundedCornerShape(20.dp)
    val CardLarge = RoundedCornerShape(22.dp)
    val Sheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val Panel = RoundedCornerShape(24.dp)
}

internal val MaterialShapes = Shapes(
    small = PetruShapes.Clip,
    medium = PetruShapes.Button,
    large = PetruShapes.Card,
)
