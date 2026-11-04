# Quickstart: Overlay Audio

## Prerequisites

Phone connected over USB (debug build), two short videos with different tones, `./gradlew` with `--offline`.

## Automated checks

- `gradle :core:model:test :feature:editor:testDebugUnitTest :core:media:testDebugUnitTest --offline`
- `gradle :core:data:connectedDebugAndroidTest --offline` (migration 22 to 23 and repository round trip)
- Paparazzi: record and verify the overlay panel with sound controls.

## Phone scenarios

1. **Mix (US1, SC-001)**: base video plus two sounding overlays; export; pull the file and confirm three sources are audible at the right times (check with `ffprobe` for an audio stream and by listening or cross-correlation for the start offset, within 40 ms).
2. **Volume and mute (US2, SC-002)**: set one overlay to 50%, then mute it; export; the muted source is absent, the rest unchanged; undo restores.
3. **Effects (US3)**: reverb on one overlay only; fade-in at its start.
4. **Detach (US4, SC-004)**: detach; an audio clip appears at the overlay start; the sound is unchanged; undo restores.
5. **Old project (SC-005)**: open a project made before the update; export; the sound equals the previous export.
6. **Preview (FR-012)**: play 30 s with three sounding overlays; picture never freezes. If the preview falls back, the notice shows and the export still has every sound.
7. **Layouts (FR-013)**: compact, medium, expanded, tabletop; fold and unfold keeps the controls' state.

## Expected outcome

All scenarios pass; the performance check for the preview is recorded in the task notes.
