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
| 5 | Trim and split | P (partly) | Select shows outline, length badge and clip tool bar; Split at the playhead gives two contiguous clips with sound; the split survived an app restart. Trim handles, duplicate, delete and undo/redo not run on the Fold. |
| 6 | Detach and replace audio | P (partly) | Detach empties the clip's own waveform and adds a Detached clip on the audio lane; Detach disappears from the tool bar. Trim, move, mute, replace not run. The "Detached" label overlaps the waveform bars (cosmetic). |
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

## Found on the Fold in the last run

- Tabletop controls had no Undo or Redo; added both (FR-021).

## Open

- Bottom of the inner screen shows a light strip where the gesture bar is; check whether it is real.
- With the Exported filter on and nothing exported, the list is blank with no message.
- A/V sync within 40 ms (T071) and the mid-range phone (T068) are not measured.

## Automated run on the Fold (2026-10-31, SM-F976B, Android 17)

All connected suites pass on the physical Fold: core:data 17, core:media 32, feature:editor 22 (including accessibility checks), feature:projects 13 (including accessibility), feature:importmedia 1 (accessibility), app 2 (rotation retention under 1 s, cover-screen flow). Unit tests, Paparazzi goldens, detekt and lint pass. Espresso 3.7.0 is pinned because older Espresso crashes on Android 17.

## Manual run on the Fold, driven over adb (2026-10-31, SM-F976B, inner screen 752x835 dp)

| Scenario | Result | Notes |
|----------|--------|-------|
| 1 First project | Pass | New project, Import, 3 videos added to the timeline in a few taps |
| 2 Multi-import in order | Pass | Badges 1, 2, 3 follow selection order; tray showed 3 selected, 1:32 total; the clip chosen first landed first |
| 3 Permissions | Pass | Explanation with "Allow access" and "Choose files instead" shown before access was granted; grid loaded after grant |
| 5 Trim, split, duplicate, delete, undo/redo | Pass | Start handle drag trimmed 0.7 s (1:32.0 to 1:31.3); Split at the scrubbed playhead gave Clip 1 and Clip 2; Duplicate added 43.4 s; Delete removed it; each undone |
| 6 Detach | Pass | Detach created "Detached audio" under the clip; Undo removed it; waveform shown |
| 7 Format from first clip | Pass | Portrait first clip gave 720p, 30 fps in the top bar |
| 9 Autosave and recovery | Pass | Edit, force-stop 2 s later, relaunch: project reopened with 4 clips, 1:31, 5 media items |
| 13 Music over original sound | Pass | Added audio sits at the playhead on A1 in blue over the video's own sound in orange |
| 4 Playback | Pass | 30.0 fps, 0 dropped frames, longest gap 36 ms over 8 s; play to first frame 78 ms |
| Overlapping second audio snap | Not seen | Snap rule covered by unit tests; the snap message was not captured on screen |
| 8 Missing media | Pass (Relink not driven) | Clip imported, source deleted, project reopened: the bar shows the file name with Relink and Remove; Remove took the total from 1:45.3 to 1:31.3 and Undo restored it. The bar text was cut to "is mi..." at 373 dp; it now wraps to two lines. Relink opens the system picker and was not driven |
| 10 Split-screen | Pass | App on the right half (373 dp wide, compact layout) beside another app: same project, playhead 44.6, total 1:45.3 and "saved" kept; no restart |
| 10 (pop-up window), 11, 12, 14 | Not done by hand | Pop-up window, keyboard, TalkBack and large font need hands on the device (the owner does not use keyboard or mouse) |

Bugs found and fixed during this run: dark status bar icons and a light navigation bar on a dark app (system bars now forced dark); "Detached" label unreadable over the waveform (now on a dark backing); the Projects two-pane list showed nothing when a filter matched no project (now shows "No projects match").

Decision: the preview frame updates when a trim handle is released, not during the drag. FR-018 and FR-019 ask for handles, a duration badge, and the result on release; a live frame would mean rebuilding the playback timeline on every drag step.

A/V sync within 40 ms is not measured: the Fold shows steady 30 fps, but no tool here measures audio to video offset, and no mid-range phone was available.
