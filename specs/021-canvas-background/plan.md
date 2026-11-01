# Implementation Plan: Canvas Background

**Branch**: `021-canvas-background` | **Date**: 2026-11-01 | **Spec**: [spec.md](spec.md)

## Summary

`Timeline` gets `canvas: CanvasBackground` (Black, Colour(argb), Blur(strength)). In the composition builder the fit step changes only when the canvas is not Black: instead of Media3's `Presentation` (which leaves black bars) the clip gets a `FitWithBackgroundEffect`, a single shader pass whose output is the project frame: the clip fitted as before in the middle, and outside it either the chosen colour or the clip's own picture enlarged to cover the frame and blurred. Everything after it in the chain (filter, effect, framing) is unchanged. A `SetCanvas` command sets the choice; Room v18 adds three columns to `project`. The editor adds a Background button on the Effects tab bar and a panel with Black, 12 swatches plus a custom picker, and Blur strengths.

## Technical Context

**Language/Version**: Kotlin 2.4.21, AGP 9.5.0, Gradle 9.8 (offline)
**Primary Dependencies**: Media3 1.11.1 (`BaseGlShaderProgram`), Room 2.8 (auto migration 17 to 18), Compose BOM 2026.09, Hilt
**Storage**: Room v18, columns on `project`: `canvasKind` (Black, Colour, Blur), `canvasColor` (ARGB), `canvasBlur` (Light, Medium, Strong)
**Testing**: JUnit4 + kotest (command, invariants), androidTest (export frames: Black equals the old output, Colour fills the bars, Blur shows the picture's colours, a transition and a filter with Blur; migration; round trip), view model tests, Paparazzi goldens, accessibility
**Target Platform**: Android minSdk 26
**Project Type**: mobile-app (existing multi-module)
**Performance Goals**: one extra full-frame pass only for projects with Colour or Blur; Blur uses a 13-tap kernel (two directions folded into one pass) at a quarter-resolution sample
**Constraints**: offline; detekt limits; `EditorViewModel` is large so handlers stay small
**Scale/Scope**: about 14 new or changed files

## Constitution Check

| Principle | Result |
|---|---|
| I Native Kotlin, Media3 | PASS. A Media3 GL shader program. |
| II Non-destructive | PASS. A project setting with undo; sources untouched. |
| III Real-time preview | PASS with measure. One cheap pass; checked in the preview test. |
| IV Incremental slices | PASS. Builds on 001 only. |
| V Test-first on engine | PASS. Command and invariants pure and tested first. |
| VI Clean architecture | PASS. Setting and command in `core:model`, effect in `core:media`, UI in `feature:editor`. |
| VII Privacy and safety | PASS. No network, no permissions. |
| VIII Adaptive and foldable-first | PASS by design. The panel reuses the sheet code. Device checks stay deferred until the user is back. |

Post-design re-check: PASS. No Complexity Tracking entries.

## Project Structure

```text
core/model/.../Canvas.kt                      CanvasBackground, BlurStrength, palette
core/model/.../Models.kt                      Timeline.canvas
core/model/.../engine/CanvasCommands.kt       SetCanvas
core/data/.../db                              columns, mapping, migration 17 to 18
core/media/.../effects/FitWithBackground.kt   the shader pass
core/media/.../composition/CompositionBuilder.kt   uses it instead of the plain fit when the canvas is not Black
feature/editor/.../ui/CanvasPanel.kt          Black, swatches, picker, Blur strengths
feature/editor/.../ui/MainToolBar.kt          Background button on the Effects tab bar
```

**Structure Decision**: extend the existing modules; no new module.

## Complexity Tracking

None.

## Spike findings

The fit shader (contain, cover with 13-tap blur at 0.85 darkening, colour fill) worked first try. Black keeps the Presentation path unchanged.
