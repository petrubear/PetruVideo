package com.petruvideo.feature.importmedia

import com.petruvideo.core.data.importing.ImportProgress
import com.petruvideo.core.media.library.LibraryAlbum
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.error.AppError
import androidx.annotation.StringRes

enum class AddMediaTab(@StringRes val labelRes: Int, val kind: MediaKind) {
    Videos(R.string.import_videos, MediaKind.Video),
    Photos(R.string.import_photos, MediaKind.Photo),
    Audio(R.string.import_audio, MediaKind.Audio),
}

/** A thumbnail in the grid. [selectionIndex] is the 1-based badge number, or null when not selected. */
data class GridItem(
    val uri: String,
    val kind: MediaKind,
    val displayName: String,
    val durationMs: Long?,
    val selectionIndex: Int?,
)

/** An entry of the selection tray, kept in selection order. */
data class SelectedItem(
    val uri: String,
    val kind: MediaKind,
    val displayName: String,
    val durationMs: Long?,
)

data class AddMediaState(
    val tab: AddMediaTab = AddMediaTab.Videos,
    val albums: List<LibraryAlbum> = LibraryAlbum.entries,
    val selectedAlbum: LibraryAlbum = LibraryAlbum.Recent,
    val items: List<GridItem> = emptyList(),
    val selection: List<SelectedItem> = emptyList(),
    val totalDurationMs: Long = 0,
    val permission: MediaAccess = MediaAccess.NotAsked,
    val importProgress: ImportProgress? = null,
    val errors: List<AppError.UnsupportedFile> = emptyList(),
    val isLoading: Boolean = false,
)

sealed interface AddMediaEvent {
    data class SwitchTab(val tab: AddMediaTab) : AddMediaEvent
    data class SwitchAlbum(val album: LibraryAlbum) : AddMediaEvent
    data class ToggleItem(val uri: String) : AddMediaEvent

    /** The only add action: imports the selection and adds it to the timeline in selection order. */
    data object AddToTimelineInOrder : AddMediaEvent
    data object LoadMore : AddMediaEvent
    data object Close : AddMediaEvent
    data object RequestAccess : AddMediaEvent
    data object OpenSystemPicker : AddMediaEvent
    data object CancelImport : AddMediaEvent
    data class AccessChanged(val access: MediaAccess) : AddMediaEvent
    data class PickedFromSystem(val uris: List<String>) : AddMediaEvent
    data object DismissErrors : AddMediaEvent
}

sealed interface AddMediaEffect {
    /** Imported files, in selection order, ready to be added to the timeline. */
    data class Done(val items: List<MediaItem>, val closeScreen: Boolean) : AddMediaEffect
    data class RequestAccess(val kind: MediaKind) : AddMediaEffect
    data class LaunchSystemPicker(val kind: MediaKind) : AddMediaEffect
    data object Close : AddMediaEffect
}
