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
import com.petruvideo.core.model.engine.SetEqualizer
import com.petruvideo.core.model.engine.SetVoiceFx
import com.petruvideo.core.model.engine.ApplyEqPreset
import com.petruvideo.core.model.engine.SetNoiseReduction
import com.petruvideo.core.model.engine.SetFade
import com.petruvideo.core.model.Bass
import com.petruvideo.core.model.Pitch
import com.petruvideo.core.model.Robot
import com.petruvideo.core.model.Echo
import com.petruvideo.core.model.VoiceFx
import com.petruvideo.core.model.Equalizer
import com.petruvideo.core.model.EqPreset
import com.petruvideo.core.model.NoiseStrength
import com.petruvideo.core.model.NoiseReduction
import com.petruvideo.core.model.Fade
import com.petruvideo.core.model.ReverbPreset
import com.petruvideo.core.model.engine.AddAudioClips
import com.petruvideo.core.model.engine.ApplyReverbPreset
import com.petruvideo.core.model.engine.SetReverbSettings
import com.petruvideo.core.model.engine.SetSpeed
import com.petruvideo.core.model.engine.SetVideoMuted
import com.petruvideo.core.model.engine.SetVolume
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

    /** SC-008, SC-005 of features 002 and 003: speed, volume, mute, and reverb leave the sources alone and reload. */
    @Test
    fun speedVolumeMuteAndReverbSurviveAReloadAndLeaveTheSourcesUntouched() = runBlocking<Unit> {
        val video = source("clip.mp4", MediaKind.Video, 10_000)
        val music = source("music.mp3", MediaKind.Audio, 20_000)
        val before = checksums()

        val repo = RoomProjectRepository(db)
        val project = repo.createProject()
        val session = ProjectSession(repo, kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default))
        session.open(project.id)
        session.apply(AddVideoClips(listOf(AddVideoItem(video)), playheadMs = 0))
        session.apply(AddAudioClips(listOf(music), playheadMs = 0))
        val clip = session.timeline.value!!.videoLane.first().id
        val audio = session.timeline.value!!.audioLane.first().id
        session.apply(SetSpeed(clip, 2_000))
        session.apply(SetVolume(clip, 40))
        session.apply(ApplyReverbPreset(clip, ReverbPreset.Hall))
        session.apply(SetVideoMuted(clip, true))
        session.apply(SetSpeed(audio, 500))
        session.apply(SetVolume(audio, 150))
        val custom = session.timeline.value!!.audioLane.first().reverb.copy(enabled = true, mix = 77, damping = 11)
        session.apply(SetReverbSettings(audio, custom))
        session.flush()
        val saved = session.timeline.value!!
        session.close()

        val loaded = repo.loadProject(project.id)!!.timeline
        loaded.videoLane shouldBe saved.videoLane
        loaded.audioLane shouldBe saved.audioLane
        loaded.videoLane.first().speedMilli shouldBe 2_000
        loaded.videoLane.first().muted shouldBe true
        loaded.audioLane.first().reverb.preset shouldBe ReverbPreset.Custom
        checksums() shouldBe before
    }

    /** SC-005, SC-009 of feature 005: fade, noise, equalizer, and voice effects reload exactly and leave sources alone. */
    @Test
    fun audioEffectsSurviveAReloadAndLeaveTheSourcesUntouched() = runBlocking<Unit> {
        val video = source("clip5.mp4", MediaKind.Video, 10_000)
        val music = source("music5.mp3", MediaKind.Audio, 20_000)
        val before = checksums()

        val repo = RoomProjectRepository(db)
        val project = repo.createProject()
        val session = ProjectSession(repo, kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default))
        session.open(project.id)
        session.apply(AddVideoClips(listOf(AddVideoItem(video)), playheadMs = 0))
        session.apply(AddAudioClips(listOf(music), playheadMs = 0))
        val clip = session.timeline.value!!.videoLane.first().id
        val audio = session.timeline.value!!.audioLane.first().id
        session.apply(SetFade(clip, Fade(1_500, 2_500)))
        session.apply(SetNoiseReduction(clip, NoiseReduction(true, NoiseStrength.High)))
        session.apply(ApplyEqPreset(clip, EqPreset.Telephone))
        session.apply(SetVoiceFx(audio, VoiceFx(Echo(true, 320, 4), Robot(true, 35), Pitch(false, -7), Bass(true, 80))))
        session.apply(SetEqualizer(audio, Equalizer(EqPreset.Custom, 3, -2, 9)))
        session.flush()
        val saved = session.timeline.value!!
        session.close()

        val loaded = repo.loadProject(project.id)!!.timeline
        loaded.videoLane shouldBe saved.videoLane
        loaded.audioLane shouldBe saved.audioLane
        loaded.videoLane.first().fx.fade shouldBe Fade(1_500, 2_500)
        loaded.videoLane.first().fx.eq.preset shouldBe EqPreset.Telephone
        loaded.audioLane.first().fx.voice.pitch shouldBe Pitch(false, -7)
        loaded.audioLane.first().fx.eq.preset shouldBe EqPreset.Custom
        checksums() shouldBe before
    }

    /** SC-005, SC-009 of feature 006: text clips reload exactly and leave the sources alone. */
    @Test
    fun textClipsSurviveAReloadAndLeaveTheSourcesUntouched() = runBlocking<Unit> {
        val video = source("clip6.mp4", MediaKind.Video, 10_000)
        val before = checksums()

        val repo = RoomProjectRepository(db)
        val project = repo.createProject()
        val session = ProjectSession(repo, kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default))
        session.open(project.id)
        session.apply(AddVideoClips(listOf(AddVideoItem(video)), playheadMs = 0))
        session.apply(com.petruvideo.core.model.engine.AddText(1_000, { "t1" }))
        session.apply(com.petruvideo.core.model.engine.SetTextContent("t1", "Line one\nLine two \u00e9\ud83d\ude00"))
        session.apply(
            com.petruvideo.core.model.engine.SetTextStyle(
                "t1",
                com.petruvideo.core.model.TextStyle(
                    font = com.petruvideo.core.model.TextFont.Serif,
                    colorArgb = 0xFFFFCC00.toInt(),
                    align = com.petruvideo.core.model.TextAlign.Right,
                    background = com.petruvideo.core.model.TextBackground(true, 0xFF112233.toInt(), 35),
                    outline = com.petruvideo.core.model.TextOutline(true, 0xFF000000.toInt(), 20),
                ),
            ),
        )
        session.apply(com.petruvideo.core.model.engine.SetTextPlacement("t1", 250, 800, 90))
        session.apply(
            com.petruvideo.core.model.engine.SetTextAnimation(
                "t1",
                com.petruvideo.core.model.TextAnimation(com.petruvideo.core.model.AnimKind.SlideUp, 700),
                com.petruvideo.core.model.TextAnimation(com.petruvideo.core.model.AnimKind.Pop, 300),
            ),
        )
        session.flush()
        val saved = session.timeline.value!!
        session.close()

        val loaded = repo.loadProject(project.id)!!.timeline
        loaded.textLane shouldBe saved.textLane
        loaded.textLane.single().text shouldBe "Line one\nLine two \u00e9\ud83d\ude00"
        loaded.textLane.single().xPermille shouldBe 250
        checksums() shouldBe before
    }

    /** SC-005, SC-008 of feature 007: filters reload exactly and leave the sources alone. */
    @Test
    fun filtersSurviveAReloadAndLeaveTheSourcesUntouched() = runBlocking<Unit> {
        val video = source("clip7.mp4", MediaKind.Video, 10_000)
        val before = checksums()

        val repo = RoomProjectRepository(db)
        val project = repo.createProject()
        val session = ProjectSession(repo, kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default))
        session.open(project.id)
        session.apply(AddVideoClips(listOf(AddVideoItem(video)), playheadMs = 0))
        val clip = session.timeline.value!!.videoLane.first().id
        session.apply(com.petruvideo.core.model.engine.SetFilterLook(clip, com.petruvideo.core.model.FilterLook.Vintage))
        session.apply(com.petruvideo.core.model.engine.SetFilterIntensity(clip, 65))
        session.apply(
            com.petruvideo.core.model.engine.SetFilterAdjustments(
                clip, com.petruvideo.core.model.ClipFilter(brightness = 12, contrast = -30, saturation = 44, warmth = -9, vignette = 70),
            ),
        )
        session.flush()
        val saved = session.timeline.value!!
        session.close()

        val loaded = repo.loadProject(project.id)!!.timeline
        loaded.videoLane shouldBe saved.videoLane
        loaded.videoLane.first().filter.vignette shouldBe 70
        loaded.videoLane.first().filter.look shouldBe com.petruvideo.core.model.FilterLook.Vintage
        checksums() shouldBe before
    }

    /** SC-006 of feature 011: transitions and effects reload exactly and leave the sources alone. */
    @Test
    fun transitionsAndEffectsSurviveAReloadAndLeaveTheSourcesUntouched() = runBlocking<Unit> {
        val video = source("clip11.mp4", MediaKind.Video, 10_000)
        val before = checksums()

        val repo = RoomProjectRepository(db)
        val project = repo.createProject()
        val session = ProjectSession(repo, kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default))
        session.open(project.id)
        session.apply(AddVideoClips(listOf(AddVideoItem(video), AddVideoItem(video)), playheadMs = 0))
        val (first, second) = session.timeline.value!!.videoLane.map { it.id }
        session.apply(
            com.petruvideo.core.model.engine.SetTransition(first, com.petruvideo.core.model.TransitionKind.Slide, 1_500),
        )
        session.apply(com.petruvideo.core.model.engine.SetClipEffect(second, com.petruvideo.core.model.EffectKind.Blur, 35))
        session.flush()
        val saved = session.timeline.value!!
        session.close()

        val loaded = repo.loadProject(project.id)!!.timeline
        loaded.videoLane shouldBe saved.videoLane
        loaded.videoLane[0].transitionOut shouldBe
            com.petruvideo.core.model.Transition(com.petruvideo.core.model.TransitionKind.Slide, 1_500)
        loaded.videoLane[1].effect shouldBe com.petruvideo.core.model.ClipEffect(com.petruvideo.core.model.EffectKind.Blur, 35)
        checksums() shouldBe before
    }

    /** SC-004 of feature 012: keyframes of overlay and text clips reload exactly and leave the sources alone. */
    @Test
    fun keyframesSurviveAReloadAndLeaveTheSourcesUntouched() = runBlocking<Unit> {
        val video = source("clip12.mp4", MediaKind.Video, 10_000)
        val pip = source("pip12.mp4", MediaKind.Video, 8_000)
        val before = checksums()

        val repo = RoomProjectRepository(db)
        val project = repo.createProject()
        val session = ProjectSession(repo, kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default))
        session.open(project.id)
        session.apply(AddVideoClips(listOf(AddVideoItem(video)), playheadMs = 0))
        session.apply(com.petruvideo.core.model.engine.AddOverlay(pip, 0, { "ov" }))
        session.apply(com.petruvideo.core.model.engine.AddText(0, { "tx" }))
        val frames = listOf(
            com.petruvideo.core.model.Keyframe(0, com.petruvideo.core.model.Pose(100, 200, 300, 100)),
            com.petruvideo.core.model.Keyframe(1_500, com.petruvideo.core.model.Pose(800, 700, 400, 40), ease = true),
        )
        val textFrames = listOf(
            com.petruvideo.core.model.Keyframe(0, com.petruvideo.core.model.Pose(500, 500, 60, 100)),
            com.petruvideo.core.model.Keyframe(2_000, com.petruvideo.core.model.Pose(400, 300, 120, 100)),
        )
        val overlay = session.timeline.value!!.overlayLane.single().withKeyframes(frames)
        val text = session.timeline.value!!.textLane.single().withKeyframes(textFrames)
        session.apply(
            com.petruvideo.core.model.engine.Command {
                com.petruvideo.core.model.engine.CommandResult.Applied(
                    it.copy(overlayLane = listOf(overlay), textLane = listOf(text)), "Keyframes",
                )
            },
        )
        session.flush()
        val saved = session.timeline.value!!
        session.close()

        val loaded = repo.loadProject(project.id)!!.timeline
        loaded.overlayLane shouldBe saved.overlayLane
        loaded.textLane shouldBe saved.textLane
        loaded.overlayLane.single().keyframes shouldBe frames
        loaded.textLane.single().keyframes shouldBe textFrames
        checksums() shouldBe before
    }

    /** SC-004 of feature 014: video clip keyframes reload exactly, also in a duplicated project. */
    @Test
    fun videoKeyframesSurviveAReloadAndAProjectDuplicate() = runBlocking<Unit> {
        val video = source("clip14.mp4", MediaKind.Video, 10_000)
        val before = checksums()
        val repo = RoomProjectRepository(db)
        val project = repo.createProject()
        val session = ProjectSession(repo, kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default))
        session.open(project.id)
        session.apply(AddVideoClips(listOf(AddVideoItem(video)), playheadMs = 0))
        val id = session.timeline.value!!.videoLane.single().id
        session.apply(com.petruvideo.core.model.engine.SetPoseAt(id, 0, com.petruvideo.core.model.FRAMING_NEUTRAL))
        session.apply(
            com.petruvideo.core.model.engine.SetPoseAt(id, 3_000, com.petruvideo.core.model.Pose(700, 400, 2_000, 60)),
        )
        session.flush()
        val saved = session.timeline.value!!.videoLane.single().keyframes
        session.close()

        saved.size shouldBe 2
        repo.loadProject(project.id)!!.timeline.videoLane.single().keyframes shouldBe saved
        val copy = repo.duplicateProject(project.id)!!
        repo.loadProject(copy.id)!!.timeline.videoLane.single().keyframes shouldBe saved
        checksums() shouldBe before
    }

    /** SC-005 of feature 013: a voice-over clip reloads and a deleted project reports the files still in use. */
    @Test
    fun voiceOversSurviveAReloadAndDeletingAProjectReportsTheFilesStillUsed() = runBlocking<Unit> {
        val take = source("take13.aac", MediaKind.Audio, 4_000)
        val reported = mutableListOf<Set<String>>()
        val repo = RoomProjectRepository(db, onDeleted = { reported += it })
        val project = repo.createProject()
        val session = ProjectSession(repo, kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default))
        session.open(project.id)
        session.apply(com.petruvideo.core.model.engine.AddVoiceOver(take, 1_500, { "vo" }))
        session.flush()
        val saved = session.timeline.value!!
        session.close()

        val loaded = repo.loadProject(project.id)!!.timeline
        loaded.audioLane shouldBe saved.audioLane
        loaded.audioLane.single().timelineStartMs shouldBe 1_500
        val copy = repo.duplicateProject(project.id)!!
        repo.deleteProject(project.id)
        reported.single().contains(take.uri) shouldBe true
        repo.loadProject(copy.id)!!.timeline.audioLane.single().lengthMs shouldBe 4_000
        repo.deleteProject(copy.id)
        reported.last().contains(take.uri) shouldBe false
    }

    /** SC-005, SC-007 of feature 010: overlays reload exactly with their media and leave the sources alone. */
    @Test
    fun overlaysSurviveAReloadAndLeaveTheSourcesUntouched() = runBlocking<Unit> {
        val video = source("clip10.mp4", MediaKind.Video, 10_000)
        val pip = source("pip10.mp4", MediaKind.Video, 8_000)
        val before = checksums()

        val repo = RoomProjectRepository(db)
        val project = repo.createProject()
        val session = ProjectSession(repo, kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default))
        session.open(project.id)
        session.apply(AddVideoClips(listOf(AddVideoItem(video)), playheadMs = 0))
        session.apply(com.petruvideo.core.model.engine.AddOverlay(pip, 1_500, { "ov1" }))
        session.apply(com.petruvideo.core.model.engine.SetOverlayPlacement("ov1", 210, 640, 420))
        session.apply(com.petruvideo.core.model.engine.SetOverlayOpacity("ov1", 55))
        session.apply(com.petruvideo.core.model.engine.TrimOverlay("ov1", 2_000, 7_000))
        session.flush()
        val saved = session.timeline.value!!
        session.close()

        val loaded = repo.loadProject(project.id)!!.timeline
        loaded.overlayLane shouldBe saved.overlayLane
        loaded.overlayLane.single().opacityPercent shouldBe 55
        loaded.overlayLane.single().durationMs shouldBe 5_000
        loaded.media.containsKey(pip.id) shouldBe true
        checksums() shouldBe before
    }
}
