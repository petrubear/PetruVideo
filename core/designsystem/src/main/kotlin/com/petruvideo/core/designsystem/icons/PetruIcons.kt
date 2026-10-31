@file:Suppress("MaxLineLength") // SVG path data

package com.petruvideo.core.designsystem.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Stroke icons on a 24x24 grid, drawn in the designer's style (2 px round stroke, current tint). */
private fun strokeIcon(name: String, vararg paths: String, filled: Boolean = false): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        for (data in paths) {
            addPath(
                pathData = PathParser().parsePathString(data).toNodes(),
                fill = if (filled) SolidColor(Color.Black) else null,
                stroke = if (filled) null else SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()

object PetruIcons {
    val Search = strokeIcon("search", "M18 11a7 7 0 1 1-14 0a7 7 0 0 1 14 0Z", "M20 20l-3.5-3.5")
    val Settings = strokeIcon("settings", "M4 7h10M18 7h2M4 17h2M10 17h10", "M18 7a2 2 0 1 1-4 0a2 2 0 0 1 4 0Z", "M10 17a2 2 0 1 1-4 0a2 2 0 0 1 4 0Z")
    val Back = strokeIcon("back", "M15 5l-7 7l7 7")
    val Close = strokeIcon("close", "M6 6l12 12M18 6L6 18")
    val Undo = strokeIcon("undo", "M9 14L4 9l5-5", "M4 9h10a6 6 0 0 1 0 12h-3")
    val Redo = strokeIcon("redo", "M15 14l5-5l-5-5", "M20 9H10a6 6 0 0 0 0 12h3")
    val Play = strokeIcon("play", "M8 5v14l11-7z", filled = true)
    val Pause = strokeIcon("pause", "M7 5h3v14H7zM14 5h3v14h-3z", filled = true)
    val Fullscreen = strokeIcon("fullscreen", "M4 9V4h5M20 9V4h-5M4 15v5h5M20 15v5h-5")
    val Split = strokeIcon("split", "M12 3v18", "M7 8l-3 4l3 4M17 8l3 4l-3 4")
    val Speed = strokeIcon("speed", "M4 16a8 8 0 1 1 16 0", "M12 16l4-5")
    val Volume = strokeIcon("volume", "M4 9v6h4l5 4V5L8 9z", "M16.5 9a4 4 0 0 1 0 6")
    val Mute = strokeIcon("mute", "M4 9v6h4l5 4V5L8 9z", "M17 9l5 6M22 9l-5 6")
    val Detach = strokeIcon("detach", "M9 18V6l10-2v12", "M9 18a3 3 0 1 1-6 0a3 3 0 0 1 6 0Z", "M19 16a3 3 0 1 1-6 0a3 3 0 0 1 6 0Z", "M4 21l16-18")
    val Duplicate = strokeIcon("duplicate", "M9 9h11v11H9z", "M5 15V5h10")
    val Delete = strokeIcon("delete", "M4 7h16M10 11v6M14 11v6", "M6 7l1 13h10l1-13M9 7V4h6v3")
    val Replace = strokeIcon("replace", "M4 8h13l-3-3M20 16H7l3 3")
    val Add = strokeIcon("add", "M12 5v14M5 12h14")
    val More = strokeIcon("more", "M12 5v.01M12 12v.01M12 19v.01")
    val Export = strokeIcon("export", "M12 15V4M7 9l5-5l5 5M5 20h14")
    val Download = strokeIcon("download", "M12 4v11M7 10l5 5l5-5M5 20h14")

    val ToolEdit = strokeIcon("tool_edit", "M4 20l4-1l11-11l-3-3L5 16z", "M14 7l3 3")
    val ToolAudio = strokeIcon("tool_audio", "M9 18V6l10-2v12", "M9 18a3 3 0 1 1-6 0a3 3 0 0 1 6 0Z", "M19 16a3 3 0 1 1-6 0a3 3 0 0 1 6 0Z")
    val ToolText = strokeIcon("tool_text", "M5 6V4h14v2M12 4v16M9 20h6")
    val ToolOverlay = strokeIcon("tool_overlay", "M4 4h11v11H4z", "M9 9h11v11H9z")
    val ToolEffects = strokeIcon("tool_effects", "M12 3l2 6l6 2l-6 2l-2 6l-2-6l-6-2l6-2z")
    val ToolFilters = strokeIcon("tool_filters", "M9 12a5 5 0 1 0 10 0a5 5 0 0 0-10 0Z", "M5 12a5 5 0 1 0 5 5")
    val ToolMedia = strokeIcon("tool_media", "M3 5h18v14H3z", "M10 9.5v5l4.5-2.5z")

    val NavProjects = strokeIcon("nav_projects", "M3 7a3 3 0 0 1 3-3h12a3 3 0 0 1 3 3v10a3 3 0 0 1-3 3H6a3 3 0 0 1-3-3z", "M10 9.5v5l4.5-2.5z")
    val NavTemplates = strokeIcon("nav_templates", "M3 5a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2zM13 5a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v4a2 2 0 0 1-2 2h-4a2 2 0 0 1-2-2zM3 15a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2zM13 15a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v4a2 2 0 0 1-2 2h-4a2 2 0 0 1-2-2z")
    val NavExports = Download
}
