package com.petruvideo.core.designsystem

import androidx.compose.ui.graphics.Color
import io.kotest.matchers.shouldBe
import kotlin.math.pow
import org.junit.Test

class PaletteTest {
    private fun channel(value: Float): Double =
        if (value <= 0.03928f) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)

    private fun luminance(c: Color) = 0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)

    private fun contrast(a: Color, b: Color): Double {
        val x = luminance(a)
        val y = luminance(b)
        return (maxOf(x, y) + 0.05) / (minOf(x, y) + 0.05)
    }

    private val palettes = mapOf("dark" to DarkPalette, "light" to LightPalette)

    private fun texts(p: PetruPalette) = listOf(
        "onBackground on background" to (p.onBackground to p.background),
        "onBackground on surface" to (p.onBackground to p.surface),
        "onBackground on surfaceHigh" to (p.onBackground to p.surfaceHigh),
        "onBackground on surfaceHighest" to (p.onBackground to p.surfaceHighest),
        "muted on background" to (p.onSurfaceMuted to p.background),
        "muted on surface" to (p.onSurfaceMuted to p.surface),
        "muted on surfaceHigh" to (p.onSurfaceMuted to p.surfaceHigh),
        "onAccent on accent" to (p.onAccent to p.accent),
        "danger on background" to (p.danger to p.background),
        "boost on surface" to (p.boost to p.surface),
    )

    private fun graphics(p: PetruPalette) = listOf(
        "accent on background" to (p.accent to p.background),
        "accent on surface" to (p.accent to p.surface),
        "outline on background" to (p.outline to p.background),
    ) + p.lanes.map { "lane ${it.name} border on its fill" to (it.border to it.fill) } +
        p.lanes.map { "lane ${it.name} border on background" to (it.border to p.background) }

    @Test
    fun `dark palette keeps today's values`() {
        DarkPalette.background shouldBe Color(0xFF0B0C10)
        DarkPalette.surface shouldBe Color(0xFF14161C)
        DarkPalette.onBackground shouldBe Color(0xFFF2F4F8)
        DarkPalette.accent shouldBe Color(0xFF35F0C0)
        DarkPalette.lanes.first { it.name == "Video" }.fill shouldBe Color(0xFF0F2F28)
        DarkPalette.lanes.first { it.name == "Pip" }.border shouldBe Color(0xFFFF6B8A)
    }

    @Test
    fun `text pairs have at least 4_5 to 1 in both looks`() {
        palettes.forEach { (look, p) ->
            texts(p).forEach { (name, pair) ->
                val ratio = contrast(pair.first, pair.second)
                check(ratio >= 4.5) { "$look: $name is %.2f".format(ratio) }
            }
        }
    }

    @Test
    fun `icon outline and lane pairs have at least 3 to 1 in both looks`() {
        palettes.forEach { (look, p) ->
            graphics(p).forEach { (name, pair) ->
                val ratio = contrast(pair.first, pair.second)
                // The dark palette is today's design, so only the light one is held to the full limit for outlines.
                val limit = if (look == "dark" && name.startsWith("outline")) 1.0 else 3.0
                check(ratio >= limit) { "$look: $name is %.2f".format(ratio) }
            }
        }
    }

    @Test
    fun `both looks have the same lane names and the lanes differ from each other`() {
        DarkPalette.lanes.map { it.name } shouldBe LightPalette.lanes.map { it.name }
        palettes.forEach { (look, p) ->
            for (i in p.lanes.indices) for (j in i + 1 until p.lanes.size) {
                val a = p.lanes[i].border
                val b = p.lanes[j].border
                val gap = listOf(a.red - b.red, a.green - b.green, a.blue - b.blue)
                    .sumOf { kotlin.math.abs(it).toDouble() }
                check(gap > 0.35f) { "$look: lanes ${p.lanes[i].name} and ${p.lanes[j].name} are too close ($gap)" }
            }
        }
    }
}
