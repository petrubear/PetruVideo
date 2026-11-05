package com.petruvideo.core.model

const val STICKER_SIZE = 180
const val STICKER_MS = 3_000L

enum class StickerCategory {
    Faces,
    Hearts,
    Celebrate,
    Nature,
    Symbols,
    Shapes,
}

/** A symbol of the gallery: an emoji (own colours) or a shape glyph (drawn in [color]). */
data class Sticker(val glyph: String, val label: String, val category: StickerCategory, val color: Int? = null)

/** The built-in gallery (feature 020); stickers are symbols from the phone's own fonts. */
object StickerCatalog {
    private const val RED = 0xFFFF3B30.toInt()
    private const val YELLOW = 0xFFFFCC00.toInt()
    private const val GREEN = 0xFF34C759.toInt()
    private const val BLUE = 0xFF0A84FF.toInt()

    private fun emoji(category: StickerCategory, vararg items: Pair<String, String>) =
        items.map { (glyph, label) -> Sticker(glyph, label, category) }

    val all: List<Sticker> =
        emoji(
            StickerCategory.Faces,
            "😀" to "Smile", "😂" to "Laughing", "😍" to "Heart eyes",
            "😎" to "Cool", "😭" to "Crying", "😡" to "Angry",
            "🤔" to "Thinking", "😴" to "Sleeping", "🥳" to "Party face",
            "🤯" to "Mind blown",
        ) + emoji(
            StickerCategory.Hearts,
            "❤️" to "Red heart", "🧡" to "Orange heart", "💛" to "Yellow heart",
            "💚" to "Green heart", "💙" to "Blue heart", "💜" to "Purple heart",
            "🖤" to "Black heart", "💔" to "Broken heart", "💖" to "Sparkling heart",
        ) + emoji(
            StickerCategory.Celebrate,
            "🎉" to "Party popper", "🎊" to "Confetti", "🎈" to "Balloon",
            "🎁" to "Gift", "🏆" to "Trophy", "🥇" to "Gold medal",
            "🔥" to "Fire", "✨" to "Sparkles", "🎂" to "Cake",
        ) + emoji(
            StickerCategory.Nature,
            "🌸" to "Blossom", "🌹" to "Rose", "🌞" to "Sun",
            "🌙" to "Moon", "🌈" to "Rainbow", "🍀" to "Clover",
            "🌴" to "Palm tree", "🐶" to "Dog", "🐱" to "Cat",
        ) + emoji(
            StickerCategory.Symbols,
            "👍" to "Thumbs up", "👎" to "Thumbs down", "👏" to "Clapping",
            "🙌" to "Raised hands", "💪" to "Strong", "👀" to "Eyes",
            "✅" to "Check mark", "❌" to "Cross mark", "💯" to "One hundred",
        ) + listOf(
            Sticker("●", "Circle", StickerCategory.Shapes, RED),
            Sticker("■", "Square", StickerCategory.Shapes, BLUE),
            Sticker("▲", "Triangle", StickerCategory.Shapes, GREEN),
            Sticker("★", "Star", StickerCategory.Shapes, YELLOW),
            Sticker("♥", "Heart", StickerCategory.Shapes, RED),
            Sticker("➤", "Arrow", StickerCategory.Shapes, YELLOW),
            Sticker("✔", "Check", StickerCategory.Shapes, GREEN),
            Sticker("✖", "Cross", StickerCategory.Shapes, RED),
        )

    fun of(category: StickerCategory): List<Sticker> = all.filter { it.category == category }
}
