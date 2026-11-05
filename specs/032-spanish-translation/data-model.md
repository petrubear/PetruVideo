# Data Model: Spanish Translation

No stored data except the existing project name text.

## UiText (in memory, `core:designsystem`)

| Case | Holds | Used for |
|---|---|---|
| `Res` | string resource id and arguments | messages, reasons, notices from view models |
| `Plural` | plurals resource id, count, arguments | counts ("1 clip", "3 clips") |
| `Raw` | a plain string | text the user wrote or imported, names |

`asString()` (composable) resolves it with the current language.

## Reasons (in memory, `core:model`)

Enums or codes replace English sentences, for example `VoiceSplitBlock`. The editor maps each case to a `UiText` with a `when` that must list every case.

## String resources

Names are `<module>_<screen_or_area>_<what>` in snake case (`editor_tool_split`). Each name exists in `values/strings.xml` and `values-es/strings.xml`; plurals in `<plurals>`. `translatable="false"` only for product and technical names.

## Project name

`project.name` stays plain saved text; the default is made from the string resource `projects_default_name` ("Untitled %d" / "Sin título %d") at creation.
