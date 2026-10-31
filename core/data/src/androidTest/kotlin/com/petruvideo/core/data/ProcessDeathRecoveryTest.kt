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
import com.petruvideo.core.model.engine.AddVideoClips
import com.petruvideo.core.model.engine.AddVideoItem
import com.petruvideo.core.model.engine.TrimEnd
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * SC-005: if the process dies, at most 5 seconds of work is lost. Autosave waits 1.5 s after the last edit, so an
 * edit that is older than that must survive the process being killed without any flush.
 */
class ProcessDeathRecoveryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val dbName = "process-death-${System.nanoTime()}.db"

    @Before
    fun setUp() {
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    private fun open() = Room.databaseBuilder(context, PetruDatabase::class.java, dbName).build()

    private fun video() = MediaItem(
        "m1", MediaKind.Video, "content://x/m1", "m1.mp4", 10_000, 1920, 1080, 30f, true,
        Fingerprint(100, 1, 10_000, "video/mp4"), Availability.Available,
    )

    @Test
    fun editsOlderThanTheAutosaveDelayAreThereAfterTheProcessIsKilled() = runBlocking<Unit> {
        val firstRun = open()
        val repo = RoomProjectRepository(firstRun)
        val projectId = repo.createProject().id
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val session = ProjectSession(repo, scope)
        session.open(projectId)
        session.apply(AddVideoClips(listOf(AddVideoItem(video())), playheadMs = 0))
        val clip = session.timeline.value!!.videoLane.single().id
        session.apply(TrimEnd(clip, 6_000))

        delay(AUTOSAVE_WAIT_MS) // longer than the debounce, then "kill" without flushing
        scope.cancel()
        firstRun.close()

        val secondRun = open()
        val loaded = RoomProjectRepository(secondRun).loadProject(projectId)!!
        loaded.timeline.videoLane.single().lengthMs shouldBe 6_000
        secondRun.close()
    }

    private companion object {
        const val AUTOSAVE_WAIT_MS = 2_500L
    }
}
