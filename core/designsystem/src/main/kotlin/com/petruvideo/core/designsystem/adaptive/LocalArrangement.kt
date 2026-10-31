package com.petruvideo.core.designsystem.adaptive

import androidx.compose.runtime.staticCompositionLocalOf

/** The arrangement resolved for the current window, provided once at the app root. */
val LocalArrangement = staticCompositionLocalOf<Arrangement> { Arrangement.Compact(wideContent = false) }
