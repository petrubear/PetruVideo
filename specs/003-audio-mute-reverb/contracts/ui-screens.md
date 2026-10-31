# UI Contract: Audio Tools - Mute and Reverb

Extends the 001 and 002 contracts. No designer artboard exists for Reverb; the panel follows the existing visual language and is reviewed with the designer.

## Clip tool bar

- Video clip with sound: Split, Speed, Volume, Mute (Unmute while muted), Reverb, Detach, Duplicate, Delete.
- Audio clip: Split, Speed, Volume, Mute, Reverb, Replace, Duplicate, Delete (bar scrolls sideways on compact).
- Photo and video without sound: Volume, Mute, and Reverb dimmed with a short reason when tapped.

## Reverb panel (bottom panel above the tool bar, like the 002 sheet)

- Header: title "Reverb", clip name, on/off switch, Done.
- Preset chips: Small room, Medium room, Hall, Cave, Plate; "Custom" chip appears selected when values match no preset.
- Visible sliders: Mix (0 to 100%), Decay time (0.2 to 10 s). "More" expander: Room size, Damping, Pre-delay (0 to 200 ms). Each shows its value.
- Reset returns to the selected preset (default preset when Custom).
- Events: `OpenPanel(Reverb)`, `ToggleReverb(on)`, `PickPreset(preset)`, `PreviewSetting(settings)` while dragging (heard live), `CommitSetting(settings)` on release, `Reset`, `Dismiss`.
- With the panel open the preview area shrinks as in the 002 sheet so the timeline stays visible.

## Timeline

- A clip with reverb on shows a small "Rv" badge at the top-left next to the speed and volume badges; a muted video clip shows the muted indicator used for audio clips.

## Expanded (inspector from 840 dp) and 700-839 dp

- Inspector gets Volume, Mute, and Reverb sections with all five settings; below 840 dp the bottom panel is used.

## Tabletop

- The panel sits on the controls side of the hinge; nothing interactive under the hinge.

## State

`EditorState` gains `reverbPanel: ReverbPanelState?` (clip id, reverb value, name) and `disabledTools` entries for Mute and Reverb. The open panel and its values survive configuration changes (kept in the view model).

## Accessibility

Switch, chips, and sliders expose labels, values, and stepping actions; 48 dp targets; text scales.
