package com.petruvideo.feature.projects

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** "Just now", "2 min ago", "3 h ago", "Yesterday", then "Oct 24" (the words follow the phone's language). */
object RelativeTime {
    private const val MINUTE_MS = 60_000L
    private const val HOUR_MS = 3_600_000L

    /** How long ago a time was, before it is turned into words. */
    sealed interface Ago {
        data object JustNow : Ago
        data class Minutes(val count: Int) : Ago
        data class Hours(val count: Int) : Ago
        data object Yesterday : Ago
        data class Date(val text: String) : Ago
    }

    fun ago(
        thenMs: Long,
        nowMs: Long,
        zone: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault(),
    ): Ago {
        val elapsed = nowMs - thenMs
        val then = Instant.ofEpochMilli(thenMs).atZone(zone).toLocalDate()
        val today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
        return when {
            elapsed < MINUTE_MS -> Ago.JustNow
            then == today && elapsed < HOUR_MS -> Ago.Minutes((elapsed / MINUTE_MS).toInt())
            then == today -> Ago.Hours((elapsed / HOUR_MS).toInt())
            then == today.minusDays(1) -> Ago.Yesterday
            else -> Ago.Date(DateTimeFormatter.ofPattern("MMM d", locale).format(then))
        }
    }

    @Composable
    fun format(thenMs: Long, nowMs: Long): String = when (val ago = ago(thenMs, nowMs)) {
        Ago.JustNow -> stringResource(R.string.projects_just_now)
        is Ago.Minutes -> stringResource(R.string.projects_minutes_ago, ago.count)
        is Ago.Hours -> stringResource(R.string.projects_hours_ago, ago.count)
        Ago.Yesterday -> stringResource(R.string.projects_yesterday)
        is Ago.Date -> ago.text
    }
}
