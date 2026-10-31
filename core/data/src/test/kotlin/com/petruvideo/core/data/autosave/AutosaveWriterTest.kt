package com.petruvideo.core.data.autosave

import com.petruvideo.core.model.Timeline
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AutosaveWriterTest {
    private fun timeline(playhead: Long) = Timeline(projectId = "p", playheadMs = playhead)

    private class Recorder {
        val saves = mutableListOf<Pair<String, Timeline>>()
        var concurrent = 0
        var maxConcurrent = 0
        var failNext = false
        var saveTimeMs = 0L

        suspend fun save(projectId: String, timeline: Timeline) {
            concurrent++
            maxConcurrent = maxOf(maxConcurrent, concurrent)
            try {
                if (saveTimeMs > 0) delay(saveTimeMs)
                if (failNext) {
                    failNext = false
                    error("disk full")
                }
                saves += projectId to timeline
            } finally {
                concurrent--
            }
        }
    }

    @Test
    fun `saves once, 1_5 seconds after the last submission`() = runTest {
        val rec = Recorder()
        val writer = AutosaveWriter(this, rec::save)

        writer.submit("p", timeline(1))
        advanceTimeBy(1_000)
        writer.submit("p", timeline(2))
        advanceTimeBy(1_000)
        writer.submit("p", timeline(3))
        advanceTimeBy(1_499)
        rec.saves.size shouldBe 0

        advanceTimeBy(2) // the write is due at exactly 3_500

        rec.saves.size shouldBe 1
        rec.saves.single().second.playheadMs shouldBe 3
    }

    @Test
    fun `status goes saving then saved`() = runTest {
        val rec = Recorder()
        val writer = AutosaveWriter(this, rec::save)
        writer.status.value shouldBe SaveStatus.Saved

        writer.submit("p", timeline(1))
        writer.status.value shouldBe SaveStatus.Saving

        advanceUntilIdle()
        writer.status.value shouldBe SaveStatus.Saved
    }

    @Test
    fun `flush saves immediately without waiting for the debounce`() = runTest {
        val rec = Recorder()
        val writer = AutosaveWriter(this, rec::save)

        writer.submit("p", timeline(7))
        writer.flush()

        rec.saves.single().second.playheadMs shouldBe 7
        currentTime shouldBe 0
        writer.status.value shouldBe SaveStatus.Saved
        advanceUntilIdle()
        rec.saves.size shouldBe 1
    }

    @Test
    fun `flush with nothing pending does nothing`() = runTest {
        val rec = Recorder()
        val writer = AutosaveWriter(this, rec::save)
        writer.flush()
        rec.saves.size shouldBe 0
    }

    @Test
    fun `only one write runs at a time and the newest timeline wins`() = runTest {
        val rec = Recorder().apply { saveTimeMs = 5_000 }
        val writer = AutosaveWriter(this, rec::save)

        writer.submit("p", timeline(1))
        advanceTimeBy(1_600) // first write is now in flight
        writer.submit("p", timeline(2))
        advanceUntilIdle()

        rec.maxConcurrent shouldBe 1
        rec.saves.map { it.second.playheadMs } shouldBe listOf(1L, 2L)
    }

    @Test
    fun `a failed write keeps the pending timeline and retry saves it`() = runTest {
        val rec = Recorder().apply { failNext = true }
        val writer = AutosaveWriter(this, rec::save)

        writer.submit("p", timeline(9))
        advanceUntilIdle()

        writer.status.value.shouldBeInstanceOf<SaveStatus.Failed>()
        rec.saves.size shouldBe 0

        writer.retry()

        rec.saves.single().second.playheadMs shouldBe 9
        writer.status.value shouldBe SaveStatus.Saved
    }

    @Test
    fun `a new submission after a failure saves the newest timeline`() = runTest {
        val rec = Recorder().apply { failNext = true }
        val writer = AutosaveWriter(this, rec::save)

        writer.submit("p", timeline(1))
        advanceUntilIdle()
        writer.submit("p", timeline(2))
        advanceUntilIdle()

        rec.saves.single().second.playheadMs shouldBe 2
        writer.status.value shouldBe SaveStatus.Saved
    }
}
