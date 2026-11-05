# Implementation Plan: Spanish Translation

**Branch**: `032-spanish-translation` | **Date**: 2026-11-04 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/032-spanish-translation/spec.md`

## Summary

Move every user-facing text into string resources with an English and a Spanish version, and make the screens read them. Today only `core:designsystem` uses resources (4 error messages); about 700 texts are written into the code: about 370 in `feature:editor`, 50 in `feature:projects`, 28 in `feature:export`, 14 in `feature:importmedia`, 21 in `core:designsystem`, 7 in `core:export` (the export notification), and about 20 reasons in the pure JVM `core:model`. The plan:

1. **Foundation**: a small `UiText` type in `core:designsystem` (a string resource with arguments, a plural, or raw user text) so view models and state can carry a message without holding a `Context`; per-module string files named `values/strings.xml` and `values-es/strings.xml` with module prefixes; build rules that fail on a missing Spanish text and on a literal text in a screen; the glossary.
2. **Engine reasons**: `core:model` stops returning English sentences; it returns small enums or codes (for example the voice split block reason) and the editor turns them into texts.
3. **Convert module by module**, each ending with its screenshot and unit tests green and the English screenshots unchanged: `core:designsystem` and `app`, the export notification, `feature:projects`, `feature:importmedia`, `feature:export`, and `feature:editor` in four slices (tool bars and tools, panels, timeline and overlays, messages and reasons).
4. **Locale behavior**: `localeConfig` for the Android 13+ per-app language setting, `locale` added to `configChanges` so a language change does not recreate the activity, plurals and relative times, the default project name made in the current language.
5. **Verification**: Spanish Paparazzi screenshots at compact width and 1.3x font for each screen family, English goldens unchanged, a Spanish walkthrough on a device, and the maintainer's review of the glossary and texts.

## Technical Context

**Language/Version**: Kotlin 2.4.21, Jetpack Compose, Android resources (`values`, `values-es`, plurals)

**Primary Dependencies**: Android resources and `stringResource` / `pluralStringResource`; Android lint (`MissingTranslation`); `AppCompat`-free per-app locale through `android:localeConfig` (Android 13+); Paparazzi (`DeviceConfig.locale`)

**Storage**: none new; the default project name is saved text at creation (existing column)

**Testing**: lint fails on a missing translation; a repository script `scripts/check-no-literals.sh` fails CI when a screen holds a literal text; unit tests on view models compare message ids instead of English sentences; Paparazzi Spanish goldens; instrumented language switch test

**Target Platform**: Android 8.0+ (minSdk 26); per-app language UI on 13+

**Project Type**: mobile-app (multi-module)

**Performance Goals**: no change; a language switch updates texts within 1 s without recreating the activity

**Constraints**: English output unchanged text for text; no `Context` inside view models or `core:model`; neutral Latin American Spanish; consistent glossary

**Scale/Scope**: about 700 texts, 10 modules, about 55 view-model or unit tests that assert English sentences, 12 Paparazzi classes

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Check |
|---|---|
| I. Native Android, Kotlin-first | Pass: platform string resources and Compose APIs. |
| II. Non-destructive editing | Pass: no project data change; the default project name keeps its saved text. |
| III. Real-time preview performance | Pass: no playback or export path touched; reading a resource is cheap. |
| IV. Incremental delivery | Pass: module by module, each shippable with the rest still English-only until its turn (mixed language during the work is acceptable on a branch, and the release waits for all of it). |
| V. Test-first on core logic | Pass: the engine reasons change is covered by unit tests changed first; the missing-translation and no-literal checks are written before the conversion they guard. |
| VI. Clean architecture | Pass with one change: `core:model` returns reasons, not English text, which strengthens the rule that the engine does not know about the UI. View models carry `UiText`, not a `Context`. |
| VII. Privacy, safety, resources | Pass: accessibility descriptions are translated; no data or permission changes. |
| VIII. Adaptive and foldable-first | Pass with one required change: `locale` is added to the activity's `configChanges`; Spanish text length is checked at compact width, tool bar, cover screen, tabletop, and large fonts. |

No violations; Complexity Tracking is empty. Re-checked after design: unchanged.

## Project Structure

### Documentation (this feature)

```text
specs/032-spanish-translation/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── tasks.md             # created by /speckit-tasks
```

### Source Code (repository root)

```text
core/designsystem/src/main/kotlin/.../UiText.kt            # UiText: Res, Plural, Raw, and asString()
core/designsystem/src/main/res/values{,-es}/strings.xml     # shared texts (errors, common actions)
core/model/src/main/kotlin/.../engine/VoiceSplitCommands.kt # reasons as an enum, not sentences (and similar spots)
core/export/src/main/...ExportService.kt                    # notification channel and texts from resources
core/data/src/main/...                                       # default project name from a provider
feature/projects|importmedia|export|editor/src/main/res/values{,-es}/strings.xml
feature/*/src/main/kotlin/...                                # stringResource / UiText instead of literals
app/src/main/res/values{,-es}/strings.xml                    # app name, notification channel names
app/src/main/res/xml/locales_config.xml, AndroidManifest.xml # per-app language, locale in configChanges
scripts/check-no-literals.sh                                 # CI check for literal texts in screens
docs/glossary-es.md                                          # agreed Spanish terms
build-logic / each module's lint config                      # MissingTranslation is an error
.github/workflows/ci.yml                                     # runs the literal check
```

**Structure Decision**: resources live in the module that shows the text (so a module stays self-contained) with a module prefix on each name; shared words (Done, Cancel, Delete) live in `core:designsystem`.

## Complexity Tracking

No violations.
