package com.petruvideo.feature.editor.ui.expanded

import android.content.ClipData
import android.content.ClipDescription
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent

private const val MEDIA_LABEL = "petruvideo-media"

/** Tapping adds the item at the playhead; a long press then drag starts a drag that the timeline accepts. */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.mediaDragSource(mediaItemId: String, onTap: () -> Unit): Modifier =
    this.clickable(onClick = onTap).dragAndDropSource { _ ->
        DragAndDropTransferData(ClipData.newPlainText(MEDIA_LABEL, mediaItemId))
    }

/**
 * Accepts media dragged from the Media panel. [onDrop] gets the media id and the drop point in window pixels
 * (x only; the timeline turns it into a time).
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.mediaDropTarget(onDrop: (mediaItemId: String, windowX: Float) -> Unit): Modifier =
    this.dragAndDropTarget(
        shouldStartDragAndDrop = { it.mimeTypes().contains(ClipDescription.MIMETYPE_TEXT_PLAIN) },
        target = object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                val clip = event.toAndroidDragEvent().clipData ?: return false
                if (clip.description.label != MEDIA_LABEL || clip.itemCount == 0) return false
                val id = clip.getItemAt(0).text?.toString() ?: return false
                onDrop(id, event.toAndroidDragEvent().x)
                return true
            }
        },
    )

