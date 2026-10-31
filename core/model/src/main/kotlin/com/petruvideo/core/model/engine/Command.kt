package com.petruvideo.core.model.engine

import com.petruvideo.core.model.Timeline

enum class RejectReason { OutOfRange, BelowMinimumLength, NoAttachedAudio, NotFound, MissingMedia }

enum class Notice { Snapped, LengthChanged, SpeedLimited }

sealed interface CommandResult {
    data class Applied(
        val timeline: Timeline,
        val label: String,
        val notices: List<Notice> = emptyList(),
    ) : CommandResult

    data class Rejected(val reason: RejectReason) : CommandResult
}

/** A pure edit: the same input timeline always produces the same result. */
fun interface Command {
    fun apply(timeline: Timeline): CommandResult
}
