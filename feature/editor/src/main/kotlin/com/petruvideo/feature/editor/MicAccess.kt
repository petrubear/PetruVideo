package com.petruvideo.feature.editor

/** What the user's answer to the microphone question means for the Record button. */
enum class MicAccess { Granted, Denied, Blocked }

object MicPermission {
    /**
     * The access after asking: allowed, refused but the system will ask again ([canAskAgain]), or refused for good
     * (the dialog no longer appears, only the phone's settings can turn the microphone on).
     */
    fun fromAnswer(granted: Boolean, canAskAgain: Boolean): MicAccess = when {
        granted -> MicAccess.Granted
        canAskAgain -> MicAccess.Denied
        else -> MicAccess.Blocked
    }
}
