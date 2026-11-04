package com.petruvideo.core.data.repo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.petruvideo.core.data.db.PetruDatabase
import com.petruvideo.core.model.AudioClip
import com.petruvideo.core.model.AudioOrigin
import com.petruvideo.core.model.Availability
import com.petruvideo.core.model.Fingerprint
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.ProjectFormat
import com.petruvideo.core.model.ProjectStatus
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.VideoClip
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

class RoomProjectRepositoryTest {
    private lateinit var db: PetruDatabase
    private lateinit var repo: RoomProjectRepository
    private var now = 1_000L
    private var nextId = 0

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PetruDatabase::class.java).build()
        repo = RoomProjectRepository(db, clock = { now }, newId = { "id-${nextId++}" })
    }

    @After
    fun tearDown() = db.close()

    private fun video(id: String) = MediaItem(
        id = id, kind = MediaKind.Video, uri = "content://x/$id", displayName = "$id.mp4",
        durationMs = 10_000, width = 1920, height = 1080, frameRate = 30f, hasAudio = true,
        fingerprint = Fingerprint(100, 1, 10_000, "video/mp4"), availability = Availability.Available,
    )

    private fun audio(id: String) = video(id).copy(kind = MediaKind.Audio, width = null, height = null, frameRate = null)

    private fun timeline(projectId: String) = Timeline(
        projectId = projectId,
        format = ProjectFormat(1920, 1080, 30),
        media = mapOf("m1" to video("m1"), "m2" to video("m2"), "unused" to video("unused"), "a1" to audio("a1")),
        videoLane = listOf(VideoClip("c1", "m1", 0, 4_000, true), VideoClip("c2", "m2", 1_000, 3_000, false)),
        audioLane = listOf(AudioClip("ac1", "a1", 0, 2_000, 5_000, false, AudioOrigin.Added)),
        playheadMs = 2_500,
    )

    @Test
    fun createdProjectsGetDefaultNamesAndAppear() = runBlocking<Unit> {
        val first = repo.createProject()
        now += 10
        val second = repo.createProject()

        first.name shouldBe "Untitled 1"
        second.name shouldBe "Untitled 2"
        repo.observeProjects().first().map { it.project.id } shouldContainExactly listOf(second.id, first.id)
    }

    @Test
    fun projectsAreOrderedByModifiedAtDescending() = runBlocking<Unit> {
        val a = repo.createProject()
        now += 10
        val b = repo.createProject()
        now += 10
        repo.saveTimeline(a.id, Timeline(a.id))

        repo.observeProjects().first().map { it.project.id } shouldContainExactly listOf(a.id, b.id)
    }

    @Test
    fun filtersSplitDraftsFromExported() = runBlocking<Unit> {
        val a = repo.createProject()
        val b = repo.createProject()
        repo.setStatus(b.id, ProjectStatus.Exported)

        repo.observeProjects(ProjectFilter.Drafts).first().map { it.project.id } shouldContainExactly listOf(a.id)
        repo.observeProjects(ProjectFilter.Exported).first().map { it.project.id } shouldContainExactly listOf(b.id)
        repo.observeProjects(ProjectFilter.All).first().size shouldBe 2
    }

    @Test
    fun queryMatchesNamePartially() = runBlocking<Unit> {
        val a = repo.createProject()
        val b = repo.createProject()
        repo.rename(a.id, "Coast road")
        repo.rename(b.id, "Dive reel")

        repo.observeProjects(query = "road").first().map { it.project.id } shouldContainExactly listOf(a.id)
        repo.observeProjects(query = "zzz").first() shouldBe emptyList()
    }

    @Test
    fun saveAndLoadRoundTripsTheTimelineAndKeepsOnlyReferencedMedia() = runBlocking<Unit> {
        val p = repo.createProject()
        val saved = timeline(p.id)

        repo.saveTimeline(p.id, saved)
        val loaded = repo.loadProject(p.id)!!

        loaded.timeline.videoLane shouldBe saved.videoLane
        loaded.timeline.audioLane shouldBe saved.audioLane
        loaded.timeline.playheadMs shouldBe 2_500
        loaded.timeline.format shouldBe saved.format
        loaded.project.format shouldBe saved.format
        loaded.timeline.media.keys shouldBe setOf("m1", "m2", "a1")
    }

    @Test
    fun summariesReportClipCountAndDuration() = runBlocking<Unit> {
        val p = repo.createProject()
        repo.saveTimeline(p.id, timeline(p.id))

        val summary = repo.observeProjects().first().single()
        summary.clipCount shouldBe 2
        summary.durationMs shouldBe 7_000L // audio clip ends at 7 s, later than the 6 s of video
        summary.playheadMs shouldBe 2_500
    }

    @Test
    fun duplicateIsADeepCopyWithNewIds() = runBlocking<Unit> {
        val p = repo.createProject()
        repo.saveTimeline(p.id, timeline(p.id))

        val copy = repo.duplicateProject(p.id)!!
        val original = repo.loadProject(p.id)!!
        val duplicate = repo.loadProject(copy.id)!!

        copy.id shouldNotBe p.id
        duplicate.timeline.videoLane.map { it.id }.intersect(original.timeline.videoLane.map { it.id }.toSet()) shouldBe emptySet()
        duplicate.timeline.media.keys.intersect(original.timeline.media.keys) shouldBe emptySet()
        duplicate.timeline.media.values.map { it.uri }.toSet() shouldBe original.timeline.media.values.map { it.uri }.toSet()
        duplicate.timeline.videoLane.map { it.sourceEndMs } shouldBe original.timeline.videoLane.map { it.sourceEndMs }
        duplicate.timeline.playheadMs shouldBe 2_500
        duplicate.project.status shouldBe ProjectStatus.Draft
    }

    @Test
    fun deleteRemovesTheProjectPictureFile() = runBlocking<Unit> {
        val p = repo.createProject()
        val picture = File.createTempFile("thumb", ".png").apply { writeText("x") }
        repo.setThumbnail(p.id, picture.path)

        repo.deleteProject(p.id)

        picture.exists() shouldBe false
    }

    @Test
    fun aDuplicateDoesNotShareThePictureOfTheOriginal() = runBlocking<Unit> {
        val p = repo.createProject()
        repo.saveTimeline(p.id, timeline(p.id))
        val picture = File.createTempFile("thumb", ".png").apply { writeText("x") }
        repo.setThumbnail(p.id, picture.path)

        val copy = repo.duplicateProject(p.id)!!

        repo.loadProject(copy.id)!!.project.thumbnailRef shouldBe null
        repo.deleteProject(copy.id)
        picture.exists() shouldBe true
        picture.delete()
    }

    @Test
    fun deleteRemovesRowsOfThatProjectOnly() = runBlocking<Unit> {
        val a = repo.createProject()
        val b = repo.createProject()
        repo.saveTimeline(a.id, timeline(a.id))
        repo.saveTimeline(b.id, timeline(b.id).copy(media = timeline(b.id).media))

        repo.deleteProject(a.id)

        repo.loadProject(a.id) shouldBe null
        db.timelineDao().mediaItems(a.id) shouldBe emptyList()
        db.timelineDao().videoClips(a.id) shouldBe emptyList()
        repo.loadProject(b.id)!!.timeline.videoLane.size shouldBe 2
    }

    @Test
    fun renameTrimsAndLimitsLength() = runBlocking<Unit> {
        val p = repo.createProject()
        repo.rename(p.id, "  " + "x".repeat(120) + "  ")
        repo.loadProject(p.id)!!.project.name.length shouldBe 80
    }
}
