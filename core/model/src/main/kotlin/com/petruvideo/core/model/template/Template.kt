package com.petruvideo.core.model.template

import com.petruvideo.core.model.AnimKind
import com.petruvideo.core.model.Availability
import com.petruvideo.core.model.ClipFilter
import com.petruvideo.core.model.FilterLook
import com.petruvideo.core.model.MIN_CLIP_MS
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.TRANSITION_MIN_MS
import com.petruvideo.core.model.TextAnimation
import com.petruvideo.core.model.TextClip
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.Transition
import com.petruvideo.core.model.TransitionKind
import com.petruvideo.core.model.VideoClip
import com.petruvideo.core.model.engine.deriveFormat
import com.petruvideo.core.model.transitionLimitMs

/** A title shown over the start of a template project. */
data class TemplateTitle(val text: String, val durationMs: Long = 2_000)

/** A built-in ready-made start: ordered slot lengths, one filter look, a title, and the transition between slots. */
data class Template(
    val id: String,
    val name: String,
    val summary: String,
    val slotsMs: List<Long>,
    val look: FilterLook = FilterLook.None,
    val lookIntensity: Int = 100,
    val title: TemplateTitle? = null,
    val transition: Transition? = null,
) {
    val slotCount: Int get() = slotsMs.size
    val lengthMs: Long
        get() = slotsMs.sum() - slotsMs.indices.drop(1).sumOf { transition?.lengthMs ?: 0L }.coerceAtLeast(0)
}

/** The six templates that ship with the app. */
object TemplateCatalog {
    val all: List<Template> = listOf(
        Template(
            "quick-cuts", "Quick Cuts", "Fast cuts with a vivid look", List(6) { 1_500L },
            FilterLook.Vivid, 80, TemplateTitle("Quick Cuts"),
        ),
        Template(
            "memories", "Memories", "Soft dissolves and a faded look", List(4) { 3_000L },
            FilterLook.Faded, 70, TemplateTitle("Memories", 2_500), Transition(TransitionKind.Dissolve, 800),
        ),
        Template(
            "travel", "Travel", "Warm color and sliding cuts", List(5) { 2_500L },
            FilterLook.Warm, 80, TemplateTitle("Travel"), Transition(TransitionKind.Slide, 500),
        ),
        Template(
            "story", "Black and White Story", "Fades through black", List(4) { 3_000L },
            FilterLook.BlackAndWhite, 100, TemplateTitle("Our Story"), Transition(TransitionKind.FadeBlack, 800),
        ),
        Template(
            "pop", "Pop", "Short bright clips with zooms", List(6) { 1_000L },
            FilterLook.Vivid, 100, TemplateTitle("Pop"), Transition(TransitionKind.Zoom, 300),
        ),
        Template(
            "slow-fade", "Slow Fade", "Long vintage scenes", List(3) { 4_000L },
            FilterLook.Vintage, 80, TemplateTitle("Slow Fade", 3_000), Transition(TransitionKind.Dissolve, 1_200),
        ),
    )
}

/** What filling a template made, and how many chosen items did not fit. */
data class TemplateFill(val timeline: Timeline, val unusedItems: Int)

object TemplateFiller {
    /**
     * Places [items] in order into the template's slots (spec FR-014): a video keeps its start and is cut to the slot,
     * or to its own length when shorter; a photo takes the slot length; items beyond the slots are not used. When there
     * are fewer items than slots, the slots left over continue through the footage that is still unused, one video
     * after another, so a single long video fills the whole template; slots with nothing left are dropped. Returns
     * null when there is nothing usable (no items, audio only, or missing files).
     */
    fun fill(template: Template, items: List<MediaItem>, projectId: String, newId: () -> String): TemplateFill? {
        val usable = items.filter { it.kind != MediaKind.Audio && it.availability == Availability.Available }
        val chosen = usable.take(template.slotCount)
        val cursors = mutableMapOf<String, Long>()
        val uses = mutableMapOf<String, Int>()
        val placed = mutableListOf<Pair<MediaItem, VideoClip>>()
        for (slotMs in template.slotsMs) {
            val next = nextPiece(chosen, cursors, uses, slotMs, newId) ?: continue
            placed += next
        }
        if (placed.isEmpty()) return null
        val clips = placed.map { it.second }
        val withEffects = clips.mapIndexed { i, clip ->
            val next = clips.getOrNull(i + 1)
            clip.copy(
                filter = ClipFilter(template.look, template.lookIntensity),
                transitionOut = template.transition?.let { transitionFor(it, clip, next) }.takeIf { next != null },
            )
        }
        val used = placed.map { it.first }.associateBy { it.id }
        val base = Timeline(projectId, deriveFormat(placed.first().first), used, withEffects)
        val title = template.title?.let { titleClip(it, base.videoLengthMs, newId) }
        return TemplateFill(base.copy(textLane = listOfNotNull(title)), usable.size - chosen.size)
    }

    /** The next slot's clip: the item used least so far that still has footage, from where its last piece ended. */
    private fun nextPiece(
        chosen: List<MediaItem>,
        cursors: MutableMap<String, Long>,
        uses: MutableMap<String, Int>,
        slotMs: Long,
        newId: () -> String,
    ): Pair<MediaItem, VideoClip>? {
        for (media in chosen.sortedBy { uses[it.id] ?: 0 }) {
            val from = cursors[media.id] ?: 0L
            val clip = clipFor(media, from, slotMs, newId) ?: continue
            cursors[media.id] = clip.sourceEndMs
            uses[media.id] = (uses[media.id] ?: 0) + 1
            return media to clip
        }
        return null
    }

    private fun clipFor(media: MediaItem, fromMs: Long, slotMs: Long, newId: () -> String): VideoClip? {
        if (media.kind == MediaKind.Photo) {
            return if (fromMs > 0) null else VideoClip(newId(), media.id, 0, slotMs, attachedAudio = false)
        }
        val left = (media.durationMs ?: return null) - fromMs
        val length = minOf(slotMs, left)
        if (length < MIN_CLIP_MS) return null
        return VideoClip(newId(), media.id, fromMs, fromMs + length, attachedAudio = media.hasAudio)
    }

    /** The template's transition, shortened to what the two clips allow, or none when they are too short. */
    private fun transitionFor(wanted: Transition, clip: VideoClip, next: VideoClip?): Transition? {
        val limit = transitionLimitMs(clip.lengthMs, next?.lengthMs ?: 0L)
        val length = minOf(wanted.lengthMs, limit)
        return if (length < TRANSITION_MIN_MS) null else wanted.copy(lengthMs = length / 100 * 100)
    }

    private fun titleClip(title: TemplateTitle, projectMs: Long, newId: () -> String) = TextClip(
        newId(), title.text, 0, minOf(title.durationMs, projectMs).coerceAtLeast(MIN_CLIP_MS),
        animIn = TextAnimation(AnimKind.Fade), animOut = TextAnimation(AnimKind.Fade),
    )
}
