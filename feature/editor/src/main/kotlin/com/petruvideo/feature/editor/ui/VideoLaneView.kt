package com.petruvideo.feature.editor.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.petruvideo.core.designsystem.PetruColors
import com.petruvideo.core.model.Availability
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.VideoClip
import com.petruvideo.feature.editor.TimelineGeometry
import com.petruvideo.feature.editor.TrimLimits

private val LANE_HEIGHT = 56.dp
private val TILE_WIDTH = 40.dp
private val CLIP_SHAPE = RoundedCornerShape(8.dp)

private fun outline(selected: Boolean): Modifier =
    if (selected) Modifier.border(2.5.dp, PetruColors.OnBackground, CLIP_SHAPE) else Modifier

/**
 * The main video lane: thumbnails per clip, selection outline, and drag to reorder (long-press, then drag). One
 * geometry unit is one dp, so widths come straight from [TimelineGeometry].
 */
@Composable
fun VideoLaneView(
    lane: List<VideoClip>,
    media: Map<String, MediaItem>,
    geometry: TimelineGeometry,
    selectedClipId: String?,
    onSelect: (String) -> Unit,
    onMoveClip: (clipId: String, newIndex: Int) -> Unit,
    onTrim: (clipId: String, start: Boolean, newSourceMs: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current.density
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragDx by remember { mutableFloatStateOf(0f) }
    var previewLength by remember { mutableStateOf<Pair<String, Long>?>(null) }

    Row(modifier.height(LANE_HEIGHT)) {
        lane.forEachIndexed { index, clip ->
            val item = media[clip.mediaItemId]
            val widthDp = geometry.timeToPx(clip.lengthMs).dp
            val startDp = geometry.videoRects(lane)[index].startPx
            val dragging = draggingId == clip.id
            Box(
                Modifier
                    .width(widthDp)
                    .fillMaxHeight()
                    .zIndex(if (dragging) 1f else 0f)
                    .graphicsLayer { translationX = if (dragging) dragDx else 0f; alpha = if (dragging) 0.85f else 1f }
                    .clip(CLIP_SHAPE)
                    .then(outline(clip.id == selectedClipId))
                    .clickable { onSelect(clip.id) }
                    .pointerInput(clip.id, lane) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { draggingId = clip.id; dragDx = 0f },
                            onDrag = { change, amount -> change.consume(); dragDx += amount.x },
                            onDragEnd = {
                                val center = startDp + widthDp.value / 2 + dragDx / density
                                val target = geometry.dropIndex(lane, index, center)
                                draggingId = null
                                dragDx = 0f
                                if (target != index) onMoveClip(clip.id, target)
                            },
                            onDragCancel = { draggingId = null; dragDx = 0f },
                        )
                    }
                    .semantics {
                        role = Role.Button
                        selected = clip.id == selectedClipId
                        contentDescription = "Clip ${index + 1}, ${item?.displayName.orEmpty()}"
                    },
            ) {
                if (item == null || item.availability == Availability.Missing) {
                    MissingClipPlaceholder(item?.displayName.orEmpty(), Modifier.fillMaxHeight().width(widthDp))
                } else {
                    ClipThumbnails(clip, item, widthDp.value)
                }
                if (clip.id == selectedClipId) {
                    TrimHandles(
                        geometry = geometry,
                        currentLengthMs = clip.lengthMs,
                        target = { start, delta -> TrimLimits.video(clip, item, start, delta) },
                        onPreview = { _, target -> previewLength = clip.id to target.lengthMs },
                        onCommit = { start, target ->
                            previewLength = null
                            if (target.lengthMs != clip.lengthMs) onTrim(clip.id, start, target.newSourceMs)
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                    DurationBadge(
                        previewLength?.takeIf { it.first == clip.id }?.second ?: clip.lengthMs,
                        Modifier.align(Alignment.TopCenter).padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ClipThumbnails(clip: VideoClip, item: MediaItem, widthDp: Float) {
    val loaders = LocalMediaLoaders.current
    val heightPx = with(LocalDensity.current) { LANE_HEIGHT.roundToPx() }
    val tiles = maxOf(1, Math.ceil(widthDp / TILE_WIDTH.value.toDouble()).toInt())
    Row(Modifier.fillMaxHeight().background(PetruColors.SurfaceHigh)) {
        repeat(tiles) { i ->
            val timeMs = clip.sourceStartMs + (clip.lengthMs * (i + 0.5) / tiles).toLong()
            val image by produceState<ImageBitmap?>(null, item.id, timeMs, heightPx) {
                value = if (item.kind == MediaKind.Audio) null else loaders.thumbnail(item, timeMs, heightPx)
            }
            Box(Modifier.width(TILE_WIDTH).fillMaxHeight()) {
                image?.let {
                    Image(it, null, Modifier.fillMaxHeight().width(TILE_WIDTH), contentScale = ContentScale.Crop)
                }
            }
        }
    }
}
