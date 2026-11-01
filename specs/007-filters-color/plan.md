# Implementation Plan: Filters and Color Adjustments

**Branch**: `007-filters-color` | **Date**: 2026-11-01 | **Spec**: [spec.md](spec.md)

## Summary

Add a `ClipFilter` (look, intensity, five adjustments) to video and photo clips, commands with undo, Room v7 columns, a Filters tab with Looks and Adjust panels, "Fl" badge, and rendering in preview and export. Approach: one pure Kotlin `ColorMath` turns a `ClipFilter` into a 4x4 color matrix (look blended by intensity, then saturation, contrast, brightness, warmth); a `LiveRgbMatrix` (Media3 `RgbMatrix`) applies it per clip with live-changeable state; vignette is a radial-gradient bitmap overlay (`BitmapOverlay`) per clip sized to the real frame. The same matrix drives the look tiles in the panel (Compose `ColorFilter`), so tiles match the video. Effects are added to a clip only while its filter is non-neutral; going neutral to active rebuilds, value changes are live.

## Technical Context

**Language/Version**: Kotlin 2.4, JVM 17. **Dependencies**: Media3 1.11 (`RgbMatrix`, `BitmapOverlay`), Room 2.8, Compose BOM 2026.09; no new libraries.
**Storage**: Room schema 6 to 7: columns `flt_look`, `flt_intensity`, `flt_brightness`, `flt_contrast`, `flt_saturation`, `flt_warmth`, `flt_vignette` on `video_clip` with neutral defaults; auto migration plus test.
**Testing**: JVM tests for `ColorMath` (identity at neutral, intensity 0 equals identity, B&W gives equal channels, ranges), commands, rules (split, duplicate, apply to all), view model; Room migration and round trip; Compose and accessibility; Paparazzi goldens; emulator export tests comparing exported average colors with and without filters, vignette corner darkening, and neutral equality.
**Performance Goals**: change visible within 100 ms; 30 fps at 1080p with filters on 10 clips (device checks deferred).
**Constraints**: offline; panel and values survive fold and rotation; one undo step per gesture; same matrix in preview and export.

## Constitution Check

All principles PASS: native Kotlin and Media3 effects (I); non-destructive clip setting with commands and undo (II); two cheap GPU passes only while active, measured later (III); independent slice (IV); `ColorMath` and commands test-first on the JVM (V); engine Android-free, rendering in `core:media` (VI); on-device (VII); panels, inspector, tabletop and state survival (VIII).

## Project Structure

```text
core/model: Filter.kt (FilterLook, ClipFilter, limits), ColorMath.kt (matrix, pure), engine/FilterCommands.kt, Models.kt (VideoClip.filter), engine rules in SplitDuplicateDelete.kt
core/data/db: Entities, Mappers, PetruDatabase v7, schemas/7.json
core/media: filter/LiveRgbMatrix.kt, filter/VignetteOverlay.kt, composition/CompositionBuilder.kt (adds effects, keeps live handles), preview/Media3PreviewEngine.kt + PreviewEngine.kt (setClipFilterLive, frame refresh)
feature/editor: EditorContract/ViewModel (Filters tab, panels, events), ui/FilterPanels.kt (looks with tiles, adjust sliders), ClipBadge "Fl", MainToolBar Filters tab, inspector sections, tabletop
```

## Risks

- **R1**: `RgbMatrix` offsets and HDR. Mitigation: spike in an export test (brightness +, B&W); matrix uses translation in elements 12 to 14 like Media3's own `Brightness`.
- **R2**: A paused preview does not redraw after a live change. Mitigation: same debounced seek refresh as text.
- **R3**: Vignette overlay needs the real frame size and per-item time. Mitigation: size from `configure`, static bitmap cached by size, alpha from strength.
- **R4**: Neutral to active switching rebuilds the composition. Mitigation: Content equality treats "active" as one structural value so slider drags never rebuild.
- **R5**: Tiles approximating the video. Mitigation: same matrix; vignette not shown in tiles.
