# Feature Specification: Clip Transforms

**Feature Branch**: `016-clip-transforms`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "Clip transforms: on a selected video or photo clip the user can rotate it in 90 degree steps, flip it horizontally or vertically, and crop it to a chosen part of the picture (free or to a preset shape: 1:1, 4:5, 9:16, 16:9, 4:3). The picture is fitted to the project frame after the transform, so a rotated or cropped clip fills the frame without distortion. Transforms apply to the whole clip, show live in the preview, are saved with the project, are exported exactly as in the preview, follow split, duplicate, trim, speed changes and relink, and work together with the clip's keyframes (zoom, pan, fade), filter, effect and transitions. Each change is one undo step and there is a Reset."

## Scope note

A video or photo clip on the main track can be turned, mirrored, and cut down to part of its picture. The result is placed in the project frame the same way every clip is placed today: as large as it fits without stretching. Transforms are one setting for the whole clip. Not included: rotation by other angles, transforms that change over time, crops that differ between moments, and transforms for overlay clips.

## Clarifications

### Session 2026-11-01

- Q: In what order are the changes applied? → A: Rotate and flip first, then crop of the turned picture, then fit to the frame, then the clip's keyframes (zoom, pan, fade), filter, and effect as today.
- Q: How does a crop relate to the frame? → A: The cropped part is fitted into the project frame as large as it fits, without stretching; the frame shape does not change. If the shapes differ the usual bars show, exactly as for a clip of that shape today.
- Q: What are the crop choices? → A: Free (drag the edges and corners) or a preset shape (1:1, 4:5, 9:16, 16:9, 4:3) that keeps its shape while the box is moved and resized; Full removes the crop.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Rotate and flip a clip (Priority: P1)

A user selects a clip that was filmed sideways or needs mirroring, taps Transform, then Rotate or Flip. The preview shows the new orientation at once and the clip fits the frame. Tapping Rotate turns it a quarter turn each time; Flip horizontal and Flip vertical mirror it.

**Why this priority**: Sideways and mirrored footage is the most common problem and the cheapest to fix.

**Independent Test**: Add a landscape clip, rotate it a quarter turn, see it fitted in the frame as a portrait picture, flip it, export, and compare a frame with the expected orientation.

**Acceptance Scenarios**:

1. **Given** a selected clip, **When** the user taps Rotate, **Then** the picture turns a quarter turn clockwise in the preview and fits the frame with no stretching.
2. **Given** a rotated clip, **When** the user taps Rotate three more times, **Then** it is back to its original orientation.
3. **Given** a selected clip, **When** the user taps Flip horizontal or Flip vertical, **Then** the picture is mirrored accordingly, and tapping it again undoes the mirror.
4. **Given** any transform change, **When** it is made, **Then** it is one undo step.
5. **Given** a transformed clip, **When** the user taps Reset, **Then** rotation, flips, and crop return to none in one undo step.

---

### User Story 2 - Crop a clip (Priority: P2)

A user taps Crop and sees the picture with a box over it. They drag the edges or corners to choose the part to keep, or pick a preset shape. The preview shows the kept part fitted in the frame. Full removes the crop.

**Why this priority**: Cropping is the second most common fix, after orientation.

**Independent Test**: Crop a clip to the right half, see it fitted in the frame; pick 1:1 and move the box; export and compare a frame with the expected part.

**Acceptance Scenarios**:

1. **Given** a selected clip, **When** the user opens Crop, **Then** a box over the whole picture appears, with handles at least 48 dp to touch.
2. **Given** the crop box, **When** the user drags an edge or corner in Free mode, **Then** the box follows and stays inside the picture and at least 10% of the picture wide and tall.
3. **Given** a preset shape, **When** the user picks it, **Then** the box takes that shape, as large as fits and centered, and keeps the shape while it is moved or resized.
4. **Given** a crop, **When** the user confirms, **Then** the preview and the clip show only the kept part, fitted in the frame without stretching.
5. **Given** a rotated or flipped clip, **When** the user crops, **Then** the box is drawn on the picture as it now looks.
6. **Given** a crop, **When** the user taps Full, **Then** the crop is removed.

---

### User Story 3 - Kept, saved, exported, and combined (Priority: P3)

Rotation, flips, and crop stay right when the clip is split, duplicated, trimmed, has its speed changed, or is relinked; are saved with the project; and appear in an export exactly as in the preview. They work with keyframes, filters, effects, and transitions, and in the unfolded and tabletop layouts.

**Why this priority**: Persistence, edit consistency, and combination with earlier features apply to this clip setting.

**Independent Test**: Rotate and crop a clip, add a push-in keyframe and a filter, split it, change the speed, save and reopen, unfold, export, and compare frames.

**Acceptance Scenarios**:

1. **Given** a transformed clip, **When** it is split or duplicated, **Then** every resulting clip keeps the transform.
2. **Given** a transformed clip, **When** it is trimmed, has its speed changed, is moved, or is relinked to another file of the same shape, **Then** the transform is kept.
3. **Given** a project with transforms, **When** it is closed and reopened, **Then** they are unchanged.
4. **Given** a transformed clip with keyframes, a filter, an effect, or a transition, **When** the project plays, **Then** keyframes zoom and pan the transformed picture, and the filter, effect, and transition apply to it.
5. **Given** an exported video, **When** a frame is compared with the preview at the same time, **Then** orientation, crop, and placement match within a small visual difference.

---

### Edge Cases

- A crop changes how large the clip is in the frame because the kept part has its own shape; the frame shape stays the same.
- Rotating a cropped clip rotates the picture together with its crop box so the same content stays kept.
- Flipping a cropped clip mirrors the crop box with the picture, so the same content stays kept.
- Relinking a clip to a file with a different shape keeps the rotation and flips and resets the crop, with a short message.
- Photo clips behave the same as video clips.
- A very small crop (the minimum) is still sharp enough to export; no minimum resolution is promised for tiny crops of low-resolution media.
- Clips without a picture (missing media) show the usual placeholder; transforms are kept.
- Reduced preview quality does not change the transform.
- Templates fill clips with no transform; project duplicate keeps each clip's.
- The Transform tool is offered for main-track clips only.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Each main-track video or photo clip MUST have a transform: a quarter-turn rotation (0, 90, 180, or 270 degrees), a horizontal flip, a vertical flip, and an optional crop box (default none).
- **FR-002**: Users MUST be able to rotate by a quarter turn, toggle each flip, and set or remove the crop, each as one undo step.
- **FR-003**: The crop box MUST stay inside the picture and be at least 10% of the turned picture wide and tall; preset shapes (1:1, 4:5, 9:16, 16:9, 4:3) MUST keep their shape while moved and resized.
- **FR-004**: The transformed picture MUST be fitted into the project frame as large as it fits without stretching, with the usual bars when shapes differ.
- **FR-005**: The order MUST be rotate and flip, then crop, then fit, then the clip's keyframes, filter, and effect.
- **FR-006**: The preview MUST show a rotation or flip at once when it is made; while the crop box is dragged the crop screen MUST show the box on the picture, and the preview MUST show the cropped result as soon as the crop is confirmed.
- **FR-007**: Export MUST render the same orientation, crop, and placement as the preview.
- **FR-008**: A clip with no transform MUST look and export exactly as before.
- **FR-009**: Split, duplicate, trim, move, speed change, delete, relink, project duplicate, and undo/redo MUST keep each clip's transform; relinking to a different shape MUST reset only the crop.
- **FR-010**: The transform MUST be saved with the project and survive close, reopen, and process death.
- **FR-011**: The transform MUST work with the clip's keyframes, filter, effect, and transitions.
- **FR-012**: A Reset in the Transform tool MUST return rotation, flips, and crop to none in one undo step.
- **FR-013**: All new controls MUST work in the portrait, unfolded, and tabletop layouts, meet touch-target and contrast rules, and have accessible labels, including the crop box handles.

### Key Entities *(include if feature involves data)*

- **Clip transform**: A clip's rotation, flips, and optional crop box, applied to its picture before it is fitted to the frame.
- **Crop box**: A rectangle inside the turned picture, stored as shares of its width and height.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can rotate a sideways clip into the right orientation in under 10 seconds from selecting it.
- **SC-002**: A user can crop a clip to a chosen part in under 30 seconds.
- **SC-003**: An exported frame matches the preview frame at the same time within a small visual difference in an automated comparison, for rotation, flip, crop, and all three combined.
- **SC-004**: In no tested case is the picture stretched: the kept part keeps its shape within 1% in the exported frame.
- **SC-005**: 100% of transforms survive close and reopen, split, duplicate, trim, speed change, undo, and redo in automated tests.
- **SC-006**: The preview keeps its normal responsiveness with a transformed clip across a transition; the playhead never stalls longer than 0.5 s.
- **SC-007**: Every transform control is reachable in 3 taps or fewer from a selected clip.

## Assumptions

- The transform is one setting for the whole clip and does not change over time.
- Quarter-turn rotation is clockwise.
- The crop box is edited on a still of the clip's first visible frame; the confirmed crop applies to the whole clip.
- A cropped clip is fitted into the frame like any clip of that shape, so no extra "fill" mode is added.
- Fold, device, and designer reviews follow the earlier features and are done after the build, not before.
- The small pictures on the timeline keep showing the clip's original picture; the preview and the export show the transform.
- Existing limits apply: undo depth, project length, and preview-quality rules stay as they are.
