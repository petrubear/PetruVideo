package com.petruvideo.feature.editor

import java.util.Locale

/** "1.50x" for a speed in thousandths (1000 = 1x). */
fun formatSpeed(speedMilli: Int): String = String.format(Locale.US, "%.2fx", speedMilli / 1000f)
