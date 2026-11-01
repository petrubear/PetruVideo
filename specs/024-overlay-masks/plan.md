# Implementation Plan: Overlay Masks

**Branch**: `024-overlay-masks` | **Spec**: [spec.md](spec.md)

## Summary

`OverlayClip.mask` (`OverlayMask`: kind, size, feather, invert). A GL shader step on the overlay's input multiplies alpha by a shape coverage value; it follows the chroma key step. Settings live in a handle read every frame so sliders are live. Room v21 adds four columns to `overlay_clip`. The Overlay panel gets a third tab, Mask.

## Design

- **Model** (`Mask.kt`): `MaskKind {None, Circle, Rectangle, Linear}`, `OverlayMask(kind, size, feather, invert)`, `normalized`, `inRange`; `SetOverlayMask` command (OverlayCommand).
- **Data**: `maskKind` (TEXT, default `None`), `maskSize` (70), `maskFeather` (20), `maskInvert` (0); auto migration 20 to 21.
- **Shader** (`core/media/effects/OverlayMaskEffect.kt`): coverage by signed distance in the overlay's uv space; feather = smoothstep over `feather * 0.25 * min(w,h)`; Linear uses uv.x against size; invert flips; alpha multiplied.
- **Builder**: the overlay item gets the mask effect after the chroma effect when `mask.kind != None`; handles kept in `masks`; `setOverlayLive` updates them.
- **UI**: `OverlayControls` segmented control gains "Mask": chips None, Circle, Rectangle, Linear; sliders Size and Feather; chip Invert.
- **Blend modes**: Media3 1.11's compositor places layers with opacity only; no blend mode. Not built; follow-up needs a custom compositor.

## Constitution Check

No violations: pure model and commands, offline, accessible, existing panel slot in all layouts.
