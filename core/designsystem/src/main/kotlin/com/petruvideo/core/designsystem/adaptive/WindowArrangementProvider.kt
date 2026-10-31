package com.petruvideo.core.designsystem.adaptive

import android.app.Activity
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import androidx.window.layout.WindowMetricsCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Observes the window size and the fold feature through Jetpack WindowManager and exposes them as state.
 * The activity handles configuration changes itself, so call [onConfigurationChanged] from the activity to
 * re-read the window size after a resize or rotation.
 */
class WindowArrangementProvider(private val activity: Activity, scope: CoroutineScope) {
    private val configTick = MutableStateFlow(0)

    val input: StateFlow<ArrangementInput> = combine(
        WindowInfoTracker.getOrCreate(activity).windowLayoutInfo(activity),
        configTick,
    ) { layoutInfo, _ -> toInput(layoutInfo) }
        .stateIn(scope, SharingStarted.Eagerly, toInput(null))

    fun onConfigurationChanged() {
        configTick.value++
    }

    private fun toInput(layoutInfo: WindowLayoutInfo?): ArrangementInput {
        val bounds = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(activity).bounds
        val density = activity.resources.displayMetrics.density
        val fold = layoutInfo?.displayFeatures?.filterIsInstance<FoldingFeature>()?.firstOrNull()
        return ArrangementInput(
            widthDp = (bounds.width() / density).toInt(),
            heightDp = (bounds.height() / density).toInt(),
            fold = when (fold?.state) {
                null -> FoldState.None
                FoldingFeature.State.HALF_OPENED -> FoldState.HalfOpened
                else -> FoldState.Flat
            },
            foldOrientation = when (fold?.orientation) {
                null -> null
                FoldingFeature.Orientation.HORIZONTAL -> FoldOrientation.Horizontal
                else -> FoldOrientation.Vertical
            },
            hinge = fold?.bounds?.let { HingeBounds(it.left, it.top, it.right, it.bottom) },
            hingeSeparates = fold?.isSeparating == true,
        )
    }
}
