# Plan: Stacked Effects

`VideoClip.effects: List<ClipEffect>` (max `EFFECT_MAX` = 3, distinct kinds) with `effect` as the first one. `SetClipEffect` adds or updates by kind (LimitReached at 3); `ResetClipEffect(clip, kind?)` removes one or all. Room: `fxe2_` and `fxe3_` embedded columns beside `fxe_` (auto migration 21 to 22, keeps the first effect). Builder keeps a list of `ClipEffectHandle` per clip; the preview updates the handle of the matching kind live. `EffectControls` takes the list: tapping an inactive tile adds it, tapping an active tile selects it for the slider and Remove. Tests: model, view model, migration, panel, export.

## Results

Full connected suite on the emulator only: 314 tests, 0 failures, 3 skipped.
