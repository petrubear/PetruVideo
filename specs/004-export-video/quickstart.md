# Quickstart: Validating Export Video

Details live in [data-model.md](data-model.md) and [contracts/ui-screens.md](contracts/ui-screens.md).

## Automated checks

```bash
gradle --offline :core:model:test
```
Bitrate table, size estimate, caps from sources, defaults, and settings clamping.

```bash
gradle --offline testDebugUnitTest verifyPaparazziDebug detekt lintDebug
```
View model (options, blockers, state), coordinator state machine, goldens for each arrangement.

```bash
gradle --offline connectedDebugAndroidTest
```
Room v3 to v4 migration, a small project exported on the device and checked with `MediaMetadataRetriever` (length within one frame, resolution, frame rate, audio track), speed and mute and reverb in the output, cancel cleanup (no MediaStore row, no cache file), interrupted job recovery, and the foreground service.

## Manual scenarios (Fold, then a mid-range phone)

1. **Two taps (SC-001)**: Export, Save to gallery; open the file from the gallery and compare it with the preview.
2. **Settings and estimate**: change each segmented control; the bitrate and size update at once; unavailable options show reasons ("above your footage", "not supported").
3. **Caps**: a 1080p-only project dims 2K and 4K; add a 4K clip and 4K appears.
4. **Background (SC-006)**: start a one-minute 4K render, press Home, watch the notification, return to the screen.
5. **Cancel (SC-005)**: cancel from the screen and from the notification; no file in the gallery and no leftover cache.
6. **Kill**: force-stop during a render, reopen; the app says it did not finish and offers Retry.
7. **Storage and missing media**: fill the storage or remove a source; the blocker explains and offers a fix.
8. **Match with the preview**: a project with 2x and 0.5x clips, a muted clip, volume 150%, and reverb; compare sound and length.
9. **Speed (SC-002)**: time a one-minute 1080p 30 fps export on the Fold and on a mid-range phone.
10. **Size (SC-003)**: compare the estimate with the real size for five settings.
11. **Layouts**: cover screen, unfolded, tabletop; fold and rotate during a render; nothing under the hinge.
12. **Sources unchanged (SC-008)**: checksums before and after.
