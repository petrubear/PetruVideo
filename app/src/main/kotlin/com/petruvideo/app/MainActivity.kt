package com.petruvideo.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
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
        // The app is dark only: keep system bar icons light whatever the system theme is.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        val arrangementProvider = WindowArrangementProvider(this, lifecycleScope)
        setContent {
            val input by arrangementProvider.input.collectAsStateWithLifecycle()
            CompositionLocalProvider(LocalArrangement provides ArrangementResolver.resolve(input)) {
                PetruTheme { PetruNavHost() }
            }
        }
    }
}
