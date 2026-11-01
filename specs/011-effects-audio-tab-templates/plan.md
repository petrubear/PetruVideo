# Implementation Plan: Effects, Audio Tab, and Templates

**Branch**: `011-effects-audio-tab-templates` | **Date**: 2026-11-01 | **Spec**: [spec.md](spec.md)

## Summary

Three slices, built in spec order, each shippable alone.

1. **Effects (P1)**: `Transition` (one per cut of the video lane, stored on the earlier clip) and `ClipEffect` (one per video clip). Pure commands with undo. Room v10 columns on `video_clip`. Clip effects are live-updatable GL effects in the shared `CompositionBuilder` (same pattern as filters). Transitions (project shortens by their length, other lanes stay fixed) overlap the tail of clip A with the head of clip B: the video lane is split over two alternating sequences (A/B) and a `TransitionLayout` compositor blends them by time, like `OverlayLayout`.
2. **Audio tab (P2)**: the Audio tab gets a bar (Fade, Noise, EQ, Voice, Volume, Add music) reusing the 005 panels. Add music picks an audio document, adds `MediaKind.Audio` media and an `AudioClip` with `AudioOrigin.Added` (existing `AddAudioClips`) at the playhead. No new tables.
3. **Templates (P3)**: six built-in `Template` definitions in `core/model` (code, not stored). A Templates screen lists them; Use opens the picker, then a pure `TemplateFiller` builds a `Timeline` from the chosen media and `ProjectRepository` saves it as a normal project and opens the editor.

## Technical Context

**Language/Version**: Kotlin 2.4.21, AGP 9.5.0, Gradle 9.8 (offline)
**Primary Dependencies**: Compose BOM 2026.09, Media3 1.11.1 (Transformer, CompositionPlayer, GL effects, VideoCompositorSettings), Room 2.8 (auto migration 9 to 10), Hilt
**Storage**: Room v10 (two nullable column groups on `video_clip`: `trn_*`, `fxe_*`); templates are code
**Testing**: JUnit4 + kotest on the JVM for commands, template filling, invariants; Paparazzi goldens; androidTest for preview, export frame comparison, and migration; accessibility checks
**Target Platform**: Android minSdk 26
**Project Type**: mobile-app (existing multi-module)
**Performance Goals**: Principle III: 30 fps preview, 1080p, up to 3 visible video layers. A transition shows two layers only inside its window.
**Constraints**: offline; media stays on device; lint forbids API above 26 without guards; detekt limits (line 120, complexity 15, method 60)
**Scale/Scope**: 5 transitions, 4 effects, 6 templates, about 25 new files

## Constitution Check

| Principle | Result |
|---|---|
| I Native Kotlin, Media3 | PASS. Media3 effects and compositor only. |
| II Non-destructive | PASS. Sources untouched; edits are model data with undo; templates produce ordinary projects. |
| III Real-time preview | PASS with measure. Transition window uses two inputs; effects are single GL passes. Quality Reduced drops effects and transitions to a hard cut in the preview only. Performance check recorded in quickstart. |
| IV Incremental slices | PASS. Three stories, no dependency on later ones (templates use transitions only if present, else cuts). |
| V Test-first on engine | PASS. Commands, invariants, filler, duration math are pure and tested first. |
| VI Clean architecture | PASS. Engine in `core:model`, GL in `core:media`, UI in `feature:*`. |
| VII Privacy and safety | PASS. No network; audio picked through the system document picker. |
| VIII Adaptive and foldable-first | PASS by design. Effects and Audio bars reuse `EditorBottomBars`, rail entries become active, Templates screen is two-column from 840 dp. Device and fold checks stay deferred until the user is back. |

Post-design re-check: PASS. No violations, so no Complexity Tracking entries.

## Project Structure

### Documentation (this feature)

```text
specs/011-effects-audio-tab-templates/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/ui-screens.md
└── tasks.md          # created by /speckit-tasks
```

### Source Code

```text
core/model/.../Effects.kt                 Transition, TransitionKind, ClipEffect, EffectKind, limits
core/model/.../engine/EffectCommands.kt   SetTransition, RemoveTransition, SetClipEffect, ResetClipEffect
core/model/.../engine/MusicCommands.kt    AddMusic (+ reuse Move/Trim/Split/Duplicate/Delete audio)
core/model/.../template/Template.kt       Template, Slot, TemplateCatalog (6 entries), TemplateFiller
core/data/.../db                          VideoClip columns trn_*, fxe_*; migration 9 to 10 (auto)
core/media/.../effects/ClipEffects.kt     Blur, Pixelate, Mirror, Shake as live GL effects
core/media/.../composition/TransitionLayout.kt  VideoCompositorSettings for A/B blending
core/media/.../composition/CompositionBuilder.kt  A/B sequences when transitions exist; effects per clip
core/media/.../preview/Media3PreviewEngine.kt     multi-input graph also when transitions exist
feature/editor/.../ui/EffectsPanels.kt, MusicFlow.kt, TransitionMarkers.kt
feature/editor/.../EditorContract.kt, EditorViewModel.kt, EditorStateBuilders.kt
feature/editor/.../ui/expanded/ToolRail.kt       Effects and Audio entries active
feature/projects/.../templates/           TemplatesViewModel, TemplatesScreen, TemplatesRoute
app/.../PetruNavHost.kt                   route templates
```

**Structure Decision**: extend the existing modules; no new module. Templates UI sits in `feature:projects` next to Exports and Settings.

## Complexity Tracking

None.

## Spike findings (T001, emulator Pixel_7_Pro_API_34, 2026-11-01)

- Two alternating video sequences plus the existing `VideoCompositorSettings` blend as designed. Two 3 s photos with a 1 s transition export 5.0 s long.
- The input with the LOWER number is drawn on top (overlays sit before the main inputs, so they are above them). The fade of a Dissolve or Zoom therefore goes on the lower input, and in a Cover the lower input is the one that moves.
- Every main input is hidden by alpha 0 outside its clips' time; each sequence runs on to the project end with a gap.
- A true Wipe needs a mask the compositor does not offer, so the fifth kind is Cover (the clip on top slides over or away).
