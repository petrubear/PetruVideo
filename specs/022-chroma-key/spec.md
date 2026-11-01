# Feature Specification: Chroma Key

**Feature Branch**: `022-chroma-key`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "Chroma key: the user selects an overlay clip and opens a Chroma key tool to remove a background colour so the layer under it shows through. They pick the colour to remove (green and blue presets, or tap a point on the preview to sample it), and adjust Strength, Smoothness and Spill. The preview updates live; a toggle shows the result against a checkerboard. The effect applies to the overlay clip only, is saved with the project, follows undo as one step, copies with duplicate and split, and is exported with the video."

## Scope note

Chroma key for overlay-lane clips (a video or photo placed over the main track). One key colour per clip, with three sliders. Not included: keying the main track, masks, several key colours on one clip, and automatic colour detection.

## Clarifications

### Session 2026-11-01

- Q: Which clips can be keyed? → A: Overlay clips only; the main track has nothing under it to show through except the canvas background.
- Q: Where is the tool? → A: With an overlay clip selected, a Chroma key button on the overlay tool bar opens a panel.
- Q: What is the default when turned on? → A: Green, Strength 40, Smoothness 20, Spill 50, so one tap gives a usable result on a typical green screen.
- Q: How is the colour sampled? → A: A Pick button lets the user tap the preview; the tapped pixel of the overlay's own picture becomes the key colour.
- Q: What does the checkerboard do? → A: A toggle in the panel that shows a checkerboard behind the overlay in the preview only; it never changes the export.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Remove a green screen from an overlay (Priority: P1)

A user places a clip shot on a green screen over their main video. They select it, open Chroma key, turn it on, and the green disappears so the main video shows through. They adjust Strength until the edges look right.

**Why this priority**: This is the whole feature.

**Independent Test**: Add an overlay of a green-backed picture over a main clip, turn Chroma key on, see the main clip through the green area in the preview, export, and compare an area that was green with the picture underneath.

**Acceptance Scenarios**:

1. **Given** a selected overlay clip, **When** the user taps Chroma key, **Then** a panel with an on/off switch, Green and Blue presets, a Pick button, and Strength, Smoothness and Spill sliders opens.
2. **Given** the panel, **When** the user turns it on, **Then** the preview shows the layer under the overlay wherever the overlay is close to the key colour.
3. **Given** a keyed clip, **When** Strength goes up, **Then** a wider range of colours near the key colour becomes see-through; at 0 only the exact colour does.
4. **Given** a keyed clip, **When** Smoothness goes up, **Then** the edge between kept and removed areas gets softer.
5. **Given** a keyed clip, **When** Spill goes up, **Then** the tint of the key colour left on the kept edges is reduced.
6. **Given** any change, **When** it is made, **Then** it is one undo step and shows live while a slider moves.

---

### User Story 2 - Pick the colour and check the result (Priority: P2)

The user's backdrop is not pure green or blue. They tap Pick and then a point on the preview, and that colour becomes the key. They turn on the checkerboard to see which areas are see-through.

**Why this priority**: Real footage rarely matches a preset.

**Independent Test**: Key an overlay with a teal backdrop by tapping it, switch the checkerboard on, and see the checkerboard where the backdrop was.

**Acceptance Scenarios**:

1. **Given** the panel, **When** the user taps Pick and then the preview, **Then** the key colour becomes the colour of the overlay's picture at that point and the preview updates.
2. **Given** the checkerboard toggle is on, **When** the overlay is keyed, **Then** the see-through areas show a checkerboard in the preview only.
3. **Given** the checkerboard toggle, **When** the project is exported, **Then** the export has no checkerboard.

---

### User Story 3 - Kept, saved, exported, combined (Priority: P2)

The key settings are saved with the project, copied by duplicate and split, and appear in an export as in the preview, together with the overlay's position, size, opacity, keyframes, and fade.

**Why this priority**: Persistence and export parity.

**Independent Test**: Key an overlay, split it, save, reopen, export, and compare a frame with the preview.

**Acceptance Scenarios**:

1. **Given** a keyed overlay, **When** the project is closed and reopened or duplicated, **Then** the key settings are unchanged.
2. **Given** a keyed overlay, **When** it is split or duplicated, **Then** each part keeps the key settings.
3. **Given** a keyed overlay with opacity below 100 or keyframes, **When** shown, **Then** the key applies first and opacity and position apply after it.
4. **Given** an export, **When** a frame is compared with the preview at the same time, **Then** they match within a small visual difference.

---

### Edge Cases

- A clip with Chroma key off looks and exports exactly as before.
- Strength 100 removes most of the picture; the panel still allows it.
- A photo overlay can be keyed like a video.
- Picking a point outside the overlay's picture does nothing.
- The key applies to the overlay's picture only, not to text or stickers.
- Replacing or relinking the overlay's media keeps the key settings.
- Undo and redo restore the exact earlier values.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Each overlay clip MUST have chroma key settings: on/off, a key colour (default green), Strength 0 to 100, Smoothness 0 to 100, and Spill 0 to 100.
- **FR-002**: With the key on, pixels of the overlay whose colour is near the key colour MUST become transparent so the layer under shows through, in the preview and the export.
- **FR-003**: Strength MUST widen the range of removed colours, Smoothness MUST soften the edge, and Spill MUST reduce the key colour's tint on kept pixels.
- **FR-004**: The panel MUST offer Green and Blue presets and a Pick tool that samples the overlay's picture at a tapped point.
- **FR-005**: The panel MUST offer a preview-only checkerboard toggle that never affects the export.
- **FR-006**: Slider and preset changes MUST show live, and each committed change MUST be one undo step.
- **FR-007**: The settings MUST be saved with the project, kept by project duplicate, copied by clip duplicate and split, and survive media relink.
- **FR-008**: A clip with the key off MUST look and export exactly as before this feature.
- **FR-009**: The panel MUST work in the portrait, unfolded, and tabletop layouts, meet touch-target and contrast rules, and have accessible labels.

### Key Entities *(include if feature involves data)*

- **Chroma key**: An overlay clip's key settings: on/off, key colour, Strength, Smoothness, Spill.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can key a green-screen overlay with 3 taps or fewer after selecting it (Chroma key, switch on, done).
- **SC-002**: In an export, pixels that were the pure key colour show the layer beneath within a small tolerance, and pixels far from the key colour are unchanged.
- **SC-003**: With the key off, an exported frame is identical to the export before this feature in an automated comparison.
- **SC-004**: 100% of settings survive close and reopen, undo, redo, split, duplicate, and project duplicate in automated tests.

## Assumptions

- The key works in colour space distance (chroma), so brightness differences in the backdrop matter less than hue.
- Strength maps to a distance of 0 to about 0.5 in chroma space, Smoothness to an edge width of 0 to about 0.2.
- Spill removes the key colour's channel excess from kept pixels; it never changes pixels far from the key.
- The key is applied in the overlay's own picture step, before position, size, and opacity.
- Fold, device, and designer reviews follow the earlier features and are done after the build.
- Existing limits apply: undo depth, project length, and preview-quality rules stay as they are.
