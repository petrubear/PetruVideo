package com.petruvideo.core.model.engine

import com.petruvideo.core.model.TextClip

/** Rows for overlapping text clips: each clip goes to the first row whose last clip has ended. Not stored. */
object TextRows {
    fun assign(clips: List<TextClip>): Map<String, Int> {
        val rowEnds = mutableListOf<Long>()
        val rows = LinkedHashMap<String, Int>()
        for (clip in clips.sortedWith(compareBy({ it.startMs }, { it.id }))) {
            var row = rowEnds.indexOfFirst { it <= clip.startMs }
            if (row < 0) {
                rowEnds += clip.endMs
                row = rowEnds.lastIndex
            } else {
                rowEnds[row] = clip.endMs
            }
            rows[clip.id] = row
        }
        return rows
    }

    fun rowCount(clips: List<TextClip>): Int = (assign(clips).values.maxOrNull() ?: -1) + 1
}
