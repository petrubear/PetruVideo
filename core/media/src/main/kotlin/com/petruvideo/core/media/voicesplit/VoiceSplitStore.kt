package com.petruvideo.core.media.voicesplit

import android.content.Context
import android.net.Uri
import com.petruvideo.core.model.MediaItem
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/** The two files of one split: the voice and everything else (feature 027). */
class SplitFiles(val voice: File, val backing: File) {
    val exists: Boolean get() = voice.length() > 0 && backing.length() > 0
}

/** Keeps the split results in the app's own storage, keyed by the source file and the range that was split. */
@Singleton
class VoiceSplitStore(private val root: File) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(File(context.filesDir, DIR))

    /** The files a split of [media] from [startMs] to [endMs] is (or will be) saved in. */
    fun filesFor(media: MediaItem, startMs: Long, endMs: Long): SplitFiles {
        root.mkdirs()
        val f = media.fingerprint
        val key = "${media.uri}|${f.sizeBytes}|${f.lastModified}|${f.durationMs}|$startMs|$endMs"
        val name = MessageDigest.getInstance("SHA-1").digest(key.toByteArray()).joinToString("") { "%02x".format(it) }
        return SplitFiles(File(root, "$name-voice.wav"), File(root, "$name-backing.wav"))
    }

    fun temporary(target: File) = File(root, target.name + ".part")

    /** Deletes split files no media item uses any more, and unfinished ones, except anything under a minute old. */
    fun sweep(referencedUris: Set<String>, now: Long = System.currentTimeMillis()) {
        root.listFiles()?.forEach { file ->
            val used = Uri.fromFile(file).toString() in referencedUris
            if (!used && now - file.lastModified() > FRESH_MS) file.delete()
        }
    }

    private companion object {
        const val DIR = "voicesplit"
        const val FRESH_MS = 60_000L
    }
}
