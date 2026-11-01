# Implementation Plan: Chroma Key

**Branch**: `022-chroma-key` | **Date**: 2026-11-01 | **Spec**: [spec.md](spec.md)

## Summary

Each overlay clip gets `ChromaKey` settings (on, key colour, strength, smoothness, spill). A GL shader step on the overlay's own input makes pixels near the key colour transparent; the compositor already blends overlay inputs by alpha. Settings are edited in the Overlay panel (a Chroma key tab beside Opacity), stored in the overlay row, and applied identically in preview and export.

## Technical Context

Kotlin, Media3 1.11.1 (BaseGlShaderProgram, EditedMediaItem effects), Room 2.8 auto migration 18 to 19, Compose. Unchanged: engine pattern (immutable Timeline, pure Commands).

## Design

- **Model** (`core/model/Chroma.kt`): `ChromaKey(enabled, color, strength, smoothness, spill)` with `normalized`, `inRange`; presets GREEN `0xFF00B140`, BLUE `0xFF0047BB`. `OverlayClip.chroma` (default off), kept by `copy` so split and duplicate carry it; `normalized()` clamps it; `TimelineInvariants` checks `inRange`.
- **Command**: `SetChromaKey(clipId, chroma)` as an `OverlayCommand` ("Chroma key"), one undo step.
- **Data**: `overlay_clip` gets `chromaEnabled`, `chromaColor`, `chromaStrength`, `chromaSmoothness`, `chromaSpill`; Room v19 auto migration with defaults; mapper updated.
- **Shader** (`core/media/effects/ChromaKeyEffect.kt`): `ChromaKeyHandle` (volatile settings, preview-only `checker` flag) and a `BaseGlShaderProgram`. Chroma distance in CbCr space between pixel and key; `alpha = smoothstep(t0, t0+soft, dist)` with `t0 = strength * 0.5`, `soft = 0.02 + smoothness * 0.2`; spill pulls the key channel excess down on kept pixels by `spill * (1 - alpha_edge)`; checker mode outputs an opaque checkerboard where alpha is low.
- **Builder**: an overlay with the key on gets the effect on its `EditedMediaItem`; handles are kept in `chromas` by clip id. `setOverlayLive(clip)` updates the handle so sliders show live; a toggle or colour change is a commit, which reloads.
- **Preview-only checkerboard**: a local editor flag pushed to the engine (`setChromaChecker`); builders for export never set it.
- **Pick**: `ChromaSampler` (MediaMetadataRetriever frame or decoded photo) returns the pixel for a tap mapped through the overlay's placement; the editor shows a tap layer on the preview while picking.
- **UI**: `OverlayPanel` gains a segmented Opacity / Chroma key control; the Chroma key tab holds the switch, Green and Blue presets, Pick, three sliders, and the checkerboard toggle.

## Constitution Check

No violations: pure engine commands, offline, accessible controls, foldable layouts reuse the existing overlay panel slot.
