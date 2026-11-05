package com.petruvideo.core.model

import io.kotest.matchers.shouldBe
import org.junit.Test
import java.io.File

/** Every effect and transition kind needs a name in English and in Spanish (feature 035). */
class EffectLabelsTest {
    private fun names(folder: String): Set<String> {
        val file = File("../designsystem/src/main/res/$folder/strings.xml")
        return Regex("""<string name="([^"]+)"""").findAll(file.readText()).map { it.groupValues[1] }.toSet()
    }

    @Test
    fun `every effect and transition kind has an English and a Spanish name`() {
        val wanted = EffectKind.entries.map { "effect_${it.name}" } +
            TransitionKind.entries.map { "transition_${it.name}" }
        val missing = listOf("values", "values-es").associateWith { folder -> wanted.filterNot { it in names(folder) } }
        missing shouldBe mapOf("values" to emptyList<String>(), "values-es" to emptyList<String>())
    }
}
