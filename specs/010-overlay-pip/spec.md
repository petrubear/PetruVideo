# Feature Specification: Overlay (Picture in Picture)

**Feature Branch**: `010-overlay-pip`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "Overlay / PiP: a second video or photo layer over the main video, with position, size, and opacity, set by dragging and pinching on the preview, timed on the timeline, live in the preview and respected by export."

## Scope note

The Overlay tab of the main tool bar, disabled until now, becomes active. An overlay is a video or photo shown over the main video for a stretch of the timeline. Overlays have no sound of their own and play at normal speed. Not included: overlay sound, speed, rotation, crop or shapes, filters on overlays, keyframed movement, and green-screen removal.

## Clarifications

### Session 2026-11-01

- Q: Do overlays bring their own sound? → A: No; overlays are silent, and the main video's sound is unchanged.
- Q: Can overlays overlap each other in time? → A: Yes; later clips are drawn over earlier ones, and rows in the lane show them separately.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Add a picture-in-picture clip (Priority: P1)

A user opens the Overlay tab, taps Add overlay, picks a video or photo from the phone, and it appears over the main video at the playhead as a small picture in the upper right, with its clip in the Overlay lane. Playing the project shows it over the main video during its time.

**Why this priority**: This is the feature's core.

**Independent Test**: Add an overlay at 3 s, play across it and see it appear and disappear over the main video, undo and see it gone.

**Acceptance Scenarios**:

1. **Given** a project, **When** the user taps Add overlay and picks a video or photo, **Then** an overlay clip starts at the playhead (a photo lasts 5 s; a video lasts its own length up to 10 s), is selected, and shows in the preview over the main video.
2. **Given** an audio file is picked, **When** it cannot be used, **Then** a short message says so and nothing is added.
3. **Given** the overlay extends past the end of the main video, **When** the project plays, **Then** the project length follows the overlay and the main video shows black after its end.
4. **Given** the main video has no clips yet, **When** an overlay is added, **Then** it plays over black.

---

### User Story 2 - Place, resize, and fade it (Priority: P1)

A user drags the overlay on the preview to move it, pinches to resize it, and sets its opacity from a panel. A guide shows when it is centered. Position and size are shares of the frame, so exports look the same at any size.

**Why this priority**: Without placement an overlay is of little use.

**Independent Test**: Drag to the lower left, pinch larger, set opacity to 50%, export and see the same place and size.

**Acceptance Scenarios**:

1. **Given** a selected overlay, **When** the user drags it on the preview, **Then** it follows the finger live, snaps to the horizontal and vertical center within 8 dp with a guide, and stays inside the frame.
2. **Given** a selected overlay, **When** the user pinches, **Then** its width changes live between 5% and 100% of the frame width, keeping its shape.
3. **Given** the overlay panel, **When** the user drags Opacity, **Then** the preview changes live from 0% to 100%.
4. **Given** one gesture, **When** it ends, **Then** it is one undo step.

---

### User Story 3 - Time it on the timeline (Priority: P2)

A user sees overlay clips in an Overlay lane under the video lane, drags a clip to move it in time, trims its ends, splits it at the playhead, duplicates it, and deletes it. Overlapping clips use separate rows.

**Why this priority**: Timing matters but the default placement already works.

**Independent Test**: Add two overlays that overlap, move and trim them, split one, delete one, undo each.

**Acceptance Scenarios**:

1. **Given** the lane, **When** a clip is long-pressed and dragged, **Then** its start changes (never below 0) and the project length follows.
2. **Given** a selected clip, **When** its handles are dragged, **Then** a video overlay stays inside its file (the start moves its source start) and a photo can be any length from 0.5 s.
3. **Given** a selected clip, **When** Split, Duplicate, or Delete is used, **Then** they work as for video clips, keeping position, size, and opacity on both parts.

---

### User Story 4 - Layouts, saved, exported (Priority: P3)

The overlay tools work unfolded (inspector) and in tabletop; overlays are saved with the project and exported as in the preview.

**Why this priority**: Layout, persistence, and export rules from earlier features apply.

**Independent Test**: Place an overlay, save, force-close, reopen; unfold and half-fold; export and compare a frame to the preview.

**Acceptance Scenarios**:

1. **Given** the unfolded editor, **When** an overlay is selected, **Then** the inspector hosts the opacity control with the same effect as the panel.
2. **Given** tabletop posture, **When** the panel is open, **Then** it is below the hinge and nothing interactive is under it.
3. **Given** a project with overlays, **When** exported, **Then** each overlay appears at the same time, relative place, relative size, and opacity as in the preview, at any export size.
4. **Given** the app was force-closed, **When** reopened, **Then** overlays are as set (at most 5 seconds lost).

---

### Edge Cases

- An overlay whose source file is missing: it is skipped in preview and export, stays in the lane, and can be relinked or removed with the missing-media tools.
- An overlay of another shape than the frame: it keeps its own shape; width is a share of the frame width.
- Many overlays at once on a mid-range device: preview holds 30 fps or the app lowers preview quality and says so; up to 3 visible at once are supported.
- Removing the main video clips does not remove overlays.
- A trim that would go past a video overlay's file is limited to the file.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The Overlay tab MUST be active and offer Add overlay, which lets the user pick a video or photo file.
- **FR-002**: An added overlay MUST start at the playhead, last 5 s for a photo and the file's length up to 10 s for a video, and play silently at normal speed.
- **FR-003**: Audio files and missing files MUST be refused with a message.
- **FR-004**: Users MUST be able to move an overlay on the preview by dragging, resize it by pinching (width 5% to 100% of the frame width), and set its opacity (0% to 100%); position and size MUST be kept as shares of the frame, with snapping to the center and a guide.
- **FR-005**: Changes to place, size, and opacity MUST be seen in the preview within 100 ms and each gesture MUST be one undo step.
- **FR-006**: The timeline MUST show an Overlay lane under the video lane; overlapping clips MUST be on separate rows; later clips draw over earlier ones.
- **FR-007**: Users MUST be able to move, trim, split, duplicate, and delete overlay clips; a video overlay MUST stay inside its file; the project length MUST follow the end of the last clip of any lane.
- **FR-008**: Overlays MUST be saved with the project and restored, with the same autosave guarantee, and MUST NOT modify source files.
- **FR-009**: Export MUST render each overlay at the same time, relative place, relative size, and opacity as the preview.
- **FR-010**: The tools MUST follow the existing visual language, work in compact, medium, expanded (inspector from 840 dp) and tabletop arrangements, keep an open panel through fold and rotation, keep nothing interactive under the hinge, scale text, have touch targets of at least 48 dp and accessible labels.

### Key Entities

- **Overlay clip**: a media file, the part of it shown, its start on the timeline, its center and width as shares of the frame, and its opacity.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user adds an overlay and places it within 30 seconds, on the first attempt.
- **SC-002**: A move, resize, or opacity change is seen within 100 ms.
- **SC-003**: With 3 overlays visible, preview holds 30 fps at 1080p on a mid-range device or the app lowers quality and says so.
- **SC-004**: 100% of overlay edits can be undone and redone to the identical project.
- **SC-005**: After force-closing right after an edit, 100% of projects reopen with the same overlays.
- **SC-006**: In an exported file an overlay appears within 1 frame of its start and end and covers the same relative area within 1% of the frame size at 720p and 1080p.
- **SC-007**: Source files are byte-identical before and after overlay edits in 100% of tested cases.
- **SC-008**: A design review finds no deviation from the existing visual language other than documented ones, and nothing under the hinge.

## Assumptions

- Features 001 to 009 are in place; the designer's reference has the Overlay tab but no panels; controls follow the existing visual language and are reviewed with the designer.
- The preview and export draw overlays with the same compositing settings (one shared builder).
