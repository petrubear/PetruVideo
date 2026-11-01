# Feature Specification: Stickers and Shapes

**Feature Branch**: `020-stickers-shapes`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "Stickers and shapes: from the Text tab the user opens Stickers, a gallery of emoji stickers (faces, hearts, celebration, nature, symbols) and simple shapes (circle, square, triangle, star, heart, arrow, check, cross), and taps one to put it on the video at the playhead. It is a layer like a title: it can be moved, resized, recoloured (shapes), faded in and out, given keyframes, trimmed, split, duplicated, and deleted, shows in the preview, is saved with the project, and is exported with the video."

## Scope note

A sticker or shape is a text layer whose content is a single symbol, so it uses everything text layers already have: place, size, colour, outline, background, animations, keyframes, timing, split, duplicate, save, and export. Not included: drawing freehand, stretching a shape to a different width and height, gradients, rotation by an angle, and downloadable sticker packs (the gallery is built in).

## Clarifications

### Session 2026-11-01

- Q: How are stickers and shapes drawn? → A: As symbols from the phone's own fonts: colour emoji for stickers, and simple symbol glyphs for shapes. Emoji keep their own colours; shapes take the layer's text colour.
- Q: Where is it in the UI? → A: A Stickers button on the Text tab opens a panel with the gallery. After a sticker is added the layer is selected and the usual text-layer tools are offered.
- Q: What is the default size and length? → A: Centered in the frame, large (about a fifth of the frame height), 3 seconds, starting at the playhead, shortened to fit the project end.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Add a sticker or shape (Priority: P1)

A user opens the Text tab, taps Stickers, picks a category, and taps a sticker or shape. It appears centered at the playhead for 3 seconds, selected, with its handles in the preview. Playing shows it over the video.

**Why this priority**: This is the whole feature.

**Independent Test**: Add a star and a party popper at different times, play, export, and see both in their windows.

**Acceptance Scenarios**:

1. **Given** the Text tab, **When** the user taps Stickers, **Then** a gallery opens with categories Faces, Hearts, Celebrate, Nature, Symbols, and Shapes, each item large enough to tap (48 dp) and labelled for screen readers.
2. **Given** the gallery, **When** the user taps an item, **Then** a layer is added at the playhead, selected, centered, about a fifth of the frame high, for 3 seconds (less if the project ends sooner), in one undo step.
3. **Given** a shape, **When** it is added, **Then** it has a vivid default colour; emoji keep their own colours.
4. **Given** the layer, **When** the user drags or pinches it in the preview, **Then** it moves and resizes like a title.
5. **Given** the gallery is open, **When** the user adds several stickers in a row, **Then** each is its own layer and the gallery stays open until closed.

---

### User Story 2 - Style and time it (Priority: P2)

With a sticker or shape selected the user can change its colour (shapes), add an outline or box, set in and out animations, add keyframes, and move or trim it on the text lane, as for a title.

**Why this priority**: Control over look and timing is what makes stickers useful.

**Independent Test**: Add an arrow, make it red with a pop-in, keyframe it across the frame, trim it, split it, and export.

**Acceptance Scenarios**:

1. **Given** a selected shape, **When** the user changes its colour, **Then** the shape changes colour in the preview; an emoji does not.
2. **Given** a layer, **When** the user uses animations, keyframes, move, trim, split, or duplicate, **Then** they work exactly as for a title.
3. **Given** a layer, **When** its words are opened in the text editor, **Then** the symbol is shown as its content and can be replaced by another symbol.

---

### User Story 3 - Kept and exported (Priority: P3)

Stickers and shapes are saved with the project, kept by project duplicate, and exported exactly as in the preview. They work in the unfolded and tabletop layouts.

**Why this priority**: Persistence and export rules apply.

**Independent Test**: Add stickers, close and reopen, duplicate the project, export, and compare a frame with the preview.

**Acceptance Scenarios**:

1. **Given** stickers, **When** the project is reopened or duplicated, **Then** they are unchanged.
2. **Given** an export, **When** a frame with a sticker is compared with the preview, **Then** the sticker is in the same place and size within a small difference.

---

### Edge Cases

- A phone that lacks a symbol in its fonts shows an empty box for it; the gallery only offers symbols the phone can draw (each is checked when the gallery opens and unavailable ones are hidden).
- Colour emoji ignore the text colour; outline and box still apply.
- A shape is not stretched: resizing changes its size only.
- The gallery works in portrait, unfolded, and tabletop; it never covers the preview's selected layer.
- A layer longer than the project end is limited as titles are.
- Undo removes the layer in one step; redo restores it.
- Auto captions' Replace never removes stickers (they are not auto captions).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The Text tab MUST offer Stickers, opening a gallery with the categories Faces, Hearts, Celebrate, Nature, Symbols, and Shapes.
- **FR-002**: Tapping an item MUST add a text layer holding that symbol at the playhead, centered, selected, of about a fifth of the frame height and 3 seconds (limited to the project end), as one undo step.
- **FR-003**: Shapes MUST get a vivid default colour; emoji MUST keep their own colours.
- **FR-004**: A sticker or shape MUST support everything a text layer supports: move, resize, colour, outline, box, animations, keyframes, trim, split, duplicate, delete, save, and export.
- **FR-005**: The gallery MUST only offer symbols the phone can draw, MUST stay open while the user adds several, and MUST work in the portrait, unfolded, and tabletop layouts with touch targets of at least 48 dp and screen reader labels.
- **FR-006**: Stickers and shapes MUST NOT be treated as auto captions.

### Key Entities *(include if feature involves data)*

- **Sticker**: A symbol (emoji or shape glyph) with a label and a category, offered in the gallery and added as a text layer.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can add a sticker in 3 taps or fewer from the Text tab.
- **SC-002**: Every offered symbol draws as a visible picture (not an empty box) on the test phone in an automated check.
- **SC-003**: An exported frame matches the preview frame with a sticker within a small visual difference.
- **SC-004**: 100% of stickers survive close and reopen, undo, redo, and project duplicate in automated tests.

## Assumptions

- Stickers are symbols from the phone's own fonts, so the look follows the phone's emoji set; a bundled sticker pack is a later feature.
- Shapes are glyphs (circle, square, triangle, star, heart, arrow, check, cross); they are not stretched.
- Fold, device, and designer reviews follow the earlier features and are done after the build, not before.
- Existing limits apply: undo depth, project length, and preview-quality rules stay as they are.
