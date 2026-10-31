# Fold validation (T135)

Device: SM-F976B (Galaxy Z Fold 8 Ultra class), Android 17 (API 37), One UI 9.0, Snapdragon SM8850.
Cover screen 1080x2520 at 480 dpi (360x840 dp). Inner screen 2256x2504 at 480 dpi (752x835 dp).
Build: debug, 2026-10-31.

Status: P = passed on the Fold, F = failed, - = not yet run.

| # | Scenario | Status | Notes |
|---|----------|--------|-------|
| 1 | First project | P (partly) | New project, Add media, clip on timeline all work on the inner screen; the 60 s target was not timed. |
| 2 | Multi-import in order | - | |
| 3 | Bad file and permissions | - | Permission grant by adb only; the prompt path and the system picker are untested. |
| 4 | Timeline and playback | P (partly) | 1080p 30 fps clip plays at 30.0 fps, 0 dropped frames, longest gap 33.3 ms; Play to first frame 76 to 79 ms warm, 239 ms cold. Scrub, reorder, photo handles not run. |
| 5 | Trim and split | - | Covered by instrumented tests on the emulator only. |
| 6 | Detach and replace audio | - | Same. |
| 7 | Format from first clip | P (partly) | Portrait clip gave 720p 30 fps; 1080p clip gave 1080p 30 fps. Mixed-orientation fit not run. |
| 8 | Missing media | - | |
| 9 | Autosave and recovery | - | Process-death test passes on the emulator. |
| 10 | Foldable | P (partly) | Unfolded editor and two-pane Projects verified; tabletop editor and Projects verified. Cover screen shows Projects. Unfold during playback and split-screen not run. |
| 11 | Large-screen input | - | Keyboard shortcuts tested on the emulator only; no keyboard attached. |
| 12 | Accessibility | - | Automated accessibility checks pass on the editor on the emulator; TalkBack and large font not run. |
| 13 | Music over original sound | - | |
| 14 | Add media on large screens | P (partly) | Centered panel with a 6-column grid on the inner screen. Tabletop and medium-width runs not done. |

## Found on the Fold and fixed

- Project detail pane was too narrow at 752 dp: the name and format wrapped letter by letter. Now stacks or shrinks the thumbnail.
- Project media list showed 0 items for a project with a clip. Now follows the project's edit time.
- Multi-pane layouts started at 840 dp, so the unfolded Fold got the phone layout. Now from 700 dp.
- Project detail pane overflowed the shorter lower half in tabletop. Now height-aware.

## Open

- Bottom of the inner screen shows a light strip where the gesture bar is; check whether it is real.
- With the Exported filter on and nothing exported, the list is blank with no message.
- A/V sync within 40 ms (T071) and the mid-range phone (T068) are not measured.
