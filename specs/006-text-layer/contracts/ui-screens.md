# UI Contract: Text Layer

Extends [001](../../001-project-media-timeline/contracts/ui-screens.md) to [005](../../005-audio-effects-part2/contracts/ui-screens.md). No designer artboard has Text panels; they follow the existing visual language and are reviewed with the designer.

## Main tool bar
- The Text tab is active. With it open and nothing selected, the bar shows **Add text**.

## Clip tool bar (selected text clip)
Edit, Style, Animation, Split, Duplicate, Delete (scrolls sideways at compact width; every button at least 48 dp).

## Panels (above the clip tool bar; inspector sections when unfolded; lower half in tabletop)
- **Edit**: multi-line text field with the keyboard, character count, Done. The preview shrinks while typing.
- **Style**: Font chips, Size slider, Color swatches (12) plus custom, Alignment segmented control, Background switch with color and opacity, Outline switch with color and width, Reset style.
- **Animation**: In and Out chips (None, Fade, Slide, Pop) with a length slider each.
- Events: `OpenTextTool(kind)`, `TextPreview(clip)` while dragging, `TextCommit(clip)` on release or tap, `TextReset(kind)`, `AddText`, `TextDismiss`.

## Preview layer
- The selected text clip shows a selection box; drag moves it, pinch scales it, tap selects a clip under the finger; center guides appear within 8 dp of the center and the text stays inside the frame margin.

## Timeline
- A Text lane under the video lane (and above the audio lane), row height 40 dp; overlapping clips use extra rows; each clip shows its first line of text; handles to trim; long press to move.

## Tabletop and expanded
- Panels sit below the hinge in tabletop; nothing interactive crosses the hinge. At 840 dp and up the inspector hosts the same sections; the open panel and typed text survive fold, unfold and rotation.

## Accessibility
- The text field, sliders, chips, switches and swatches have labels and values; touch targets at least 48 dp; text scales to 2x; the lane clip announces "Text, <words>, starts at, lasts".
