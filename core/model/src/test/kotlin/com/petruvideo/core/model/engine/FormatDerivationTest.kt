package com.petruvideo.core.model.engine

import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.ProjectFormat
import io.kotest.matchers.shouldBe
import org.junit.Test

class FormatDerivationTest {
    private fun video(w: Int, h: Int, fps: Float?) = testVideo("v").copy(width = w, height = h, frameRate = fps)

    private fun derive(item: MediaItem) = deriveFormat(item)

    @Test
    fun `landscape 1080p keeps its size and frame rate`() {
        derive(video(1920, 1080, 30f)) shouldBe ProjectFormat(1920, 1080, 30)
    }

    @Test
    fun `4K is scaled so the short side is 1080`() {
        derive(video(3840, 2160, 60f)) shouldBe ProjectFormat(1920, 1080, 60)
    }

    @Test
    fun `portrait uses the display size after rotation and keeps its aspect ratio`() {
        derive(video(1080, 1920, 30f)) shouldBe ProjectFormat(1080, 1920, 30)
        derive(video(2160, 3840, 30f)) shouldBe ProjectFormat(1080, 1920, 30)
    }

    @Test
    fun `smaller footage is never upscaled`() {
        derive(video(640, 360, 30f)) shouldBe ProjectFormat(640, 360, 30)
    }

    @Test
    fun `dimensions are rounded down to even numbers`() {
        derive(video(1001, 563, 30f)) shouldBe ProjectFormat(1000, 562, 30)
    }

    @Test
    fun `frame rate rounds to the nearest of 24, 30 and 60`() {
        derive(video(1920, 1080, 23.976f)).fps shouldBe 24
        derive(video(1920, 1080, 25f)).fps shouldBe 24
        derive(video(1920, 1080, 29.97f)).fps shouldBe 30
        derive(video(1920, 1080, 50f)).fps shouldBe 60
        derive(video(1920, 1080, 59.94f)).fps shouldBe 60
        derive(video(1920, 1080, 120f)).fps shouldBe 60
        derive(video(1920, 1080, 15f)).fps shouldBe 24
    }

    @Test
    fun `unknown frame rate defaults to 30`() {
        derive(video(1920, 1080, null)).fps shouldBe 30
    }

    @Test
    fun `a photo gives 30 fps and the photo's aspect ratio scaled to 1080`() {
        derive(testPhoto("p")) shouldBe ProjectFormat(1440, 1080, 30)
    }
}
