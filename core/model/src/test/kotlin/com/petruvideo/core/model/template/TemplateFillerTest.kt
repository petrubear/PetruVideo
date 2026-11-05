package com.petruvideo.core.model.template

import com.petruvideo.core.model.Availability
import com.petruvideo.core.model.FilterLook
import com.petruvideo.core.model.Fingerprint
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.engine.TimelineInvariants
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.Test

class TemplateFillerTest {
    private var n = 0
    private fun id() = "id${n++}"

    private fun video(name: String, ms: Long = 10_000, audio: Boolean = true) = MediaItem(
        name, MediaKind.Video, "content://$name", "$name.mp4", ms, 1920, 1080, 30f, audio,
        Fingerprint(1, 1, ms, "video/mp4"),
    )

    private fun photo(name: String) = MediaItem(
        name, MediaKind.Photo, "content://$name", "$name.jpg", null, 800, 600, null, false,
        Fingerprint(1, 1, null, "image/jpeg"),
    )

    private val memories = TemplateCatalog.all.first { it.id == "memories" }

    @Test
    fun `there are six templates with valid slots`() {
        TemplateCatalog.all shouldHaveSize 6
        TemplateCatalog.all.all { it.slotCount in 3..6 && it.lengthMs > 0 } shouldBe true
        TemplateCatalog.all.map { it.id }.toSet() shouldHaveSize 6
    }

    @Test
    fun `videos are cut to the slot, photos take it, and the look title and transitions are applied`() {
        val fill = TemplateFiller.fill(
            memories,
            listOf(video("a"), photo("b"), video("c"), video("d")),
            "p",
            titleText = "Memories",
            newId = ::id,
        )!!
        val lane = fill.timeline.videoLane
        lane.map { it.lengthMs } shouldBe listOf(3_000L, 3_000L, 3_000L, 3_000L)
        lane.all { it.filter.look == FilterLook.Faded && it.filter.intensity == 70 } shouldBe true
        lane.map { it.transitionOut != null } shouldBe listOf(true, true, true, false)
        fill.timeline.videoLengthMs shouldBe 12_000 - 3 * 800
        fill.timeline.textLane.single().text shouldBe "Memories"
        fill.unusedItems shouldBe 0
        fill.timeline.format?.width shouldBe 1920
        TimelineInvariants.violations(fill.timeline) shouldBe emptyList()
    }

    @Test
    fun `a short video shortens its slot, fewer items drop the unused slots, extra items are counted`() {
        val few = TemplateFiller.fill(memories, listOf(video("a", 1_500), video("b")), "p", newId = ::id)!!
        few.timeline.videoLane.map { it.lengthMs } shouldBe listOf(1_500L, 3_000L, 3_000L, 3_000L)
        few.timeline.videoLane[0].transitionOut?.lengthMs shouldBe 700
        val many = TemplateFiller.fill(memories, List(7) { video("v$it") }, "p", newId = ::id)!!
        many.timeline.videoLane shouldHaveSize 4
        many.unusedItems shouldBe 3
    }

    @Test
    fun `audio, missing files and no items give nothing`() {
        val audio = video("s").copy(kind = MediaKind.Audio)
        TemplateFiller.fill(memories, emptyList(), "p", newId = ::id) shouldBe null
        TemplateFiller.fill(memories, listOf(audio), "p", newId = ::id) shouldBe null
        val missing = video("m").copy(availability = Availability.Missing)
        TemplateFiller.fill(memories, listOf(missing), "p", newId = ::id) shouldBe null
    }

    @Test
    fun `a single item has no transition and a template without one has none`() {
        val one = TemplateFiller.fill(memories, listOf(video("a", 3_000)), "p", newId = ::id)!!
        one.timeline.videoLane.single().transitionOut shouldBe null
        val quickCuts = TemplateCatalog.all.first { it.id == "quick-cuts" }
        val quick = TemplateFiller.fill(quickCuts, listOf(video("a"), video("b")), "p", newId = ::id)!!
        quick.timeline.videoLane.all { it.transitionOut == null } shouldBe true
    }

    @Test
    fun `one long video fills every slot with the next part of it`() {
        val quickCuts = TemplateCatalog.all.first { it.id == "quick-cuts" }
        val fill = TemplateFiller.fill(quickCuts, listOf(video("a", 10_000)), "p", newId = ::id)!!
        val lane = fill.timeline.videoLane
        lane shouldHaveSize 6
        lane.map { it.sourceStartMs } shouldBe listOf(0L, 1_500L, 3_000L, 4_500L, 6_000L, 7_500L)
        lane.map { it.lengthMs } shouldBe List(6) { 1_500L }
        TimelineInvariants.violations(fill.timeline) shouldBe emptyList()
    }

    @Test
    fun `two videos alternate and run out in their own time`() {
        val quickCuts = TemplateCatalog.all.first { it.id == "quick-cuts" }
        val fill = TemplateFiller.fill(quickCuts, listOf(video("a", 3_000), video("b", 10_000)), "p", newId = ::id)!!
        fill.timeline.videoLane.map { it.mediaItemId } shouldBe listOf("a", "b", "a", "b", "b", "b")
        fill.timeline.videoLane.map { it.sourceStartMs } shouldBe listOf(0L, 0L, 1_500L, 1_500L, 3_000L, 4_500L)
    }

    @Test
    fun `files that cannot be used are not counted as left over`() {
        val audio = video("s").copy(kind = MediaKind.Audio)
        val missing = video("m").copy(availability = Availability.Missing)
        val fill = TemplateFiller.fill(memories, List(4) { video("v$it") } + audio + missing, "p", newId = ::id)!!
        fill.unusedItems shouldBe 0
        TemplateFiller.fill(memories, List(6) { video("w$it") } + audio, "p", newId = ::id)!!.unusedItems shouldBe 2
    }
}
