# UI Contract: Speed Ramps

- **Speed curve tool** (main-track video clip): opens a panel (phone sheet, unfolded inspector section, tabletop lower half) with six preset chips, the graph, point buttons, the length readout, and Reset.
- **Graph**: x is the clip as played, y the speed on a log scale from 0.1x to 10x with marks at 0.1x, 1x, 10x; points are 48 dp touch targets; tapping empty graph adds a point; dragging moves a point (time and speed) and updates the readout; release applies the change as one undo step.
- **Accessibility**: each point has a label ("Point 2, at 3.0 seconds, 0.5x") and custom actions to nudge or delete it; the readout is a live region.
- **Unavailable**: photos, freeze stills, missing media: the tool is dimmed with a reason. The constant Speed tool is dimmed while a curve is on.
