package com.petruvideo.core.model.error

/** User-facing failures the app handles gracefully (constitution Principle VII). */
sealed interface AppError {
    data object LowStorage : AppError
    data object MissingMedia : AppError
    data class UnsupportedFile(val name: String) : AppError
    data object PermissionDenied : AppError
}
