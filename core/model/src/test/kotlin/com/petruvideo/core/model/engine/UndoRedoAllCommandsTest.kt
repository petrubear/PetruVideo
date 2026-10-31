package com.petruvideo.core.model.engine

import com.petruvideo.core.model.AudioClip
import com.petruvideo.core.model.AudioOrigin
import com.petruvideo.core.model.ProjectFormat
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.VideoClip
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.Test

import kotlin.random.Random

class UndoRedoAllCommandsTest {
    private fun commands(t: Timeline, random: Random): List<Command> = buildList {
        val v = t.videoLane.randomOrNull(random)
        val a = t.audioLane.randomOrNull(random)
        if (v != null) {
            add(TrimStart(v.id, v.sourceStartMs + 10))
            add(TrimEnd(v.id, v.sourceEndMs - 10))
            add(Split(v.id, (t.videoStartOf(v.id) ?: 0) + v.lengthMs / 2))
            add(Duplicate(v.id))
            add(Delete(v.id))
            add(DetachAudio(v.id))
            add(SetSpeed(v.id, random.nextInt(100, 5000)))
            add(SetVolume(v.id, random.nextInt(0, 250)))
            add(MoveVideoClip(v.id, 0))
        }
        if (a != null) {
            add(TrimStart(a.id, a.sourceStartMs + 10))
            add(Split(a.id, a.timelineStartMs + a.lengthMs / 2))
            add(Duplicate(a.id))
            add(Delete(a.id))
            add(MoveAudioClip(a.id, a.timelineStartMs + 7))
            add(SetMuted(a.id, !a.muted))
            add(SetSpeed(a.id, random.nextInt(100, 5000)))
            add(SetVolume(a.id, random.nextInt(0, 250)))
            add(ReplaceAudio(a.id, testAudio("repl", 1_500)))
        }
        add(AddAudioClips(listOf(testAudio("new", 3_000)), 0))
    }

    @Test
    fun `undo of every applied command restores the timeline and redo reapplies it`() {
        repeat(200) { seed ->
            val random = Random(seed)
            val start = randomTimeline(random)
            for (command in commands(start, random)) {
                val history = EditHistory(start)
                val result = history.apply(command)
                if (result !is CommandResult.Applied) {
                    history.current shouldBe start
                    continue
                }
                TimelineInvariants.violations(result.timeline).shouldBeEmpty()
                history.undo()
                history.current shouldBe start
                history.redo()
                history.current shouldBe result.timeline
            }
        }
    }
}
