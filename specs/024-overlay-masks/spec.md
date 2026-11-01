# Feature Specification: Overlay Masks

**Feature Branch**: `024-overlay-masks`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "Blend modes and masks. Masks for overlay clips: Circle, Rectangle and Linear shapes with Size, Feather (soft edge) and Invert, shown live, saved with the project, one undo step, copied by split and duplicate, exported as in the preview."

## Scope note

The request was blend modes and masks. Blend modes need a compositor that can blend a layer with the picture under it; the video compositor this app uses (Media3) can only place a layer with opacity, so blend modes are not possible without replacing the compositor. They are left out and recorded as a follow-up. This feature delivers masks for overlay clips (video or photo over the main track). Not included: masks on the main track, text, or stickers; moving the mask away from the overlay's centre; several masks per clip; animated masks.

## Clarifications

### Session 2026-11-01

- Q: Where is the tool? → A: A Mask tab beside Opacity and Chroma key in the Overlay panel.
- Q: Which shapes? → A: None (default), Circle (an ellipse that follows the overlay's shape), Rectangle, and Linear (a straight edge that reveals the overlay from the left).
- Q: What do the sliders mean? → A: Size is how much of the overlay is shown (Circle and Rectangle: the size of the shape as a share of the overlay, 10 to 100; Linear: how far across the reveal reaches, 0 to 100); Feather is the softness of the edge, 0 to 100; Invert hides the inside and shows the outside.
- Q: Defaults when a shape is chosen? → A: Size 70, Feather 20, Invert off.
- Q: How does it combine with chroma key and opacity? → A: Chroma key first, then the mask, then position, size, and opacity.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Show an overlay through a shape (Priority: P1)

A user places a video over the main track as a picture-in-picture and wants it round. They open Overlay, Mask, choose Circle, and the overlay is cut to a soft-edged circle live in the preview.

**Why this priority**: This is the whole feature and the most common use.

**Independent Test**: Add an overlay, choose Circle, see the corners of the overlay disappear in the preview, export, and compare a corner pixel with the layer beneath and a centre pixel with the overlay.

**Acceptance Scenarios**:

1. **Given** a selected overlay, **When** the user opens the Mask tab, **Then** None, Circle, Rectangle, and Linear are offered with the current one marked, plus Size, Feather, and Invert.
2. **Given** Circle, **When** shown, **Then** the overlay is visible inside an ellipse centred on it and see-through outside.
3. **Given** Rectangle, **When** shown, **Then** the overlay is visible inside a centred rectangle of the chosen size.
4. **Given** Linear, **When** Size moves, **Then** the overlay is revealed from the left up to that share of its width.
5. **Given** Feather above 0, **When** shown, **Then** the edge fades softly over that range.
6. **Given** Invert, **When** on, **Then** the inside is hidden and the outside shown.
7. **Given** any committed change, **When** made, **Then** it is one undo step; sliders show live while moving.

---

### User Story 2 - Kept, saved, exported, combined (Priority: P2)

The mask is saved with the project, copied by clip split and duplicate, kept by project duplicate, and exported as in the preview, together with chroma key, position, size, opacity, and keyframes.

**Independent Test**: Choose Circle, split the overlay, save and reopen, export, and compare a frame with the preview.

**Acceptance Scenarios**:

1. **Given** a masked overlay, **When** closed and reopened or duplicated, **Then** the mask is unchanged.
2. **Given** a masked overlay, **When** split or duplicated, **Then** each part keeps the mask.
3. **Given** a masked and keyed overlay, **When** shown, **Then** the key applies first and the mask after it.
4. **Given** an export, **When** a frame is compared with the preview, **Then** they match within a small visual difference.

---

### Edge Cases

- With None, the overlay looks and exports exactly as before.
- Size 100 on Circle still cuts the corners (an inscribed ellipse); Rectangle at 100 shows everything.
- Photos behave like videos.
- Undo and redo restore the exact earlier values.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Each overlay clip MUST have a mask: shape None, Circle, Rectangle, or Linear; Size 0 to 100 (default 70); Feather 0 to 100 (default 20); Invert (default off).
- **FR-002**: With a shape chosen, the overlay MUST be see-through outside the shape (inside, with Invert), in the preview and the export, applied after chroma key and before position, size, and opacity.
- **FR-003**: Feather MUST soften the edge over a width proportional to its value; at 0 the edge is hard.
- **FR-004**: With None, an overlay MUST look and export exactly as before.
- **FR-005**: Slider and chip changes MUST show live and each commit MUST be one undo step.
- **FR-006**: The mask MUST be saved with the project, kept by project duplicate, and copied by clip split and duplicate.
- **FR-007**: The Mask tab MUST work in all layouts, meet touch-target and contrast rules, and have accessible labels.

### Key Entities *(include if feature involves data)*

- **Overlay mask**: An overlay's shape, size, feather, and invert flag.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can make an overlay round in 3 taps or fewer after selecting it (Mask tab, Circle, done).
- **SC-002**: In an export, a pixel in the corner of a Circle-masked overlay shows the layer beneath and a pixel at its centre shows the overlay, within a small tolerance.
- **SC-003**: With None, an exported frame is identical to the export before this feature.
- **SC-004**: 100% of mask settings survive close and reopen, undo, redo, split, duplicate, and project duplicate in automated tests.

## Assumptions

- Shapes are centred on the overlay's picture and follow its aspect; moving or rotating the mask is out of scope.
- Feather width is up to a quarter of the overlay's shorter side.
- Blend modes are a separate, larger feature (custom compositor) and are recorded as a follow-up.
- Fold, device, and designer reviews follow the earlier features and are done after the build.
