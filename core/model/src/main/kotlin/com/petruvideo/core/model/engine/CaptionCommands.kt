package com.petruvideo.core.model.engine

import com.petruvideo.core.model.ARGB_BLACK
import com.petruvideo.core.model.ARGB_WHITE
import com.petruvideo.core.model.CAPTION_BOX_OPACITY
import com.petruvideo.core.model.CAPTION_MAX
import com.petruvideo.core.model.CAPTION_SIZE
import com.petruvideo.core.model.CAPTION_X
import com.petruvideo.core.model.CAPTION_Y
import com.petruvideo.core.model.CaptionLine
import com.petruvideo.core.model.TEXT_MAX_CHARS
import com.petruvideo.core.model.TEXT_MIN_MS
import com.petruvideo.core.model.TextBackground
import com.petruvideo.core.model.TextClip
import com.petruvideo.core.model.TextStyle
import com.petruvideo.core.model.Timeline
import java.util.UUID

/**
 * Adds one caption text clip for each of [lines] (bottom of the frame, light text on a dark box, marked as auto
 * captions). With [replace], the auto captions already in the project are removed in the same step; typed text
 * never is.
 */
class AddCaptions(
    private val lines: List<CaptionLine>,
    private val replace: Boolean,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : Command {
    override fun apply(timeline: Timeline): CommandResult {
        val style = TextStyle(
            colorArgb = ARGB_WHITE,
            background = TextBackground(true, ARGB_BLACK, CAPTION_BOX_OPACITY),
        )
        val added = lines
            .filter { it.text.isNotBlank() && it.endMs - it.startMs >= TEXT_MIN_MS }
            .take(CAPTION_MAX)
            .map { line ->
                TextClip(
                    newId(), line.text.trim().take(TEXT_MAX_CHARS), line.startMs.coerceAtLeast(0),
                    line.endMs - line.startMs, CAPTION_X, CAPTION_Y, CAPTION_SIZE, style, caption = true,
                )
            }
        val kept = if (replace) timeline.textLane.filterNot { it.caption } else timeline.textLane
        if (added.isEmpty() && kept.size == timeline.textLane.size) {
            return CommandResult.Rejected(RejectReason.OutOfRange)
        }
        return CommandResult.Applied(timeline.copy(textLane = (kept + added).sortedBy { it.startMs }), "Auto captions")
    }
}
