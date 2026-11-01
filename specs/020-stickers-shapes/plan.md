# Implementation Plan: Stickers and Shapes

**Branch**: `020-stickers-shapes` | **Date**: 2026-11-01 | **Spec**: [spec.md](spec.md)

## Summary

A sticker or shape is a `TextClip` whose text is one symbol, so the model, lane, selection, preview box, keyframes, animations, split, duplicate, persistence, preview rendering, and export are all reused unchanged. New: a pure `StickerCatalog` (categories, symbols, labels, default colour for shapes), an `AddSticker` command (a text clip of about a fifth of the frame height, 3 s, centered, with the default colour for shapes), a `GlyphCheck` in `feature:editor` that keeps only symbols the phone can draw (`Paint.hasGlyph`), and a Stickers button on the Text tab opening a gallery panel. No database change.

## Technical Context

**Language/Version**: Kotlin 2.4.21, AGP 9.5.0, Gradle 9.8 (offline)
**Primary Dependencies**: existing text rendering (`TextRenderer`), Compose BOM 2026.09, Hilt
**Storage**: none new (a sticker is a `TextClip`)
**Testing**: JUnit4 + kotest (catalog sanity, `AddSticker`), view model tests, androidTest (every symbol draws; a sticker exports in place), Paparazzi goldens, accessibility
**Target Platform**: Android minSdk 26
**Project Type**: mobile-app (existing multi-module)
**Performance Goals**: gallery opens in one frame; the glyph check is done once per process
**Constraints**: offline; detekt limits; `EditorViewModel` is large so sticker handlers stay small
**Scale/Scope**: about 8 new or changed files

## Constitution Check

| Principle | Result |
|---|---|
| I Native Kotlin, Media3 | PASS. Reuses the text layer path. |
| II Non-destructive | PASS. A text layer with undo; no media touched. |
| III Real-time preview | PASS. Same cost as a title. |
| IV Incremental slices | PASS. Builds on 006 and 012 only. |
| V Test-first on engine | PASS. Catalog and command are pure and tested first. |
| VI Clean architecture | PASS. Catalog and command in `core:model`, glyph check and gallery in `feature:editor`. |
| VII Privacy and safety | PASS. No network, no permissions. |
| VIII Adaptive and foldable-first | PASS by design. The gallery reuses the sheet code. Device checks stay deferred until the user is back. |

Post-design re-check: PASS. No Complexity Tracking entries.

## Project Structure

```text
core/model/.../Stickers.kt                    Sticker, StickerCategory, StickerCatalog
core/model/.../engine/StickerCommands.kt      AddSticker
feature/editor/.../GlyphCheck.kt              symbols the phone can draw
feature/editor/.../ui/StickersPanel.kt        categories and the grid
feature/editor/.../ui/MainToolBar.kt          Stickers button on the Text tab
feature/editor/.../EditorViewModel.kt, EditorContract.kt   events and state
```

**Structure Decision**: extend the existing modules; no new module.

## Complexity Tracking

None.
