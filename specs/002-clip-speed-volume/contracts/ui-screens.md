# UI Contract: Clip Speed and Volume

Extends [001 ui-screens](../../001-project-media-timeline/contracts/ui-screens.md). Only changes are listed. No designer artboard exists for these controls; they follow the existing visual language and are reviewed with the designer.

## Clip tool bar (compact and tabletop)

- Video clip: Split, **Speed**, **Volume**, Detach (when sound is attached), Duplicate, Delete. Speed and Volume are now active. Volume is dimmed for a clip without sound or with detached sound.
- Photo clip: Split, Duplicate, Delete; Speed and Volume dimmed.
- Audio clip: Split, **Speed**, **Volume**, Mute, Replace, Duplicate, Delete. The bar scrolls sideways at compact width (it already does).

## Speed and Volume bottom sheet

- Opens above the clip tool bar; timeline and preview stay visible and interactive.
- Contents: title (Speed or Volume), clip name, large numeric readout ("1.50x", "80%"), slider, and for Speed quick choices 0.25x, 0.5x, 1x, 2x, 4x; a Reset action; a Done action.
- Volume slider: 0% to 200%; the part above 100% is drawn in a boost color with a label.
- Speed slider: logarithmic feel (equal steps per doubling), 0.25x to 4x, snapping to the quick choices.
- Events: `OpenSheet(tool)`, `PreviewValue(value)` while dragging, `CommitValue(value)` on release or a quick choice, `Reset`, `Dismiss`.
- Volume streams the live value to the preview during `PreviewValue`; speed shows the new length in the readout and commits on release.
- Notices: a limit message ("Limited to 0.8x to avoid overlapping the next audio clip") appears as a snackbar-style message above the tool bar.

## Timeline

- A clip whose speed is not 1x or whose volume is not 100% shows a small badge ("2x", "40%") at its top-left; both badges can show together.
- Duration badge on a selected clip shows timeline length (post-speed).

## Expanded (inspector, from 840 dp) and 700-839 dp

- Inspector gains Speed and Volume sections with the same slider and quick choices, no sheet.
- At 700 to 839 dp (no inspector) the bottom sheet is used, placed inside the timeline column.

## Tabletop

- The sheet fills the lower half under the hinge side; nothing interactive crosses the hinge.

## State

`EditorState` gains: `sheet: SheetState?` (tool, preview value), `selectedSpeedMilli`, `selectedVolumePercent`, `canSetVolume`, `canSetSpeed`. The open sheet and its preview value survive configuration changes and fold transitions (kept in the view model / saved state).

## Accessibility

- Sliders expose value and range to screen readers with stepping actions; quick choices are buttons with labels; every touch target is at least 48 dp; text scales.
