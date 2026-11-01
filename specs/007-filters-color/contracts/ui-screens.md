# UI Contract: Filters and Color Adjustments

- **Main tool bar**: the Filters tab is active; with a video or photo clip selected the clip tool bar offers Filters and Adjust (and the usual tools). With nothing selected the Filters tab shows a hint "Select a clip".
- **Filters panel**: a row of tiles (None, Vivid, Warm, Cool, Black and white, Vintage, Faded) on the selected clip's frame at the playhead, an Intensity slider, Apply to all, Remove, Done.
- **Adjust panel**: Brightness, Contrast, Saturation, Warmth, Vignette sliders with numbers, Reset, Done.
- Events: `OpenFilterTool(kind)`, `FilterPreview(filter)` while dragging, `FilterCommit(filter)` on release or tap, `FilterReset`, `FilterApplyAll`, `FilterRemove`, `FilterDismiss`.
- **Timeline**: "Fl" badge with the other badges.
- **Expanded**: inspector sections with the same controls; **tabletop**: panels below the hinge; state survives fold and rotation.
- **Accessibility**: tiles and sliders have labels and values; 48 dp targets; 2x font.
