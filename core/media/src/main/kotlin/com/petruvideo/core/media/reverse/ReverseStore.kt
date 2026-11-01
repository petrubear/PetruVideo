package com.petruvideo.core.media.reverse

import android.content.Context
import com.petruvideo.core.model.MediaItem
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

/** What a reversed clip needs: a media item and the source range it plays. */
class ReverseNeed(val media: MediaItem, val startMs: Long, val endMs: Long)

/** A prepared backwards file and the source range it covers. */
class ReversedFile(val file: File, val startMs: Long, val endMs: Long) {
    /** Where in the file (forward time) the source range [from]..[to] sits; the file runs from [endMs] back. */
    fun fileRange(from: Long, to: Long): LongRange = (endMs - to)..(endMs - from)
}

/**
 * The backwards copies made for reversed clips (feature 017), kept in the app's storage by media fingerprint and source
 * range. A copy serves any clip whose range lies inside it, so trimming a reversed clip needs no new copy.
 */
class ReverseStore(private val root: File) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(File(context.filesDir, "reversed"))

    /** A prepared copy that covers [startMs]..[endMs] of [media], or null. */
    fun find(media: MediaItem, startMs: Long, endMs: Long): ReversedFile? =
        files(media).filter { it.startMs <= startMs && it.endMs >= endMs }.minByOrNull { it.endMs - it.startMs }

    /** Where the copy of exactly this range is to be written. */
    fun newFile(media: MediaItem, startMs: Long, endMs: Long): File =
        File(root.apply { mkdirs() }, "${key(media)}_${startMs}_$endMs.mp4")

    /** Whether the storage has room for the copy of a range: the intra pass and the result, about twice its size. */
    fun hasRoomFor(startMs: Long, endMs: Long): Boolean {
        root.mkdirs()
        val bytes = (endMs - startMs) * BYTES_PER_MS * 2
        return root.usableSpace > bytes + RESERVE
    }

    /** Deletes every copy that no entry of [needs] can use. */
    fun sweep(needs: List<ReverseNeed>) {
        root.listFiles { f -> f.extension == "mp4" }.orEmpty().forEach { file ->
            val parsed = parse(file) ?: return@forEach file.delete().let { }
            val used = needs.any { need ->
                key(need.media) == parsed.first && parsed.second <= need.startMs && parsed.third >= need.endMs
            }
            if (!used) file.delete()
        }
    }

    private fun files(media: MediaItem): List<ReversedFile> {
        val wanted = key(media)
        return root.listFiles { f -> f.extension == "mp4" }.orEmpty().mapNotNull { file ->
            parse(file)?.takeIf { it.first == wanted }?.let { ReversedFile(file, it.second, it.third) }
        }
    }

    private fun key(media: MediaItem): String {
        val f = media.fingerprint
        return "${f.sizeBytes}-${f.lastModified}-${f.durationMs}"
    }

    private fun parse(file: File): Triple<String, Long, Long>? {
        val parts = file.nameWithoutExtension.split('_')
        if (parts.size != PARTS) return null
        val start = parts[1].toLongOrNull() ?: return null
        val end = parts[2].toLongOrNull() ?: return null
        return Triple(parts[0], start, end)
    }

    private companion object {
        const val PARTS = 3
        const val BYTES_PER_MS = 5_000L
        const val RESERVE = 50L * 1024 * 1024
    }
}
