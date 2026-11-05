package com.petruvideo.core.designsystem

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

/**
 * A text for the user that is not resolved yet, so view models and state can hold it without a Context and the screen
 * shows it in the language of the moment, even after the language changes (feature 032).
 */
sealed interface UiText {
    /** A string resource with its arguments. */
    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText {
        constructor(@StringRes id: Int, vararg args: Any) : this(id, args.toList())
    }

    /** A plurals resource for [count], with its arguments (the count is not added to them). */
    data class Plural(@PluralsRes val id: Int, val count: Int, val args: List<Any> = emptyList()) : UiText

    /** A text that is not translated: something the user wrote or imported. */
    data class Raw(val value: String) : UiText

    @Composable
    fun asString(): String = when (this) {
        is Res -> stringResource(id, *args.toTypedArray())
        is Plural -> pluralStringResource(id, count, *args.toTypedArray())
        is Raw -> value
    }

    fun asString(context: Context): String = when (this) {
        is Res -> context.getString(id, *args.toTypedArray())
        is Plural -> context.resources.getQuantityString(id, count, *args.toTypedArray())
        is Raw -> value
    }
}

/**
 * The name of an enum entry in the current language, read from the string resource `<group>_<EntryName>` (for example
 * `reverb_SmallRoom`). Lets modules without Android resources keep plain enums; a test checks every entry has its text.
 */
@Composable
fun enumLabel(group: String, value: Enum<*>): String {
    val context = LocalContext.current
    val id = context.resources.getIdentifier("${group}_${value.name}", "string", context.packageName)
    return if (id != 0) stringResource(id) else value.name
}

/** The text of the string resource named [key] (hyphens become underscores), or the key when there is none. */
@Composable
fun keyedText(key: String): String {
    val context = LocalContext.current
    val id = context.resources.getIdentifier(key.replace('-', '_'), "string", context.packageName)
    return if (id != 0) stringResource(id) else key
}
