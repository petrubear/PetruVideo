package com.petruvideo.core.media.voiceover

import android.content.Context
import android.net.Uri
import com.petruvideo.core.model.Fingerprint
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** A take that was started and not yet turned into a clip: which project, where on the timeline, and its file. */
data class PendingTake(val projectId: String, val startMs: Long, val file: File)

/** The recordings folder in the app's own storage, the journal of the open take, and cleanup. */
@Singleton
class VoiceOverStore(private val root: File) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(File(context.filesDir, DIR))

    private val dir get() = root.apply { mkdirs() }
    private val journal get() = File(dir, JOURNAL)

    fun newFile(): File = File(dir, "${UUID.randomUUID()}.aac")

    fun freeBytes(): Long = dir.usableSpace

    fun openTake(projectId: String, startMs: Long, file: File) {
        journal.writeText("$projectId\n$startMs\n${file.name}\n")
    }

    fun closeTake() {
        journal.delete()
    }

    /** The take left behind by a crash or a lost process, if its file is still there. */
    fun pendingTake(): PendingTake? {
        val lines = runCatching { journal.readLines() }.getOrNull() ?: return null
        val start = lines.getOrNull(1)?.toLongOrNull()
        val name = lines.getOrNull(2)
        if (lines.size < 3 || start == null || name == null) return null
        return PendingTake(lines[0], start, File(dir, name))
    }

    /** The media item of a recording: the length is the measured time, not read from the stream (research 1). */
    fun mediaItemFor(file: File, durationMs: Long) = MediaItem(
        id = UUID.randomUUID().toString(),
        kind = MediaKind.Audio,
        uri = Uri.fromFile(file).toString(),
        displayName = "Voice-over",
        durationMs = durationMs,
        width = null,
        height = null,
        frameRate = null,
        hasAudio = true,
        fingerprint = Fingerprint(file.length(), file.lastModified(), durationMs, "audio/aac"),
    )

    /** Deletes recordings no media item uses any more, except the open take's file and anything under a minute old. */
    fun sweep(referencedUris: Set<String>, now: Long = System.currentTimeMillis()) {
        val keep = pendingTake()?.file?.name
        dir.listFiles()?.filter { it.extension == "aac" }?.forEach { file ->
            val used = Uri.fromFile(file).toString() in referencedUris
            if (!used && file.name != keep && now - file.lastModified() > FRESH_MS) file.delete()
        }
    }

    private companion object {
        const val DIR = "voiceovers"
        const val JOURNAL = "pending.txt"
        const val FRESH_MS = 60_000L
    }
}
