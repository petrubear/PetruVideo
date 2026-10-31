package com.petruvideo.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

object Routes {
    const val PROJECTS = "projects"
    const val ADD_MEDIA = "add_media"
    const val EDITOR = "editor/{projectId}"

    fun editor(projectId: String) = "editor/$projectId"
}

/** Navigation skeleton; each placeholder is replaced by its feature screen in the user story phases. */
@Composable
fun PetruNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(navController, startDestination = Routes.PROJECTS, modifier = modifier) {
        composable(Routes.PROJECTS) { Placeholder("Projects") }
        composable(Routes.ADD_MEDIA) { Placeholder("Add media") }
        composable(Routes.EDITOR) { Placeholder("Editor") }
    }
}

@Composable
private fun Placeholder(name: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(name) }
}
