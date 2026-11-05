# Implementation Plan: Effects and Transitions Pack

**Branch**: `035-effects-transitions-pack` | **Date**: 2026-11-05 | **Spec**: [spec.md](spec.md)

## Summary

Append ten kinds to `EffectKind` and ten to `TransitionKind`. Effects are new fragment shaders in the existing one-pass effect shader; transitions are new poses (position, scale per axis, rotation, opacity) in the compositor's per-input settings. The lists in the editor are built from the enums, so they grow without new screens. Nothing existing changes.

## Constitution Check

No new module or dependency, local only, tests first on core logic, same undo path. PASS.

## Findings that shape the design

- **Effects** (`core/media/.../effects/ClipEffects.kt`): one `GlProgram` per kind from `fragmentFor(kind)`; uniforms `uStrength`, `uAspect`, `uTime`. Preview and export share it, so one implementation serves both (FR-005). New kinds are new branches, and `USES_ASPECT` / `USES_TIME` list which uniforms a shader declares.
- **Transitions** (`core/media/.../composition/OverlayLayout.kt`): the compositor only takes per-input alpha, anchor, scale and rotation, so no per-pixel transitions (wipe, iris, glitch, pixelate, swirl) are possible; the list in the spec was reworked for that. The preview already plays transitions as hard cuts (`withTransitionsAsCuts`), the export blends; new ones follow the same path.
- **Unknown kinds**: both mappers already drop a kind they do not know (`firstOrNull`), which meets FR-007.
- **Lists**: `EffectPanels.kt` iterates `EffectKind.entries` and `TransitionKind.entries`; labels come from `effect_<Kind>` and `transition_<Kind>` string resources found by `enumLabel`.

## Design

- **Model**: new enum entries appended after the existing ones (stored names, so order is not stored; the test pins the first nine and five anyway).
- **Pose function**: move the per-kind math out of `OverlayLayout.mainSettings` into a pure `TransitionPose` (alpha, x, y, scaleX, scaleY, rotation) computed from kind, progress, outgoing and on-top. The five existing kinds return exactly the values they return today (a test pins them); the new kinds add scale per axis and rotation. `OverlayLayout.placed` builds settings from a pose.
- **Shaders**: ten new bodies; every one returns the input unchanged at strength 0. Strobe and Flash keep their pace fixed (2.5 Hz and 1 Hz) so the rate cap holds at every strength.
- **Flash notice**: applying Strobe or Flash through the editor adds a short message through the existing message banner.
- **Strings**: `effect_*` and `transition_*` for the 20 new kinds, English and Spanish, and one notice string.

## Tests

- **Model**: the first nine effects and five transitions keep their order and names; every kind has a label in English and Spanish (a test over the resource files).
- **Pose**: existing five poses equal the old values at sample progress points; for every new kind the first frame is the outgoing clip fully shown and the last frame is the incoming clip fully shown, with no pose value outside sane bounds.
- **Shaders**: an instrumented test compiles and links every effect program on a device GL context (cannot run on the JVM).
- **Editor**: Strobe and Flash show the notice; effects list contains all kinds.
- **Screenshots**: the existing effect and transition screenshots must pass unchanged.
