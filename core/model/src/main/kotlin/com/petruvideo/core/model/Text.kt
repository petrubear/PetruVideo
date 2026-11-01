package com.petruvideo.core.model

const val TEXT_MAX_CHARS = 500
const val TEXT_MIN_MS = 200L
const val TEXT_MAX_MS = 600_000L
const val TEXT_DEFAULT_MS = 5_000L
const val TEXT_ADD_MIN_MS = 500L
const val TEXT_SIZE_MIN = 20
const val TEXT_SIZE_MAX = 300
const val TEXT_SIZE_DEFAULT = 60
const val TEXT_PERMILLE = 1000
const val TEXT_OUTLINE_MAX = 40
const val ANIM_MIN_MS = 200
const val ANIM_MAX_MS = 1_500
const val TEXT_PLACEHOLDER = "Your text"
const val ARGB_WHITE = 0xFFFFFFFF.toInt()
const val ARGB_BLACK = 0xFF000000.toInt()

enum class TextFont(val label: String) {
    Sans("Sans"),
    Serif("Serif"),
    Mono("Mono"),
    Display("Display"),
    Handwritten("Handwritten"),
    Bold("Bold"),
}

enum class TextAlign(val label: String) { Left("Left"), Center("Center"), Right("Right") }

enum class AnimKind(val label: String) {
    None("None"),
    Fade("Fade"),
    SlideUp("Slide up"),
    SlideDown("Slide down"),
    Pop("Pop"),
}

val ANIM_IN_KINDS = listOf(AnimKind.None, AnimKind.Fade, AnimKind.SlideUp, AnimKind.Pop)
val ANIM_OUT_KINDS = listOf(AnimKind.None, AnimKind.Fade, AnimKind.SlideDown, AnimKind.Pop)

data class TextAnimation(val kind: AnimKind = AnimKind.None, val lengthMs: Int = 500) {
    fun normalized() = copy(lengthMs = lengthMs.coerceIn(ANIM_MIN_MS, ANIM_MAX_MS))
}

data class TextBackground(
    val enabled: Boolean = false,
    val colorArgb: Int = ARGB_BLACK,
    val opacityPercent: Int = 60,
)

data class TextOutline(
    val enabled: Boolean = false,
    val colorArgb: Int = ARGB_BLACK,
    val widthPermille: Int = 12,
)

data class TextStyle(
    val font: TextFont = TextFont.Sans,
    val colorArgb: Int = ARGB_WHITE,
    val align: TextAlign = TextAlign.Center,
    val background: TextBackground = TextBackground(),
    val outline: TextOutline = TextOutline(),
) {
    fun normalized() = copy(
        background = background.copy(opacityPercent = background.opacityPercent.coerceIn(0, 100)),
        outline = outline.copy(widthPermille = outline.widthPermille.coerceIn(0, TEXT_OUTLINE_MAX)),
    )
}

/** A title or caption over the video, with its place on the frame as shares of the frame so it scales on export. */
data class TextClip(
    val id: String,
    val text: String,
    val startMs: Long,
    val durationMs: Long,
    val xPermille: Int = TEXT_PERMILLE / 2,
    val yPermille: Int = TEXT_PERMILLE / 2,
    val sizePermille: Int = TEXT_SIZE_DEFAULT,
    val style: TextStyle = TextStyle(),
    val animIn: TextAnimation = TextAnimation(),
    val animOut: TextAnimation = TextAnimation(),
    val keyframes: List<Keyframe> = emptyList(),
    /** Made by Auto captions (feature 019); typed text is false. */
    val caption: Boolean = false,
) {
    val endMs: Long get() = startMs + durationMs

    /** In and out lengths cut down so together they never exceed the clip; the stored choice is not changed. */
    fun effectiveAnimMs(): Pair<Long, Long> {
        val half = durationMs / 2
        val inMs = if (animIn.kind == AnimKind.None) 0L else minOf(animIn.lengthMs.toLong(), half)
        val outMs = if (animOut.kind == AnimKind.None) 0L else minOf(animOut.lengthMs.toLong(), half)
        return inMs to outMs
    }

    /** The plain place and size as a pose (full opacity); used when there are no keyframes. */
    val basePose: Pose get() = Pose(xPermille, yPermille, sizePermille, 100)

    /** The shown values [relMs] after the clip's start. */
    fun poseAt(relMs: Long): Pose = Keyframes.poseAt(basePose, keyframes, relMs)

    /** The clip as the preview box and panels show it at [timeMs]: plain values set to the pose there. */
    fun shownAt(timeMs: Long): TextClip {
        if (keyframes.isEmpty()) return this
        val pose = poseAt((timeMs - startMs).coerceIn(0, durationMs))
        return copy(xPermille = pose.xPermille, yPermille = pose.yPermille, sizePermille = pose.sizePermille)
    }

    /** The clip with [frames]; the plain values follow the first keyframe, or [plain] when the list is empty. */
    fun withKeyframes(frames: List<Keyframe>, plain: Pose? = null): TextClip {
        val pose = frames.firstOrNull()?.pose ?: plain ?: basePose
        return copy(
            keyframes = frames,
            xPermille = pose.xPermille,
            yPermille = pose.yPermille,
            sizePermille = pose.sizePermille,
        )
    }

    fun normalized() = copy(
        text = text.take(TEXT_MAX_CHARS),
        startMs = startMs.coerceAtLeast(0),
        durationMs = durationMs.coerceIn(TEXT_MIN_MS, TEXT_MAX_MS),
        xPermille = xPermille.coerceIn(0, TEXT_PERMILLE),
        yPermille = yPermille.coerceIn(0, TEXT_PERMILLE),
        sizePermille = sizePermille.coerceIn(TEXT_SIZE_MIN, TEXT_SIZE_MAX),
        style = style.normalized(),
        animIn = animIn.normalized(),
        animOut = animOut.normalized(),
        keyframes = Keyframes.normalized(
            keyframes, durationMs.coerceIn(TEXT_MIN_MS, TEXT_MAX_MS), TEXT_SIZE_MIN..TEXT_SIZE_MAX,
        ),
    )

    val inRange: Boolean get() = normalized() == this
}
