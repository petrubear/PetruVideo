package com.petruvideo.core.media.effects

import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Crop
import androidx.media3.effect.ScaleAndRotateTransformation
import com.petruvideo.core.model.ClipTransform

private const val DEGREES_PER_TURN = 90f
private const val TURNS = 4
private const val NDC_SPAN = 2f
private const val PERMILLE = 1000f

/**
 * The effects that turn, mirror, and crop a clip's picture, in that order, to run before the fit to the frame
 * (feature 016). A clip without a transform gets none. Media3 turns counterclockwise, so a clockwise turn counts the
 * other way round.
 */
@OptIn(UnstableApi::class)
fun transformEffects(transform: ClipTransform): List<Effect> = buildList {
    if (transform.quarterTurns % TURNS != 0) {
        val ccw = (TURNS - transform.quarterTurns % TURNS) * DEGREES_PER_TURN
        add(ScaleAndRotateTransformation.Builder().setRotationDegrees(ccw).build())
    }
    if (transform.flipH || transform.flipV) {
        add(
            ScaleAndRotateTransformation.Builder()
                .setScale(if (transform.flipH) -1f else 1f, if (transform.flipV) -1f else 1f)
                .build(),
        )
    }
    val crop = transform.crop
    if (!crop.isFull) {
        // Our box runs from the top left in permille; the effect takes edges from -1 to 1 with the top at +1.
        add(
            Crop(
                -1f + NDC_SPAN * crop.left / PERMILLE,
                -1f + NDC_SPAN * crop.right / PERMILLE,
                1f - NDC_SPAN * crop.bottom / PERMILLE,
                1f - NDC_SPAN * crop.top / PERMILLE,
            ),
        )
    }
}
