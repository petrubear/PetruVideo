package com.petruvideo.core.media.thumbs

import java.io.File

/**
 * Where a project's thumbnail picture lives: `<projectId>-<key>.png`, where the key follows the first clip, so a new
 * first clip always means a new path (the stored path is then the only thing a screen needs to watch).
 */
object ProjectThumbFiles {
    fun fileFor(dir: File, projectId: String, clipKey: String): File =
        File(dir, "$projectId-${clipKey.hashCode().toUInt().toString(HEX)}.png")

    /** Deletes every picture of [projectId] (the older name `<projectId>.png` too) except [keep]. */
    fun deleteOthers(dir: File, projectId: String, keep: File?) {
        dir.listFiles()?.forEach { file ->
            val ofProject = file.name == "$projectId.png" || file.name.startsWith("$projectId-")
            if (ofProject && file != keep) file.delete()
        }
    }

    private const val HEX = 16
}
