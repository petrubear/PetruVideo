package com.petruvideo.feature.editor.ui

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.designsystem.PetruType
import com.petruvideo.core.designsystem.components.PetruChip
import com.petruvideo.core.designsystem.components.PillButton
import com.petruvideo.core.model.CROP_FULL
import com.petruvideo.core.model.ClipTransform
import com.petruvideo.core.model.CropBox
import com.petruvideo.core.model.CropHandle
import com.petruvideo.core.model.CropShape
import com.petruvideo.feature.editor.CropSessionState
import com.petruvideo.feature.editor.EditorEvent
import kotlin.math.roundToInt

private val HANDLE = 48.dp
private const val STILL_HEIGHT_PX = 1080
private const val NUDGE = 10

/** The crop screen: the clip's picture as it now looks with a box to drag, shape chips, Full, Cancel and Done. */
@Composable
fun CropScreen(session: CropSessionState?, onEvent: (EditorEvent) -> Unit, modifier: Modifier = Modifier) {
    if (session == null) return
    val loaders = LocalMediaLoaders.current
    val still by produceState<ImageBitmap?>(null, session.media.id, session.stillTimeMs, session.transform) {
        value = loaders.thumbnail(session.media, session.stillTimeMs, STILL_HEIGHT_PX)
            ?.let { turnedStill(it, session.transform) }
    }
    Column(
        modifier.fillMaxSize().background(PetruColors.Background).statusBarsPadding().navigationBarsPadding()
            .padding(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PillButton("Cancel", onClick = { onEvent(EditorEvent.CropCancel) })
            Text(
                "Crop",
                style = PetruType.Title,
                color = PetruColors.OnBackground,
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            )
            PillButton("Done", onClick = { onEvent(EditorEvent.CropDone) })
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(vertical = 12.dp).padding(HANDLE / 2)) {
            CropPicture(session, still, maxWidth, maxHeight, onEvent)
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CropShape.entries.forEach { shape ->
                PetruChip(
                    shape.label, selected = session.shape == shape,
                    onClick = { onEvent(EditorEvent.CropShapePick(shape)) },
                )
            }
            PetruChip("Full", selected = session.box.isFull, onClick = { onEvent(EditorEvent.CropFullPicture) })
        }
    }
}

/** The picture [still] turned and mirrored like the clip, as an image of the turned picture. */
private fun turnedStill(image: ImageBitmap, transform: ClipTransform): ImageBitmap {
    val matrix = Matrix().apply {
        postRotate(transform.quarterTurns * ROTATION_STEP)
        postScale(if (transform.flipH) -1f else 1f, if (transform.flipV) -1f else 1f)
    }
    val source = image.asAndroidBitmap()
    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true).asImageBitmap()
}

private const val ROTATION_STEP = 90f

@Composable
private fun CropPicture(
    session: CropSessionState,
    still: ImageBitmap?,
    maxWidth: Dp,
    maxHeight: Dp,
    onEvent: (EditorEvent) -> Unit,
) {
    val aspect = session.pictureWidth.toFloat() / session.pictureHeight
    val width = minOf(maxWidth.value, maxHeight.value * aspect).dp
    val height = width / aspect
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(Modifier.width(width).height(height)) {
            still?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds) }
            CropOverlay(session.box, session.shape, width, height, onEvent)
        }
    }
}

@Composable
private fun CropOverlay(box: CropBox, shape: CropShape, width: Dp, height: Dp, onEvent: (EditorEvent) -> Unit) {
    val density = LocalDensity.current
    val widthPx = with(density) { width.toPx() }
    val heightPx = with(density) { height.toPx() }
    val left = width * box.left / CROP_FULL
    val top = height * box.top / CROP_FULL
    val boxWidth = width * box.widthPermille / CROP_FULL
    val boxHeight = height * box.heightPermille / CROP_FULL
    val accent = PetruColors.Accent
    Canvas(Modifier.fillMaxSize().graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
        drawRect(Color.Black.copy(alpha = DIM))
        drawRect(
            Color.Transparent, Offset(left.toPx(), top.toPx()), Size(boxWidth.toPx(), boxHeight.toPx()),
            blendMode = BlendMode.Clear,
        )
        drawRect(
            accent, Offset(left.toPx(), top.toPx()), Size(boxWidth.toPx(), boxHeight.toPx()),
            style = Stroke(BORDER_PX),
        )
    }
    // Moving the whole box: drag inside it.
    Box(
        Modifier
            .offset(left, top).size(boxWidth, boxHeight)
            .dragging(widthPx, heightPx, null, onEvent)
            .semantics { contentDescription = "Crop box, drag to move" },
    )
    CropHandle.entries.forEach { handle ->
        val x = left + boxWidth * handleX(handle) - HANDLE / 2
        val y = top + boxHeight * handleY(handle) - HANDLE / 2
        Box(
            Modifier
                .offset(x, y).size(HANDLE)
                .dragging(widthPx, heightPx, handle, onEvent)
                .semantics {
                    contentDescription = "Crop ${handleName(handle)}"
                    customActions = listOf(
                        CustomAccessibilityAction("Move outward") { nudge(handle, outward = true, shape, onEvent) },
                        CustomAccessibilityAction("Move inward") { nudge(handle, outward = false, shape, onEvent) },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(HANDLE_DOT).background(PetruColors.Accent, androidx.compose.foundation.shape.CircleShape))
        }
    }
}

private fun Modifier.dragging(widthPx: Float, heightPx: Float, handle: CropHandle?, onEvent: (EditorEvent) -> Unit) =
    pointerInput(handle, widthPx, heightPx) {
        detectDragGestures { change, amount ->
            change.consume()
            onEvent(
                EditorEvent.CropDrag(
                    handle,
                    (amount.x / widthPx * CROP_FULL).roundToInt(),
                    (amount.y / heightPx * CROP_FULL).roundToInt(),
                ),
            )
        }
    }

private fun nudge(handle: CropHandle, outward: Boolean, shape: CropShape, onEvent: (EditorEvent) -> Unit): Boolean {
    val sign = if (outward) 1 else -1
    val dx = if (handle.movesLeft) -sign * NUDGE else if (handle.movesRight) sign * NUDGE else 0
    val dy = if (handle.movesTop) -sign * NUDGE else if (handle.movesBottom) sign * NUDGE else 0
    onEvent(EditorEvent.CropDrag(handle, dx, dy))
    return shape == shape
}

private fun handleX(handle: CropHandle): Float = when {
    handle.movesLeft -> 0f
    handle.movesRight -> 1f
    else -> HALF
}

private fun handleY(handle: CropHandle): Float = when {
    handle.movesTop -> 0f
    handle.movesBottom -> 1f
    else -> HALF
}

private fun handleName(handle: CropHandle): String = when (handle) {
    CropHandle.TopLeft -> "top left corner"
    CropHandle.Top -> "top edge"
    CropHandle.TopRight -> "top right corner"
    CropHandle.Right -> "right edge"
    CropHandle.BottomRight -> "bottom right corner"
    CropHandle.Bottom -> "bottom edge"
    CropHandle.BottomLeft -> "bottom left corner"
    CropHandle.Left -> "left edge"
}

private const val HALF = 0.5f
private const val DIM = 0.6f
private const val BORDER_PX = 4f
private val HANDLE_DOT = 16.dp
