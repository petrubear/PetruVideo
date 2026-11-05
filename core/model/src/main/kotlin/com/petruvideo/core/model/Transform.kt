package com.petruvideo.core.model

const val CROP_FULL = 1000
const val CROP_MIN = 100

/**
 * A part of the turned picture in permille of its width and height. The whole picture is `0, 0, 1000, 1000`.
 */
data class CropBox(
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = CROP_FULL,
    val bottom: Int = CROP_FULL,
) {
    val isFull: Boolean get() = this == CropBox()

    val widthPermille: Int get() = right - left
    val heightPermille: Int get() = bottom - top

    val inRange: Boolean
        get() = left >= 0 && top >= 0 && right <= CROP_FULL && bottom <= CROP_FULL &&
            right - left >= CROP_MIN && bottom - top >= CROP_MIN

    /** The box inside the picture, at least [CROP_MIN] wide and tall. */
    fun normalized(): CropBox {
        val l = left.coerceIn(0, CROP_FULL - CROP_MIN)
        val t = top.coerceIn(0, CROP_FULL - CROP_MIN)
        return CropBox(l, t, right.coerceIn(l + CROP_MIN, CROP_FULL), bottom.coerceIn(t + CROP_MIN, CROP_FULL))
    }

    /** The box after the picture is turned a quarter turn clockwise: the same content, in the new picture. */
    fun turned() = CropBox(CROP_FULL - bottom, left, CROP_FULL - top, right)

    fun mirroredHorizontally() = CropBox(CROP_FULL - right, top, CROP_FULL - left, bottom)

    fun mirroredVertically() = CropBox(left, CROP_FULL - bottom, right, CROP_FULL - top)

    /** The box moved by ([dx], [dy]) permille, kept inside the picture at the same size. */
    fun moved(dx: Int, dy: Int): CropBox {
        val x = (left + dx).coerceIn(0, CROP_FULL - widthPermille)
        val y = (top + dy).coerceIn(0, CROP_FULL - heightPermille)
        return CropBox(x, y, x + widthPermille, y + heightPermille)
    }

    /**
     * The box with [handle] dragged by ([dx], [dy]) permille. With a pixel shape [aspect] (width over height) the box
     * keeps that shape on a [pictureWidth] by [pictureHeight] picture, anchored on the opposite corner or edge.
     */
    fun resized(
        handle: CropHandle,
        dx: Int,
        dy: Int,
        aspect: Float?,
        pictureWidth: Int,
        pictureHeight: Int,
    ): CropBox {
        val free = CropBox(
            left + if (handle.movesLeft) dx else 0,
            top + if (handle.movesTop) dy else 0,
            right + if (handle.movesRight) dx else 0,
            bottom + if (handle.movesBottom) dy else 0,
        ).normalized()
        if (aspect == null) return free
        return keepShape(free, handle, aspect * pictureHeight / pictureWidth)
    }

    private fun keepShape(free: CropBox, handle: CropHandle, ratio: Float): CropBox {
        val ax = anchorOf(handle.movesLeft, handle.movesRight, left, right)
        val ay = anchorOf(handle.movesTop, handle.movesBottom, top, bottom)
        val widthLeads = handle.movesLeft || handle.movesRight
        val wanted = if (widthLeads) free.widthPermille.toFloat() else free.heightPermille * ratio
        val roomW = roomFrom(handle.movesLeft, handle.movesRight, ax)
        val roomH = roomFrom(handle.movesTop, handle.movesBottom, ay)
        val w = minOf(wanted, roomW, roomH * ratio).coerceAtLeast(maxOf(CROP_MIN.toFloat(), CROP_MIN * ratio))
        val h = w / ratio
        val l = originFrom(handle.movesLeft, handle.movesRight, ax, w)
        val t = originFrom(handle.movesTop, handle.movesBottom, ay, h)
        return CropBox(l.toInt(), t.toInt(), (l + w).toInt(), (t + h).toInt()).normalized()
    }

    /** The fixed side of a resize: the far edge of the dragged side, or the middle when neither edge moves. */
    private fun anchorOf(low: Boolean, high: Boolean, lo: Int, hi: Int) =
        if (low) hi else if (high) lo else (lo + hi) / 2

    /** How far the box can grow from [anchor] before it leaves the picture. */
    private fun roomFrom(low: Boolean, high: Boolean, anchor: Int): Float =
        (if (low) anchor else if (high) CROP_FULL - anchor else 2 * minOf(anchor, CROP_FULL - anchor)).toFloat()

    private fun originFrom(low: Boolean, high: Boolean, anchor: Int, size: Float): Float =
        if (low) anchor - size else if (high) anchor.toFloat() else anchor - size / 2

    companion object {
        /**
         * The largest box of pixel shape [aspect] (width over height) that fits a [pictureWidth] by [pictureHeight]
         * picture, centered.
         */
        fun forShape(aspect: Float, pictureWidth: Int, pictureHeight: Int): CropBox {
            val width = minOf(pictureWidth.toFloat(), pictureHeight * aspect)
            val height = width / aspect
            val w = (width / pictureWidth * CROP_FULL).toInt().coerceIn(CROP_MIN, CROP_FULL)
            val h = (height / pictureHeight * CROP_FULL).toInt().coerceIn(CROP_MIN, CROP_FULL)
            val l = (CROP_FULL - w) / 2
            val t = (CROP_FULL - h) / 2
            return CropBox(l, t, l + w, t + h)
        }
    }
}

/** The eight handles of the crop box; each names the edges it moves. */
enum class CropHandle(
    val movesLeft: Boolean = false,
    val movesTop: Boolean = false,
    val movesRight: Boolean = false,
    val movesBottom: Boolean = false,
) {
    TopLeft(movesLeft = true, movesTop = true),
    Top(movesTop = true),
    TopRight(movesTop = true, movesRight = true),
    Right(movesRight = true),
    BottomRight(movesRight = true, movesBottom = true),
    Bottom(movesBottom = true),
    BottomLeft(movesLeft = true, movesBottom = true),
    Left(movesLeft = true),
}

/** The shapes the crop box can keep; Free has none. */
enum class CropShape(val aspect: Float?) {
    Free(null),
    Square(1f),
    Portrait(4f / 5f),
    Story(9f / 16f),
    Wide(16f / 9f),
    Classic(4f / 3f),
}

/**
 * How a clip's picture is turned, mirrored, and cropped before it is fitted to the frame: first [quarterTurns]
 * clockwise quarter turns, then the mirrors, then the [crop] of the result.
 */
data class ClipTransform(
    val quarterTurns: Int = 0,
    val flipH: Boolean = false,
    val flipV: Boolean = false,
    val crop: CropBox = CropBox(),
) {
    val isNeutral: Boolean get() = this == ClipTransform()

    val inRange: Boolean get() = quarterTurns in 0..3 && crop.inRange

    /**
     * Turned a quarter turn clockwise as the picture now looks. Turning a mirrored picture swaps which axis the
     * mirror is on, so the two flips swap while the stored turns go up by one.
     */
    fun rotated() = ClipTransform((quarterTurns + 1) % 4, flipV, flipH, crop.turned())

    fun flippedHorizontally() = copy(flipH = !flipH, crop = crop.mirroredHorizontally())

    fun flippedVertically() = copy(flipV = !flipV, crop = crop.mirroredVertically())

    /** The size of a [width] by [height] picture after the quarter turns. */
    fun turnedSize(width: Int, height: Int): Pair<Int, Int> =
        if (quarterTurns % 2 == 1) height to width else width to height
}
