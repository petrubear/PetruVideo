package com.petruvideo.feature.projects.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

private val gradients = listOf(
    listOf(Color(0xFFF08F5A), Color(0xFFB8407A), Color(0xFF2A2150)),
    listOf(Color(0xFF2C8AA0), Color(0xFF12435E), Color(0xFF0A1A2E)),
    listOf(Color(0xFFA9D8FF), Color(0xFF4A86D6), Color(0xFF1D3B6B)),
    listOf(Color(0xFF3B2A5E), Color(0xFF8A4FD0), Color(0xFFE58BD1)),
    listOf(Color(0xFFE2EDB8), Color(0xFF74A96D), Color(0xFF23422F)),
)

/** The colored box shown under a project picture, and alone when a project has none (see ProjectThumbnail). */
@Composable
fun PlaceholderThumbnail(seed: String, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit = {}) {
    val colors = gradients[(seed.hashCode() and Int.MAX_VALUE) % gradients.size]
    Box(modifier.background(Brush.verticalGradient(colors))) {
        Box(Modifier.fillMaxSize()) { content() }
    }
}
