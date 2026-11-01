package com.petruvideo.core.media.reverse

import android.content.Context
import android.net.Uri
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.Timeline
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Where a reversed clip plays from: a prepared file and the part of it (forward time) that the clip uses. */
class ReversedSource(val uri: String, val startMs: Long, val endMs: Long, val fileDurationMs: Long)

/** What the editor needs to prepare, watch, and cancel reversed copies (feature 017). */
interface ReversePreparer {
    /** Progress (0 to 100) of each copy being made, by clip id; a clip leaves the map when its copy is done. */
    val progress: StateFlow<Map<String, Int>>

    /** Whether there is room to make a copy of the range. */
    fun hasRoom(startMs: Long, endMs: Long): Boolean

    /** Makes the copy for the clip's range if there is none yet; false when it could not be made. */
    suspend fun prepare(clipId: String, media: MediaItem, startMs: Long, endMs: Long): Boolean

    /** The prepared sources of the reversed clips of [timeline] that already have a copy, by clip id. */
    fun sources(timeline: Timeline): Map<String, ReversedSource>

    /** Makes every missing copy the timeline needs; false when one could not be made. */
    suspend fun ensure(timeline: Timeline): Boolean

    /** Deletes the copies no entry of [needs] can use. */
    fun sweep(needs: List<ReverseNeed>)
}

/** The preparer used where reversing is not wired in (tests): nothing is ever prepared. */
object NoReversePreparer : ReversePreparer {
    override val progress: StateFlow<Map<String, Int>> = MutableStateFlow(emptyMap())

    override fun hasRoom(startMs: Long, endMs: Long) = false

    override suspend fun prepare(clipId: String, media: MediaItem, startMs: Long, endMs: Long) = false

    override fun sources(timeline: Timeline): Map<String, ReversedSource> = emptyMap()

    override suspend fun ensure(timeline: Timeline) = true

    override fun sweep(needs: List<ReverseNeed>) = Unit
}

/** Makes the backwards copies with [ReverseRenderer] one at a time and keeps them in a [ReverseStore]. */
@Singleton
class ReverseService(
    private val store: ReverseStore,
    private val renderer: ReverseRenderer,
) : ReversePreparer {
    @Inject
    constructor(@ApplicationContext context: Context) : this(ReverseStore(context), ReverseRenderer(context))

    private val lock = Mutex()
    private val _progress = MutableStateFlow<Map<String, Int>>(emptyMap())
    override val progress: StateFlow<Map<String, Int>> = _progress

    override fun hasRoom(startMs: Long, endMs: Long) = store.hasRoomFor(startMs, endMs)

    override suspend fun prepare(clipId: String, media: MediaItem, startMs: Long, endMs: Long): Boolean {
        if (store.find(media, startMs, endMs) != null) return true
        if (media.kind != MediaKind.Video) return false
        return lock.withLock {
            if (store.find(media, startMs, endMs) != null) return@withLock true
            val target = store.newFile(media, startMs, endMs)
            _progress.update { it + (clipId to 0) }
            try {
                val ok = renderer.render(media.uri, startMs, endMs, media.hasAudio, target) { percent ->
                    _progress.update { it + (clipId to percent) }
                }
                if (!ok) target.delete()
                ok
            } catch (e: CancellationException) {
                target.delete()
                throw e
            } finally {
                _progress.update { it - clipId }
            }
        }
    }

    override fun sources(timeline: Timeline): Map<String, ReversedSource> =
        timeline.videoLane.filter { it.reversed }.mapNotNull { clip ->
            val media = timeline.media[clip.mediaItemId] ?: return@mapNotNull null
            val file = store.find(media, clip.sourceStartMs, clip.sourceEndMs) ?: return@mapNotNull null
            val range = file.fileRange(clip.sourceStartMs, clip.sourceEndMs)
            val uri = Uri.fromFile(file.file).toString()
            clip.id to ReversedSource(uri, range.first, range.last, file.endMs - file.startMs)
        }.toMap()

    override suspend fun ensure(timeline: Timeline): Boolean =
        timeline.videoLane.filter { it.reversed }.all { clip ->
            val media = timeline.media[clip.mediaItemId] ?: return@all false
            prepare(clip.id, media, clip.sourceStartMs, clip.sourceEndMs)
        }

    override fun sweep(needs: List<ReverseNeed>) = store.sweep(needs)

    companion object {
        /** A service for code that is not built by Hilt (tests). */
        fun forContext(context: Context) = ReverseService(context)
    }
}
