# Implementation Plan: Light and Dark Theme

**Branch**: `030-light-dark-theme` | **Date**: 2026-11-04 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/030-light-dark-theme/spec.md`

## Summary

Add a light color set next to the dark one and pick between them from the phone's setting, switching live. Today `PetruColors` and `TrackColors` are plain `Color` constants read directly in 72 files, `PetruTheme` only builds a dark Material scheme, `MainActivity` forces dark system bars, `themes.xml` has a fixed dark window background, and the activity's `configChanges` does not include `uiMode`, so a system theme change would restart the activity. The plan:

1. **One palette source** in `core:designsystem`: a `PetruPalette` data class (all named colors) with `DarkPalette` (today's values, unchanged) and `LightPalette`, provided by a `CompositionLocal`. `PetruColors.X` and `TrackColors.X` keep their names but become composable getters that read the current palette, so the 72 call sites keep compiling inside composables; any call site that is not composable (top-level vals, drawing lambdas that captured nothing) is fixed where the compiler reports it, by reading the color in the composable and passing it in.
2. **Theme**: `PetruTheme(darkTheme = isSystemInDarkTheme())` builds a Material `darkColorScheme` or `lightColorScheme` from the palette and provides the palette.
3. **Live switch without restart**: add `uiMode` to the activity's `configChanges` (the constitution forbids restarts, Principle VIII); `isSystemInDarkTheme()` then recomposes on the configuration change.
4. **System bars and first frame**: call `enableEdgeToEdge` with automatic bar styles from a `DisposableEffect(darkTheme)` so bar icons follow the look at start and on change; `themes.xml` gets a light window background and `values-night/themes.xml` the dark one, so the first frame matches.
5. **Light palette design**: same accent and lane hues, lighter surfaces, darker text, lane fills as light tints; a pure JVM test checks contrast ratios of every text/background pair in both palettes (FR-005, SC-003).
6. **Tests**: existing Paparazzi goldens stay dark (rules set to night mode), light goldens are added for each screen family (SC-005), instrumented test flips the system mode while the editor is open and checks no restart (SC-002).

## Technical Context

**Language/Version**: Kotlin 2.4.21, Jetpack Compose (BOM 2026.09.00), Material 3

**Primary Dependencies**: `androidx.compose.material3` (`lightColorScheme`, `darkColorScheme`), `androidx.activity` (`enableEdgeToEdge`, `SystemBarStyle.auto`), Paparazzi for goldens

**Storage**: none (the look is read from the system each time; nothing saved)

**Testing**: JVM unit test for palette contrast and completeness; Paparazzi screenshots in both modes; instrumented test that changes the system night mode and checks the activity is not recreated; existing accessibility checks in `feature/editor` androidTest run in both modes

**Target Platform**: Android 8.0+ (minSdk 26), phone, foldable inner and cover screens, tablet

**Project Type**: mobile-app (multi-module: `core:designsystem`, all `feature:*`, `app`)

**Performance Goals**: look changes within 1 s of the system change; no extra recompositions beyond one theme pass; no effect on playback or export

**Constraints**: no activity restart, no loss of playhead, selection, open panel or running export; dark look pixel-identical to today; text contrast 4.5:1 (3:1 for large text, icons, outlines)

**Scale/Scope**: about 20 named colors per palette, 10 lane colors, 72 files reading them, 12 Paparazzi test classes

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Check |
|---|---|
| I. Native Android, Kotlin-first | Pass: Compose and Material 3 theming. |
| II. Non-destructive editing | Pass: the look is not part of a project; nothing saved or migrated. |
| III. Real-time preview performance | Pass: no playback or export code changes; a theme switch recomposes UI only. A playback check during a switch is in the quickstart. |
| IV. Incremental delivery | Pass: palette and theme first (everything light and dark), then bars and first frame, then goldens; each step ships. |
| V. Test-first on core logic | Pass: palette contrast and completeness test is written first; UI covered by screenshot and instrumented tests. |
| VI. Clean architecture | Pass: colors live in `core:designsystem`; features only read them. |
| VII. Privacy and resources | Pass: no data, no permission. Accessibility contrast goal is part of the plan. |
| VIII. Adaptive and foldable-first | Pass with one required change: `uiMode` is added to `configChanges` so a mode switch never restarts the activity; verified at compact, medium, expanded, tabletop, cover screen and across fold and unfold. |

No violations; Complexity Tracking is empty. Re-checked after design: unchanged.

## Project Structure

### Documentation (this feature)

```text
specs/030-light-dark-theme/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── tasks.md             # created by /speckit-tasks
```

### Source Code (repository root)

```text
core/designsystem/src/main/kotlin/com/petruvideo/core/designsystem/
├── Color.kt             # PetruPalette, DarkPalette, LightPalette; PetruColors and TrackColors read the current palette
└── PetruTheme.kt        # darkTheme parameter, light and dark Material schemes, palette provider
core/designsystem/src/test/kotlin/com/petruvideo/core/designsystem/
└── PaletteTest.kt       # contrast and completeness of both palettes
app/src/main/
├── kotlin/com/petruvideo/app/MainActivity.kt   # automatic bar styles, updated on look change
├── AndroidManifest.xml                          # uiMode in configChanges
└── res/values/themes.xml, res/values-night/themes.xml   # window background per mode
feature/*/src/main/kotlin/...                    # call sites that are not composable (fixed as the compiler reports them)
feature/*/src/test, core/designsystem/src/test   # Paparazzi rules set to night; light goldens added
feature/editor/src/androidTest/...               # mode switch test; accessibility checks in both modes
```

**Structure Decision**: extend `core:designsystem` in place; no new module. Features keep using `PetruColors` and `TrackColors` by name.

## Complexity Tracking

No violations.
