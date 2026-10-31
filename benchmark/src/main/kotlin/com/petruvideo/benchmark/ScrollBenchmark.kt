package com.petruvideo.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Frame timing while scrolling the Projects grid and the editor timeline. Run with a library of 100+ projects and
 * an open project with 100+ clips on the device; the benchmark opens the first project from the grid.
 */
@RunWith(AndroidJUnit4::class)
class ScrollBenchmark {
    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun scrollProjects() = rule.measureRepeated(
        packageName = PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.DEFAULT,
        iterations = 5,
        startupMode = StartupMode.WARM,
    ) {
        startActivityAndWait()
        val grid = device.findObject(By.scrollable(true)) ?: return@measureRepeated
        grid.setGestureMargin(device.displayWidth / 5)
        repeat(3) { grid.fling(Direction.DOWN) }
        repeat(3) { grid.fling(Direction.UP) }
    }

    @Test
    fun scrollTimeline() = rule.measureRepeated(
        packageName = PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.DEFAULT,
        iterations = 5,
        startupMode = StartupMode.WARM,
        setupBlock = {
            startActivityAndWait()
            device.findObject(By.textContains("Continue"))?.click()
            device.waitForIdle()
        },
    ) {
        val timeline = device.findObject(By.scrollable(true)) ?: return@measureRepeated
        timeline.setGestureMargin(device.displayWidth / 5)
        repeat(3) { timeline.fling(Direction.RIGHT) }
        repeat(3) { timeline.fling(Direction.LEFT) }
    }
}
