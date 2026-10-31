package com.petruvideo.feature.editor

import androidx.media3.common.Player
import com.petruvideo.core.data.repo.LoadedProject
import com.petruvideo.core.data.repo.ProjectFilter
import com.petruvideo.core.data.repo.ProjectRepository
import com.petruvideo.core.data.repo.ProjectSummary
import com.petruvideo.core.media.pressure.PreviewQuality
import com.petruvideo.core.media.preview.MediaAvailabilityChecker
import com.petruvideo.core.media.preview.PreviewEngine
import com.petruvideo.core.media.preview.PreviewState
import com.petruvideo.core.media.preview.ThumbnailRefresher
import com.petruvideo.core.model.AudioClip
import com.petruvideo.core.model.AudioOrigin
import com.petruvideo.core.model.Availability
import com.petruvideo.core.model.Fingerprint
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.Project
import com.petruvideo.core.model.ProjectFormat
import com.petruvideo.core.model.ProjectStatus
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.VideoClip
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf

class FakeRepository(private val projects: Map<String, LoadedProject>) : ProjectRepository {
    val saves = mutableListOf<Timeline>()
    override fun observeProjects(filter: ProjectFilter, query: String): Flow<List<ProjectSummary>> =
        flowOf(emptyList())
    override suspend fun loadProject(id: String) = projects[id]
    override suspend fun createProject(): Project = error("unused")
    override suspend fun saveTimeline(projectId: String, timeline: Timeline) {
        saves += timeline
    }
    override suspend fun duplicateProject(id: String): Project? = null
    override suspend fun deleteProject(id: String) = Unit
    override suspend fun rename(id: String, name: String) = Unit
    override suspend fun setStatus(id: String, status: ProjectStatus) = Unit
    override suspend fun setThumbnail(id: String, ref: String?) = Unit
}

class FakePreviewEngine : PreviewEngine {
    private val _state = MutableStateFlow(PreviewState())
    override val state: StateFlow<PreviewState> = _state
    override val player: Player? = null
    val loads = mutableListOf<Pair<Timeline, PreviewQuality>>()
    val calls = mutableListOf<String>()
    var released = false

    override fun load(timeline: Timeline, quality: PreviewQuality) {
        loads += timeline to quality
        _state.value = _state.value.copy(durationMs = timeline.totalDurationMs)
    }

    override fun play() {
        calls += "play"
        _state.value = _state.value.copy(isPlaying = true, ended = false)
    }

    override fun pause() {
        calls += "pause"
        _state.value = _state.value.copy(isPlaying = false)
    }

    override fun setClipVolumeLive(clipId: String, percent: Int) {
        calls += "volume $clipId $percent"
    }

    override fun seekTo(positionMs: Long) {
        calls += "seek $positionMs"
        _state.value = _state.value.copy(positionMs = positionMs)
    }

    override fun release() {
        released = true
    }

    /** Simulates playback progress reported by the real engine. */
    fun progress(positionMs: Long, ended: Boolean = false) {
        val playing = !ended && _state.value.isPlaying
        _state.value = _state.value.copy(positionMs = positionMs, ended = ended, isPlaying = playing)
    }
}

class FakeAvailability(var missing: Set<String> = emptySet()) : MediaAvailabilityChecker {
    override suspend fun check(items: Collection<MediaItem>): Map<String, Availability> =
        items.associate { it.id to if (it.id in missing) Availability.Missing else Availability.Available }
}

class FakeThumbnails : ThumbnailRefresher {
    val refreshed = mutableListOf<Pair<String, Timeline>>()
    override suspend fun refresh(projectId: String, timeline: Timeline) {
        refreshed += projectId to timeline
    }
}

fun media(
    id: String,
    kind: MediaKind = MediaKind.Video,
    durationMs: Long? = 10_000,
    hasAudio: Boolean = true,
): MediaItem {
    val visual = kind != MediaKind.Audio
    return MediaItem(
        id = id, kind = kind, uri = "content://m/$id", displayName = "$id.mp4", durationMs = durationMs,
        width = if (visual) 1920 else null, height = if (visual) 1080 else null,
        frameRate = 30f, hasAudio = hasAudio, fingerprint = Fingerprint(100, 1, durationMs, "video/mp4"),
    )
}

/** Three clips (4 s with sound, 3 s silent, 5 s with sound) and one detached audio clip: 12 s long. */
fun sampleLoaded(): LoadedProject {
    val items = listOf(media("m1"), media("m2", hasAudio = false), media("m3"), media("d", MediaKind.Audio))
    val timeline = Timeline(
        projectId = "p",
        format = ProjectFormat(1920, 1080, 30),
        media = items.associateBy { it.id },
        videoLane = listOf(
            VideoClip("c1", "m1", 0, 4_000, attachedAudio = true),
            VideoClip("c2", "m2", 0, 3_000, attachedAudio = false),
            VideoClip("c3", "m3", 1_000, 6_000, attachedAudio = true),
        ),
        audioLane = listOf(AudioClip("a1", "d", 0, 2_000, timelineStartMs = 5_000, origin = AudioOrigin.Detached)),
    )
    return LoadedProject(Project("p", "Coast road", 0, 0), timeline)
}
