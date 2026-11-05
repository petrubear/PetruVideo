package com.petruvideo.core.media.text

import android.graphics.Bitmap
import com.petruvideo.core.model.TextAlign
import com.petruvideo.core.model.TextBackground
import com.petruvideo.core.model.TextClip
import com.petruvideo.core.model.TextFont
import com.petruvideo.core.model.TextOutline
import com.petruvideo.core.model.TextStyle
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.security.MessageDigest

/**
 * Fingerprints of how default text looks (feature 036): rotation, shadow and spacing at their defaults must leave
 * every pixel as it was before they existed. The expected values were recorded from the renderer before the change.
 */
class TextRendererFingerprintTest {
    private val samples = mapOf(
        "plain" to TextClip("a", "TITLE", 0, 2_000, sizePermille = 100),
        "two lines" to TextClip("b", "First line\nSecond line", 0, 2_000, sizePermille = 70),
        "boxed" to TextClip(
            "c", "Boxed caption", 0, 2_000, sizePermille = 60,
            style = TextStyle(background = TextBackground(true), align = TextAlign.Left),
        ),
        "outlined" to TextClip(
            "d", "Outlined", 0, 2_000, sizePermille = 120,
            style = TextStyle(font = TextFont.Serif, outline = TextOutline(true, widthPermille = 30)),
        ),
        "boxed and outlined" to TextClip(
            "e", "Both styles", 0, 2_000, sizePermille = 90,
            style = TextStyle(
                font = TextFont.Display, background = TextBackground(true, opacityPercent = 80),
                outline = TextOutline(true, widthPermille = 18),
            ),
        ),
    )

    private fun fingerprint(bitmap: Bitmap): String {
        val bytes = ByteBuffer.allocate(bitmap.byteCount).also { bitmap.copyPixelsToBuffer(it) }.array()
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes + "${bitmap.width}x${bitmap.height}".toByteArray())
        return digest.joinToString("") { "%02x".format(it) }.take(16)
    }

    @Test
    fun defaultTextRendersExactlyAsBefore() {
        // The hashes were recorded with the emulator's fonts; another maker's fonts draw the same text differently.
        assumeTrue("pinned to the emulator's fonts", android.os.Build.FINGERPRINT.contains("generic") || android.os.Build.HARDWARE.contains("ranchu"))
        val actual = samples.mapValues { (_, clip) -> fingerprint(TextRenderer.render(clip, 1280, 720)) }
        val expected = EXPECTED
        check(actual == expected) { "fingerprints: $actual" }
    }

    private companion object {
        val EXPECTED: Map<String, String> = mapOf(
            "plain" to "77f17beb6ce3b44a",
            "two lines" to "c398c5973b9aef7b",
            "boxed" to "2a127c958eab9417",
            "outlined" to "83290f6e17668901",
            "boxed and outlined" to "b7dec0e24d0fd1c1",
        )
    }
}
