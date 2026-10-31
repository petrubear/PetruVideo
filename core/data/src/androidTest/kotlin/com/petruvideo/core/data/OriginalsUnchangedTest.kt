package com.petruvideo.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.petruvideo.core.data.db.PetruDatabase
import com.petruvideo.core.data.repo.RoomProjectRepository
import com.petruvideo.core.data.session.ProjectSession
import com.petruvideo.core.model.Availability
import com.petruvideo.core.model.Fingerprint
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.engine.AddAudioClips
import com.petruvideo.core.model.engine.AddVideoClips
import com.petruvideo.core.model.engine.AddVideoItem
import com.petruvideo.core.model.engine.Delete
import com.petruvideo.core.model.engine.DetachAudio
import com.petruvideo.core.model.engine.Duplicate
import com.petruvideo.core.model.engine.MoveAudioClip
import com.petruvideo.core.model.engine.MoveVideoClip
import com.petruvideo.core.model.engine.ReplaceAudio
import com.petruvideo.core.model.engine.SetMuted
import com.petruvideo.core.model.engine.Split
import com.petruvideo.core.model.engine.TrimEnd
import com.petruvideo.core.model.engine.TrimStart
import io.kotest.matchers.shouldBe
import java.io.File
import java.security.MessageDigest
import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

/** SC-007: a full edit session, saved and undone, never changes the user's source files. */
class OriginalsUnchangedTest {
    private lateinit var db: PetruDatabase
    private lateinit var dir: File

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PetruDatabase::class.java).build()
        dir = File(context.cacheDir, "originals-${System.nanoTime()}").apply { mkdirs() }
    }

    @After
    fun tearDown() {
        db.close()
        dir.deleteRecursively()
    }

    private fun source(name: String, kind: MediaKind, durationMs: Long): MediaItem {
        val file = File(dir, name).apply { writeBytes(Random(name.hashCode()).nextBytes(64 * 1024)) }
        return MediaItem(
            id = name, kind = kind, uri = file.toURI().toString(), displayName = name, durationMs = durationMs,
            width = if (kind == MediaKind.Audio) null else 1920, height = if (kind == MediaKind.Audio) null else 1080,
            frameRate = if (kind == MediaKind.Audio) null else 30f, hasAudio = true,
            fingerprint = Fingerprint(file.length(), file.lastModified(), durationMs, "application/octet-stream"),
            availability = Availability.Available,
        )
    }

    private fun checksums(): Map<String, Pair<String, Long>> = dir.listFiles().orEmpty().associate {
        it.name to (MessageDigest.getInstance("SHA-256").digest(it.readBytes()).joinToString("") { b -> "%02x".format(b) } to
            it.lastModified())
    }

    @Test
    fun aFullEditSessionLeavesTheSourceFilesUntouched() = runBlocking<Unit> {
        val video = source("clip.mp4", MediaKind.Video, 10_000)
        val other = source("other.mp4", MediaKind.Video, 8_000)
        val music = source("music.mp3", MediaKind.Audio, 20_000)
        val voice = source("voice.mp3", MediaKind.Audio, 2_000)
        val before = checksums()

        val repo = RoomProjectRepository(db)
        val project = repo.createProject()
        val session = ProjectSession(repo, kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default))
        session.open(project.id)

        session.apply(AddVideoClips(listOf(AddVideoItem(video), AddVideoItem(other)), playheadMs = 0))
        session.apply(AddAudioClips(listOf(music), playheadMs = 0))
        val clip = session.timeline.value!!.videoLane.first().id
        session.apply(TrimStart(clip, 1_000))
        session.apply(TrimEnd(clip, 8_000))
        session.apply(Split(clip, 3_000))
        session.apply(Duplicate(clip))
        session.apply(DetachAudio(session.timeline.value!!.videoLane.last().id))
        session.apply(MoveVideoClip(clip, 1))
        val audio = session.timeline.value!!.audioLane
        session.apply(SetMuted(audio.first().id, true))
        session.apply(MoveAudioClip(audio.first().id, 30_000))
        session.apply(ReplaceAudio(audio.first().id, voice))
        session.apply(Delete(session.timeline.value!!.videoLane.first().id))
        session.flush()
        repeat(6) { session.undo() }
        repeat(3) { session.redo() }
        session.flush()
        session.close()

        checksums() shouldBe before
    }
}
