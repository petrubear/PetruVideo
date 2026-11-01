# UI Contracts: Keyframes

## Clip tool
Overlay and text clips get a Keyframes tool in the clip tool bar. It opens the Keyframes panel above the bar.

## Keyframes panel
Buttons: Add keyframe, Previous, Next (jump to the neighbour keyframe), Copy here, Delete. A Linear or Ease choice for the selected keyframe. A readout "Keyframe 2 of 3" or "No keyframes". Delete and Copy here are disabled with no keyframe selected. Done closes the panel.

## Timeline diamonds
On the selected clip, a diamond at each keyframe time, 48 dp touch target. Tap selects and moves the playhead to it. Drag moves it within its neighbours and snaps to the playhead within 8 dp. The selected diamond is filled with the accent color.

## Preview box
Dragging or pinching the selected clip on the preview commits `SetPoseAt` at the playhead when the clip has keyframes, otherwise it edits the plain values as before. Opacity sliders follow the same rule.

## Layouts
Compact: panel above the tool bar. Unfolded: the same controls in the inspector section. Tabletop: panel on the controls half. All targets 48 dp, with accessible labels ("Keyframe at 1.2 seconds, selected").
