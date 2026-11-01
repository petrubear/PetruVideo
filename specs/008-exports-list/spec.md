# Feature Specification: Exports List

**Feature Branch**: `008-exports-list`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "Exports list: the Exports entry of the Projects bottom navigation shows the videos the user has exported, newest first, with a thumbnail, project name, date, length, size, and format; each can be played, shared, or removed from the list."

## Scope note

Export (004) saves a video to the gallery and keeps only the latest job of each project. This feature keeps a history of finished exports and shows it on a new Exports screen reached from the Exports entry of the Projects bottom navigation, which is disabled until now. Removing an entry never deletes the video file; deleting files, cloud upload, and re-exporting from the list are out of scope.

## Clarifications

### Session 2026-11-01

- Q: Does removing an entry delete the video from the gallery? → A: No, it only removes the entry; the file stays in the gallery.
- Q: What happens to an entry whose video was deleted outside the app? → A: It stays in the list marked "File not found" with only Remove available.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - See my exported videos (Priority: P1)

A user opens Exports from the bottom navigation and sees every video they exported, newest first. Each row shows a thumbnail, the project name, the date and time, the video length, the file size, and the format (for example "1080p, 30 fps, Recommended").

**Why this priority**: Finding past exports is the whole point of the screen.

**Independent Test**: Export two projects, open Exports, see two rows newest first with the right details; with no exports see an empty message.

**Acceptance Scenarios**:

1. **Given** at least one finished export, **When** the user opens Exports, **Then** the list shows each export newest first with thumbnail, project name, date and time, length, size, and format.
2. **Given** no exports yet, **When** the user opens Exports, **Then** a short empty message explains that exported videos appear here.
3. **Given** an export finishes while the Exports screen is open, **When** the list updates, **Then** the new row appears at the top without leaving the screen.
4. **Given** a project was renamed or deleted after exporting, **When** the list shows, **Then** the row keeps the name the project had at export time.
5. **Given** a failed or cancelled export, **When** the list shows, **Then** it has no row for it.

---

### User Story 2 - Play, share, remove (Priority: P1)

A user taps a row to play the video in the device's video player, taps Share to send it, or removes the row from the list. A removed row disappears, and the video stays in the gallery.

**Why this priority**: Playing and sharing are the reasons users come back to exports.

**Independent Test**: Tap a row and see the video open; tap Share and see the share sheet; remove a row and see it gone while the file is still in the gallery.

**Acceptance Scenarios**:

1. **Given** a row, **When** the user taps it or Play, **Then** the video opens in a player app.
2. **Given** a row, **When** the user taps Share, **Then** the system share sheet opens with the video.
3. **Given** a row, **When** the user taps Remove, **Then** the row disappears after a confirmation message with Undo for 5 seconds, and the video file is not deleted.
4. **Given** the file was deleted outside the app, **When** the list shows, **Then** the row is marked "File not found", Play and Share are unavailable, and Remove is available.

---

### User Story 3 - Layouts, accessibility, persistence (Priority: P3)

The list works on the phone, unfolded (two columns), and in tabletop; entries survive restarting the app.

**Why this priority**: Layout and persistence rules apply to every screen.

**Independent Test**: Export, force-close, reopen and find the entries; unfold and see a grid; half-fold and see nothing under the hinge.

**Acceptance Scenarios**:

1. **Given** the unfolded device (840 dp and wider), **When** Exports opens, **Then** rows are shown in two columns; in tabletop the list sits below the hinge.
2. **Given** the app was force-closed, **When** it reopens, **Then** all entries are still listed.
3. **Given** a screen reader, **When** a row is focused, **Then** it reads the project name, date, length, size, and available actions.

---

### Edge Cases

- Hundreds of entries: the list scrolls smoothly and thumbnails load lazily.
- A thumbnail that cannot be read: a plain placeholder with the format is shown.
- Two exports of the same project: both appear as separate rows.
- Removing the last row shows the empty message.
- Storage permission revoked or the file moved: shown as "File not found".

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Every export that finishes MUST add an entry with the project name at that time, the saved video's location, its length and size, the settings used (resolution, frame rate, quality), and the finish time.
- **FR-002**: The Exports entry of the Projects bottom navigation MUST open the Exports screen, with a way back to Projects.
- **FR-003**: The list MUST show entries newest first, each with a thumbnail, project name, date and time, length, size, and format, and MUST update while open.
- **FR-004**: Tapping an entry or Play MUST open the video in a player app; Share MUST open the share sheet with the video.
- **FR-005**: Remove MUST remove only the entry, show a confirmation with Undo for 5 seconds, and MUST NOT delete the video file.
- **FR-006**: An entry whose file cannot be opened MUST show "File not found", offer only Remove, and be re-checked each time the screen opens.
- **FR-007**: Failed and cancelled exports MUST NOT create entries.
- **FR-008**: Entries MUST be kept when the app restarts and MUST NOT depend on the project still existing.
- **FR-009**: An empty list MUST show an explanatory message.
- **FR-010**: The screen MUST follow the existing visual language, use two columns from 840 dp, keep nothing interactive under the hinge in tabletop, scale text, and have touch targets of at least 48 dp and accessible labels.

### Key Entities

- **Export record**: id, project name, video location, length, size, resolution, frame rate, quality, finish time.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user finds and plays a past export within 15 seconds of opening the app.
- **SC-002**: 100% of finished exports appear in the list, and 0% of failed or cancelled ones.
- **SC-003**: With 500 entries the list opens in under 1 second and scrolls without visible stutter on a mid-range device.
- **SC-004**: After force-closing the app, 100% of entries are still listed.
- **SC-005**: Removing entries never deletes a video file in 100% of tested cases.
- **SC-006**: A design review finds no deviation from the existing visual language other than documented ones, and nothing sits under the hinge.

## Assumptions

- Feature 004's export and gallery saving are in place; the saved video's location is available when the export finishes.
- The designer's reference shows the Exports entry but no screen for it; the screen follows the existing visual language and is reviewed with the designer.
- Thumbnails come from the exported video's own frames.
- Entries made before this feature exist only for exports finished after it ships.
