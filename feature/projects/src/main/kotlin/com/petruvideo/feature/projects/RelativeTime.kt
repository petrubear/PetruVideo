package com.petruvideo.feature.projects

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** "Just now", "2 min ago", "3 h ago", "Yesterday", then "Oct 24". */
object RelativeTime {
    private const val MINUTE_MS = 60_000L
    private const val HOUR_MS = 3_600_000L

    fun format(
        thenMs: Long,
        nowMs: Long,
        zone: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault(),
    ): String {
        val elapsed = nowMs - thenMs
        val then = Instant.ofEpochMilli(thenMs).atZone(zone).toLocalDate()
        val today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
        return when {
            elapsed < MINUTE_MS -> "Just now"
            then == today && elapsed < HOUR_MS -> "${elapsed / MINUTE_MS} min ago"
            then == today -> "${elapsed / HOUR_MS} h ago"
            then == today.minusDays(1) -> "Yesterday"
            else -> DateTimeFormatter.ofPattern("MMM d", locale).format(then)
        }
    }
}
