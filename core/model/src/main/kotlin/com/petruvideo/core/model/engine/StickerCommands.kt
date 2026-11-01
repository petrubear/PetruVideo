package com.petruvideo.core.model.engine

import com.petruvideo.core.model.STICKER_MS
import com.petruvideo.core.model.STICKER_SIZE
import com.petruvideo.core.model.Sticker
import com.petruvideo.core.model.TEXT_ADD_MIN_MS
import com.petruvideo.core.model.TextClip
import com.petruvideo.core.model.TextStyle
import com.petruvideo.core.model.Timeline
import java.util.UUID

/** Adds [sticker] as a text layer at [atMs], centered and large, for 3 seconds (less if the project ends sooner). */
class AddSticker(
    private val atMs: Long,
    private val sticker: Sticker,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : Command {
    override fun apply(timeline: Timeline): CommandResult {
        val start = atMs.coerceAtLeast(0)
        val projectMs = timeline.totalDurationMs
        val room = if (projectMs <= 0) STICKER_MS else projectMs - start
        val duration = room.coerceIn(TEXT_ADD_MIN_MS, STICKER_MS)
        val style = sticker.color?.let { TextStyle(colorArgb = it) } ?: TextStyle()
        val clip = TextClip(newId(), sticker.glyph, start, duration, sizePermille = STICKER_SIZE, style = style)
        val lane = (timeline.textLane + clip).sortedBy { it.startMs }
        return CommandResult.Applied(timeline.copy(textLane = lane), "Add sticker")
    }
}
