package com.petruvideo.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.petruvideo.core.designsystem.adaptive.ArrangementResolver
import com.petruvideo.core.designsystem.adaptive.LocalArrangement
import com.petruvideo.core.designsystem.adaptive.WindowArrangementProvider
import com.petruvideo.app.navigation.PetruNavHost
import com.petruvideo.core.designsystem.PetruTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val arrangementProvider = WindowArrangementProvider(this, lifecycleScope)
        setContent {
            val input by arrangementProvider.input.collectAsStateWithLifecycle()
            val darkTheme = isSystemInDarkTheme()
            // System bar icons follow the look, at start and whenever the phone switches between light and dark.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                )
                onDispose {}
            }
            CompositionLocalProvider(LocalArrangement provides ArrangementResolver.resolve(input)) {
                PetruTheme(darkTheme) { PetruNavHost() }
            }
        }
    }
}
