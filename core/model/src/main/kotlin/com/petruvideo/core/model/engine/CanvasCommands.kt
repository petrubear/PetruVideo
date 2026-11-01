package com.petruvideo.core.model.engine

import com.petruvideo.core.model.CanvasBackground
import com.petruvideo.core.model.Timeline

/** Sets what fills the frame behind clips that do not cover it; rejects a choice that is already set. */
class SetCanvas(private val canvas: CanvasBackground) : Command {
    override fun apply(timeline: Timeline): CommandResult {
        if (canvas == timeline.canvas) return CommandResult.Rejected(RejectReason.OutOfRange)
        return CommandResult.Applied(timeline.copy(canvas = canvas), "Background")
    }
}
