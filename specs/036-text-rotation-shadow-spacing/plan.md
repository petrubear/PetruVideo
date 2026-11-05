# Implementation Plan: Text Rotation, Shadow and Spacing

**Branch**: `036-text-rotation-shadow-spacing` | **Date**: 2026-11-05 | **Spec**: [spec.md](spec.md)

## Summary

Add rotation, shadow, letter spacing and line spacing to `TextStyle`, draw them in `TextRenderer` (the one place that draws text for preview and export), store them as new defaulted columns, and add four controls to the text Style tool.

## Constitution Check

No new module or dependency, local only, tests first on core logic, same undo path as other text edits (`SetTextStyle`). PASS.

## Findings that shape the design

- `TextRenderer.render` draws a text clip into a bitmap with the Android canvas; `TextClipOverlay` places it. Both preview and export use it, so one change serves both (FR-007, FR-008). The overlay caches the bitmap by `(text, style, size, frame)`, so any new style field must live in `TextStyle` to invalidate the cache.
- Rotation is drawn inside the renderer by rotating the canvas into a bitmap that is the bounding box of the turned block. This keeps the box, outline and shadow turning together, keeps the safe-margin clamp working on the real footprint, and does not depend on how the overlay engine corrects for aspect ratio.
- Shadow uses the paint's shadow layer on the shape it follows (the background box when it is on, the outline stroke when there is one, otherwise the letters). The bitmap edge grows by the shadow's reach so nothing is cut. Off, the edge is unchanged, which is what keeps today's look byte for byte.
- Letter spacing uses the paint's letter spacing in em; line spacing uses the layout's line spacing multiplier.
- `TextClipEntity` stores the style as flat columns, so nine defaulted columns and a Room auto migration (24 to 25) carry the new values.

## Design

- **Model** (`core/model/Text.kt`): `TextShadow(enabled, colorArgb, opacityPercent, softnessPercent, distancePercent, directionDegrees)`; `TextStyle` gains `shadow`, `rotationDegrees`, `letterSpacingPercent`, `lineSpacingPercent` with neutral defaults; `normalized()` clamps; `snapRotation(degrees)` snaps within 3 degrees of 0, 90, -90, 180.
- **Renderer** (`core/media/text/TextRenderer.kt`): pure helpers for the turned bounding size and the shadow's pixel offset and reach (tested on the JVM); drawing changes only act when a new field is non-default.
- **Storage** (`core/data`): columns `rotation`, `shEnabled`, `shColor`, `shOpacity`, `shSoftness`, `shDistance`, `shDirection`, `letterSpacing`, `lineSpacing`; mappers; schema 25; migration test.
- **Editor**: Rotation, Shadow (switch, color, opacity, softness, distance, direction), Letter spacing and Line spacing controls in `TextStyleControls`, each with its value and a Reset; strings in English and Spanish.

## Tests

- **Model**: defaults, clamping, snapping.
- **Renderer helpers (JVM)**: rotated bounds at 0, 90, 45 degrees; shadow offset directions; reach.
- **On device**: before changing the renderer, record a fingerprint of the pixels of several default text clips; after the change they must be identical (FR-006). New: a rotated clip has the turned footprint, a shadow adds pixels outside the unshadowed footprint, letter spacing widens, line spacing makes it taller.
- **Mappers and migration**: round trip; old rows read with defaults.
- **Screenshots**: new Style panel goldens (dark, light, Spanish); existing text screenshots pass unchanged.
