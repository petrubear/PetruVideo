# Quickstart: Validating Clip Speed and Volume

Run after [001's checks](../001-project-media-timeline/quickstart.md) pass. Details are in [data-model.md](data-model.md) and [contracts/](contracts/).

## Automated checks

```bash
gradle :core:model:test
```
Commands and invariants: SetSpeed, SetVolume, trim/split/duplicate/detach with speed, 200-seed property test (undo and redo return the identical timeline).

```bash
gradle :core:data:connectedDebugAndroidTest
```
Room migration 1 to 2 and round trip of speed and volume.

```bash
gradle testDebugUnitTest verifyPaparazziDebug detekt lint
gradle connectedDebugAndroidTest
```
View model, goldens (sheet, badges, inspector, tabletop), Compose flows and accessibility on the Fold.

## Manual scenarios (on the Fold, plus a mid-range phone for 5)

1. **Volume on music**: video with sound plus a music clip; select the music; Volume; drag to 30%; hear it quieter while the video's own sound stays; Undo; hear the original balance.
2. **Volume 0 versus Mute**: set a clip to 0%; it is silent; Mute on and off keeps the 0% level.
3. **Speed 2x ripple**: three clips; middle to 2x; its length halves, the third slides left, total updates; Undo restores.
4. **Slow motion**: 0.5x doubles the length; attached sound slows and keeps pitch.
5. **Speed performance**: 1080p clip at 0.25x, 2x and 4x plays at 30 fps; `PlaybackStats` shows no dropped frames; A/V in step.
6. **Edits on a sped clip**: trim handles move in what is seen; split at the playhead; duplicate; detach (audio stays in step).
7. **Audio overlap limit**: two close audio clips; slow the first until it would overlap; the speed stops at the limit and a message says why.
8. **Photo**: select a photo; Speed and Volume are dimmed.
9. **Badges and inspector**: 1.5x shows "1.5x"; unfold; inspector shows the same value; changing it updates the clip.
10. **Folds**: open the sheet, fold to tabletop, unfold, rotate; the sheet and value survive and sit below the hinge in tabletop.
11. **Persistence**: change speed and volume, force-stop within seconds, reopen; values restored.
12. **Accessibility**: TalkBack announces the slider value; large font shows no clipped text.

## Done when

All automated checks pass, scenarios 1 to 12 pass on the Fold (5 also on a mid-range phone), and the designer has reviewed the sheet, badges, and inspector.
