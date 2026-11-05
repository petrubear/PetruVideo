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
import com.petruvideo.core.data.repo.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val arrangementProvider = WindowArrangementProvider(this, lifecycleScope)
        setContent {
            val input by arrangementProvider.input.collectAsStateWithLifecycle()
            val settings by settingsRepository.settings.collectAsStateWithLifecycle()
            val darkTheme = settings.theme.isDark(isSystemInDarkTheme())
            // System bar icons follow the look in use, at start and whenever the choice or the phone's mode changes.
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
