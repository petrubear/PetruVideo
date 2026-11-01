package com.petruvideo.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.petruvideo.feature.editor.EditorRoute
import com.petruvideo.feature.export.ExportRoute
import com.petruvideo.feature.export.ExportsRoute
import com.petruvideo.feature.importmedia.AddMediaRoute
import com.petruvideo.feature.projects.ProjectsRoute
import com.petruvideo.feature.projects.settings.SettingsRoute
import com.petruvideo.feature.projects.templates.TemplatesRoute

object Routes {
    const val PROJECTS = "projects"
    const val ADD_MEDIA = "add_media"
    const val EDITOR = "editor/{projectId}"
    const val EXPORT = "export/{projectId}"
    const val EXPORTS = "exports"
    const val SETTINGS = "settings"
    const val TEMPLATES = "templates"

    fun editor(projectId: String) = "editor/$projectId"

    fun export(projectId: String) = "export/$projectId"
}

/** Navigation skeleton; each placeholder is replaced by its feature screen in the user story phases. */
@Composable
fun PetruNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(navController, startDestination = Routes.PROJECTS, modifier = modifier) {
        composable(Routes.PROJECTS) {
            ProjectsRoute(
                onOpenEditor = { navController.navigate(Routes.editor(it)) },
                onOpenExport = { navController.navigate(Routes.export(it)) },
                onOpenExports = { navController.navigate(Routes.EXPORTS) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenTemplates = { navController.navigate(Routes.TEMPLATES) },
            )
        }
        composable(Routes.ADD_MEDIA) {
            val session: SessionViewModel = hiltViewModel()
            AddMediaRoute(
                onItemsReady = session::addMedia,
                onClose = { navController.popBackStack() },
            )
        }
        composable(
            Routes.EDITOR,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
        ) { entry ->
            EditorRoute(
                projectId = entry.arguments?.getString("projectId").orEmpty(),
                onBack = { navController.popBackStack() },
                onAddMedia = { navController.navigate(Routes.ADD_MEDIA) },
                onExport = {
                    entry.arguments?.getString("projectId")?.let { navController.navigate(Routes.export(it)) }
                },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsRoute(onBack = { navController.popBackStack() })
        }
        composable(Routes.TEMPLATES) {
            TemplatesRoute(
                onBack = { navController.popBackStack() },
                onOpenEditor = {
                    navController.popBackStack()
                    navController.navigate(Routes.editor(it))
                },
            )
        }
        composable(Routes.EXPORTS) {
            ExportsRoute(onBack = { navController.popBackStack() })
        }
        composable(
            Routes.EXPORT,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
        ) { entry ->
            ExportRoute(
                projectId = entry.arguments?.getString("projectId").orEmpty(),
                onBack = { navController.popBackStack() },
            )
        }
    }
}
