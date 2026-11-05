package com.petruvideo.core.model

const val REVERB_DECAY_MIN_MS = 200
const val REVERB_DECAY_MAX_MS = 10_000
const val REVERB_PREDELAY_MAX_MS = 200
const val REVERB_PERCENT_MAX = 100

enum class ReverbPreset {
    SmallRoom,
    MediumRoom,
    Hall,
    Cave,
    Plate,
    Custom,
}

/** A clip's reverb: an on/off switch and five settings; [preset] says which preset they match (or Custom). */
data class Reverb(
    val enabled: Boolean = false,
    val preset: ReverbPreset = ReverbPresets.DEFAULT,
    val roomSize: Int = 50,
    val decayMs: Int = 1_500,
    val damping: Int = 50,
    val preDelayMs: Int = 20,
    val mix: Int = 30,
) {
    /** The same settings with every number inside its range and [preset] recomputed from the numbers. */
    fun normalized(): Reverb {
        val clamped = copy(
            roomSize = roomSize.coerceIn(0, REVERB_PERCENT_MAX),
            decayMs = decayMs.coerceIn(REVERB_DECAY_MIN_MS, REVERB_DECAY_MAX_MS),
            damping = damping.coerceIn(0, REVERB_PERCENT_MAX),
            preDelayMs = preDelayMs.coerceIn(0, REVERB_PREDELAY_MAX_MS),
            mix = mix.coerceIn(0, REVERB_PERCENT_MAX),
        )
        return clamped.copy(preset = ReverbPresets.match(clamped))
    }

    val inRange: Boolean get() = normalized().copy(preset = preset) == this
}

object ReverbPresets {
    val DEFAULT = ReverbPreset.MediumRoom

    private val values = mapOf(
        ReverbPreset.SmallRoom to Reverb(false, ReverbPreset.SmallRoom, 25, 600, 60, 5, 25),
        ReverbPreset.MediumRoom to Reverb(false, ReverbPreset.MediumRoom, 50, 1_500, 50, 20, 30),
        ReverbPreset.Hall to Reverb(false, ReverbPreset.Hall, 80, 3_500, 40, 30, 35),
        ReverbPreset.Cave to Reverb(false, ReverbPreset.Cave, 95, 7_000, 30, 60, 45),
        ReverbPreset.Plate to Reverb(false, ReverbPreset.Plate, 60, 2_200, 20, 0, 30),
    )

    val all: List<ReverbPreset> = values.keys.toList()

    /** The numbers of [preset] keeping [enabled]; Custom has no numbers of its own and returns the default's. */
    fun settings(preset: ReverbPreset, enabled: Boolean = false): Reverb =
        (values[preset] ?: values.getValue(DEFAULT)).copy(enabled = enabled)

    /** The preset whose five numbers equal [reverb]'s, or Custom. */
    fun match(reverb: Reverb): ReverbPreset =
        values.entries.firstOrNull { (_, v) -> v.sameNumbers(reverb) }?.key ?: ReverbPreset.Custom

    private fun Reverb.sameNumbers(o: Reverb) =
        roomSize == o.roomSize && decayMs == o.decayMs && damping == o.damping &&
            preDelayMs == o.preDelayMs && mix == o.mix
}
