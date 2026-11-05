@file:Suppress("SpreadOperator") // The resource APIs take their arguments as a vararg.

package com.petruvideo.core.designsystem

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
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

    /** Several texts one after another, for example the sentences of a summary. */
    data class Joined(val parts: List<UiText>, val separator: String = ". ") : UiText

    /** A text that is not translated: something the user wrote or imported. */
    data class Raw(val value: String) : UiText

    @Composable
    fun asString(): String = when (this) {
        is Res -> stringResource(id, *args.toTypedArray())
        is Plural -> pluralStringResource(id, count, *args.toTypedArray())
        is Raw -> value
        is Joined -> parts.map { it.asString() }.joinToString(separator)
    }

    fun asString(context: Context): String = when (this) {
        is Res -> context.getString(id, *args.toTypedArray())
        is Plural -> context.resources.getQuantityString(id, count, *args.toTypedArray())
        is Raw -> value
        is Joined -> parts.joinToString(separator) { it.asString(context) }
    }
}

/**
 * The name of an enum entry in the current language, read from the string resource `<group>_<EntryName>` (for example
 * `reverb_SmallRoom`). Lets modules without Android resources keep plain enums; a test checks every entry has its text.
 */
@Composable
fun enumLabel(group: String, value: Enum<*>): String {
    val resources = LocalResources.current
    val id = resources.getIdentifier("${group}_${value.name}", "string", LocalContext.current.packageName)
    return if (id != 0) stringResource(id) else value.name
}

/** The text of the string resource named [key] (hyphens become underscores), or the key when there is none. */
@Composable
fun keyedText(key: String): String {
    val resources = LocalResources.current
    val id = resources.getIdentifier(key.replace('-', '_'), "string", LocalContext.current.packageName)
    return if (id != 0) stringResource(id) else key
}

/** Describes this element for accessibility with a string resource, read in the current language. */
@Composable
fun Modifier.described(@StringRes id: Int, vararg args: Any): Modifier {
    val text = stringResource(id, *args)
    return semantics { contentDescription = text }
}
