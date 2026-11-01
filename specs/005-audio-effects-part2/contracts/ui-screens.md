# UI Contract: Audio Tools Part 2

Extends [003 ui-screens](../../003-audio-mute-reverb/contracts/ui-screens.md) and [002](../../002-clip-speed-volume/contracts/ui-screens.md). No designer artboard exists; controls follow the existing visual language and are reviewed with the designer.

## Clip tool bar
- Clip with sound: Split, Speed, **Audio**, Detach (when sound is attached), Duplicate, Delete. Audio opens the tool list. Volume, Mute and Reverb are no longer separate buttons.
- Photo or clip without sound: Audio is dimmed with the reason.

## Audio tool list (sheet above the tool bar; inspector section when unfolded)
- Rows, each at least 48 dp tall with a state hint: Volume (80%), Mute (On/Off), Fade (2.0 s / 1.0 s), Noise reduction (Medium/Off), Equalizer (preset name/Off), Voice effects (names of the active effects/Off), Reverb (preset/Off).
- Tapping a row opens that tool's panel in the same place; a back action returns to the list.

## Panels (same pattern as the Reverb panel: title, readout, controls, Reset, Done)
- **Fade**: Fade in and Fade out sliders (0 s to half the length), numeric readouts, Reset. A message when limited.
- **Noise reduction**: on/off switch, strength chips Low, Medium, High, note "Best for steady noise in voice".
- **Equalizer**: preset chips (Custom first when it applies), Low, Mid, High sliders (-12 dB to +12 dB), Reset.
- **Voice effects**: four sections (Echo, Robot, Pitch, Bass), each with a switch and its slider(s): Echo delay and repeats, Robot intensity, Pitch semitones, Bass amount; a section expands when its switch is on.
- Events per panel: `Open`, `Preview(value)` while dragging, `Commit(value)` on release or chip tap, `Reset`, `Dismiss`.
- Setting values stream to the preview while dragging; commit creates one undo step.

## Timeline badges
- A clip shows badges for non-neutral tools: "Fd", "Nr", "Eq", "Vx" (voice) next to speed, volume and "Rv". When they do not fit the clip's width, the extra ones collapse into "+N".
- A ramp marker is drawn at the faded end.

## Expanded (inspector, from 840 dp) and 700-839 dp
- Inspector gains sections for Fade, Noise reduction, Equalizer and Voice effects with the same controls, no sheet. At 700 to 839 dp the sheet is used inside the timeline column.

## Tabletop
- The tool list and panels fill the lower half under the hinge side; nothing interactive crosses the hinge.

## State
`EditorState` gains: the open audio tool (list, Fade, Noise, Equalizer, Voice, plus the existing Volume and Reverb), the dragged preview value, and the selected clip's fade, noise, equalizer and voice values. The open tool and preview value survive configuration changes and fold transitions.

## Accessibility
- Sliders expose value and range with stepping actions; chips and switches have labels; every touch target at least 48 dp; text scales to 2x without clipping; the list is readable in order by a screen reader.
