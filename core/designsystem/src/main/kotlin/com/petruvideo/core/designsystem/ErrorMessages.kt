package com.petruvideo.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.petruvideo.core.model.error.AppError

@Composable
fun AppError.userMessage(): String = when (this) {
    AppError.LowStorage -> stringResource(R.string.error_low_storage)
    AppError.MissingMedia -> stringResource(R.string.error_missing_media)
    is AppError.UnsupportedFile -> stringResource(R.string.error_unsupported_file, name)
    AppError.PermissionDenied -> stringResource(R.string.error_permission_denied)
}
