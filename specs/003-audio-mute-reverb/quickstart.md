# Quickstart: Validating Audio Tools - Mute and Reverb

Details live in [data-model.md](data-model.md) and [contracts/ui-screens.md](contracts/ui-screens.md).

## Automated checks

```bash
gradle --offline :core:model:test
```
Commands, presets, invariants, undo, and the property test with the new commands.

```bash
gradle --offline :core:media:testDebugUnitTest
```
Reverb DSP: impulse response decays at the set time, mix 0 equals input, tail stops with the input, no clipping, parameter changes without discontinuities.

```bash
gradle --offline testDebugUnitTest verifyPaparazziDebug detekt lintDebug
gradle --offline connectedDebugAndroidTest
```
View model, goldens, Room v2 to v3 migration, emulator playback with reverb (seek, split, speed), Compose and accessibility checks.

## Manual scenarios (Fold, then a mid-range phone)

1. **Reverb on (SC-001)**: Select a voice clip, tap Reverb, turn on Hall; hear the tail within 15 s.
2. **Settings live (SC-002)**: While playing, drag Mix, Decay, then More (Room size, Damping, Pre-delay); each is heard in under 100 ms without a gap or click.
3. **Preset and Custom**: Pick Cave, drag Damping, see Custom; Reset returns to Cave.
4. **Tail cut (SC-006)**: Clip followed by silence and by another clip; no tail after the clip end.
5. **Mute**: Mute a video with music over it; only the music plays; Unmute restores sound at the earlier volume and reverb.
6. **Detach a muted clip**: The audio clip is muted.
7. **Split, duplicate, speed**: Reverb kept; sped clips sound natural.
8. **Five reverb clips (SC-003)**: 30 fps and no dropouts; check CPU on the mid-range phone.
9. **Layouts**: Badge on the timeline; inspector sections when unfolded; panel below the hinge in tabletop; fold and rotate with the panel open.
10. **Autosave (SC-005)**: Change settings, force-stop, reopen: same values.
11. **Undo (SC-004)**: Undo and redo every change above.
