# Implementation Plan: Adjustments Pack

**Branch**: `023-adjustments-pack` | **Date**: 2026-11-01 | **Spec**: [spec.md](spec.md)

## Summary

`ClipFilter` gains seven integer fields. One new GL shader pass (`ClipAdjustEffect`) applies them after the existing RGB matrix and before the vignette overlay, read live from a handle so sliders need no rebuild. Room stores seven new `flt_*` columns on `video_clip` (auto migration 19 to 20). The Filters panel's Adjust section is grouped Light, Colour, Detail with a segmented control.

## Design

- **Model**: `exposure`, `gamma`, `highlights`, `shadows`, `tint`, `hue` (-100..100), `sharpen` (0..100) in `ClipFilter`; `adjustmentsNeutral`, `normalized`, `inRange` cover them. `SetFilterAdjustments` and `ResetFilterAdjustments` carry them.
- **Shader** (`core/media/filter/ClipAdjustEffect.kt`): linear exposure `c *= 2^(2*e)`; gamma `c = pow(c, 2^(-g))`; shadows `c += s * k * (1-luma)^2 * (1-c)`... see research.md for exact curves; tint adds `(+t, -t, +t) * 0.1` to RGB scaled by mid-tone; hue rotates in YIQ; sharpen is `c + a * (c - blur4)`. Skipped (no pass added) when all seven are neutral, so neutral clips export as before.
- **Wiring**: `ClipFilterEffects.update` updates the new handle live; `CompositionBuilder` adds the pass only when a clip has a non-neutral new adjustment or already has a filter handle (so a slider drag from neutral works after the first commit).
- **UI**: `AdjustControls` grows to three groups behind a segmented control (Light, Colour, Detail) so the panel height stays near today's.

## Constitution Check

No violations: pure model and commands, offline, accessible, existing panel slot in all layouts.
