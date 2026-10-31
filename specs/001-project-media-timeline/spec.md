# Feature Specification: Project Creation, Media Import, and Basic Timeline with Trim

**Feature Branch**: `001-project-media-timeline`

**Created**: 2026-10-30

**Status**: Draft

**Input**: User description: "for project creation, media import, and basic timeline with trim"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Create and manage projects (Priority: P1)

A user opens the app and sees their list of projects. They can create a new project, give it a name, reopen an existing project, rename it, duplicate it, or delete it. Every project saves automatically so work is never lost.

**Why this priority**: A project is the container for all editing. Without it nothing else can be saved or resumed.

**Independent Test**: Create a project, close the app, reopen it, and confirm the project is listed and opens in its previous state.

**Acceptance Scenarios**:

1. **Given** the user is on the project list, **When** they create a new project, **Then** an empty project opens in the editor and appears in the list.
2. **Given** an existing project, **When** the user reopens the app, **Then** the project is listed with its name, last-modified time, and a thumbnail or placeholder.
3. **Given** a project in the list, **When** the user deletes it and confirms, **Then** it is removed from the list and the original media files on the device are untouched.
4. **Given** a project in the list, **When** the user renames or duplicates it, **Then** the list reflects the change and the duplicate edits independently.

---

### User Story 2 - Import media into a project (Priority: P1)

Inside a project, the user chooses videos, photos, and audio files from their device and adds them to the project's media library. They can then see all imported items as thumbnails with duration.

**Why this priority**: Editing needs source material; import is the entry point to every later workflow.

**Independent Test**: Open a project, import one video, one photo, and one audio file, and confirm each appears in the media library with correct type, thumbnail, and duration.

**Acceptance Scenarios**:

1. **Given** an open project, **When** the user picks several media files from the device, **Then** all selected files appear in the media library.
2. **Given** an unsupported or corrupted file is selected, **When** import runs, **Then** the user sees a clear message naming the file and the rest of the selection still imports.
3. **Given** the source file is later moved or deleted on the device, **When** the project is opened, **Then** the affected clip is marked as missing and the rest of the project still opens.
4. **Given** the user has not granted access to their media, **When** they try to import, **Then** the app explains why access is needed and allows them to choose files without granting broad access.

---

### User Story 3 - Arrange clips on a basic timeline (Priority: P1)

The user adds imported clips to a timeline, sees them laid out in order with thumbnails, scrubs through the timeline to preview, plays and pauses, and reorders or removes clips.

**Why this priority**: The timeline is the core editing surface; trimming and every future feature build on it.

**Independent Test**: Add three clips to the timeline, reorder them, scrub and play the preview, and confirm playback follows the new order.

**Acceptance Scenarios**:

1. **Given** clips in the media library, **When** the user adds them to the timeline, **Then** they appear one after another in the order added.
2. **Given** a timeline with clips, **When** the user drags the playhead, **Then** the preview shows the frame at that position and the time display updates.
3. **Given** a timeline with clips, **When** the user presses play, **Then** the video and its audio play continuously across clip boundaries until the end or until paused.
4. **Given** a clip on the timeline, **When** the user drags it to a new position or deletes it, **Then** the timeline updates and the total duration is recalculated.
5. **Given** a still photo is added, **When** it appears on the timeline, **Then** it has a default duration that the user can change.

---

### User Story 4 - Trim and split clips (Priority: P2)

The user shortens a clip by dragging its start or end handles, or splits it at the playhead into two clips. All trims are reversible, and the original media is never changed.

**Why this priority**: Trimming is the most fundamental edit, but it needs the timeline from Story 3 first.

**Independent Test**: Trim a clip's start and end, split another at the playhead, then undo and redo each change, and confirm the timeline duration and preview match at every step.

**Acceptance Scenarios**:

1. **Given** a clip on the timeline, **When** the user drags its end handle inward, **Then** the clip is shortened, the preview matches, and later clips shift to close the gap.
2. **Given** a clip trimmed earlier, **When** the user drags the handle back outward, **Then** the previously hidden footage is restored up to the original length.
3. **Given** the playhead is inside a clip, **When** the user chooses split, **Then** the clip becomes two adjacent clips with no gap and no lost frames.
4. **Given** any trim, split, move, or delete, **When** the user taps undo then redo, **Then** the timeline returns to the exact previous and next states.
5. **Given** a clip, **When** the user trims it, **Then** the source file on the device is unchanged.

---

### Edge Cases

- Importing very large files (over 2 GB or over 30 minutes) must not freeze the app; import shows progress and can be cancelled.
- Videos with unusual orientation, rotation metadata, or variable frame rate display and play correctly.
- Videos with no audio track, and audio-only files, are handled without errors.
- A clip trimmed to the minimum length cannot be trimmed to zero or negative length.
- The app is closed or killed mid-edit; on reopen the project is restored to within the last few seconds of work.
- The device runs low on storage while saving; the user is warned and existing project data is not corrupted.
- Importing the same file twice creates two independent references without duplicating the file.
- An empty timeline shows a helpful empty state and playback controls are disabled.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Users MUST be able to create, rename, duplicate, and delete projects from a project list.
- **FR-002**: The system MUST autosave project changes so no more than a few seconds of work are lost after an unexpected exit.
- **FR-003**: The project list MUST show each project's name and last-modified time, ordered most recent first.
- **FR-004**: Users MUST be able to import videos, photos, and audio files from device storage into a project, selecting multiple files at once.
- **FR-005**: The system MUST show imported media in a library with thumbnail, type, and duration.
- **FR-006**: The system MUST reject unsupported or unreadable files with a clear, per-file message without blocking the rest of the import.
- **FR-007**: The system MUST NOT modify or move the user's original media files at any time.
- **FR-008**: The system MUST detect missing source media when a project is opened and flag the affected clips instead of failing.
- **FR-009**: Users MUST be able to add library items to a single-track timeline, reorder them, and remove them.
- **FR-010**: The timeline MUST display clips in sequence with thumbnails and a visible playhead, and show total duration.
- **FR-011**: Users MUST be able to scrub the playhead and see the matching preview frame, and play and pause the full timeline with audio.
- **FR-012**: Photos on the timeline MUST have an adjustable duration, with a default of 5 seconds.
- **FR-013**: Users MUST be able to trim a clip's start and end by dragging handles, and restore trimmed footage by dragging them back.
- **FR-014**: Users MUST be able to split a clip at the playhead into two adjacent clips.
- **FR-015**: Every timeline change (add, move, remove, trim, split) MUST be undoable and redoable.
- **FR-016**: The system MUST request media access only when needed and offer a way to import without granting broad storage access.
- **FR-017**: Import and preview MUST show progress or a loading state for operations taking longer than one second, and long imports MUST be cancellable.
- **FR-018**: The editor MUST work fully offline.

### Key Entities

- **Project**: A named editing session with creation and modification times, a media library, and a timeline.
- **Media Item**: A reference to an imported video, photo, or audio file, with type, duration, dimensions where applicable, and source location. It never contains a modified copy of the original.
- **Timeline**: The ordered sequence of clips in a project, with a total duration and a current playhead position.
- **Clip**: A placement of a media item on the timeline, with a start offset and end offset within the source (the trim) and its position in the sequence.
- **Edit History**: The ordered record of timeline changes that supports undo and redo.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A new user can create a project, import a video, and see it on the timeline in under 60 seconds.
- **SC-002**: Users can trim a clip to a chosen length on their first attempt in under 20 seconds.
- **SC-003**: Scrubbing and playback preview stay smooth, at 30 frames per second for 1080p footage, on a mid-range device.
- **SC-004**: Importing 20 mixed media files completes with all thumbnails visible in under 15 seconds.
- **SC-005**: After force-closing the app mid-edit, 100% of projects reopen with no more than 5 seconds of work lost.
- **SC-006**: 100% of trim, split, move, and delete actions can be undone and redone back to the identical timeline state.
- **SC-007**: Original media files are byte-identical before and after editing in 100% of tested cases.
- **SC-008**: 90% of first-time testers complete the create, import, arrange, and trim flow without help.

## Assumptions

- Target users are casual creators editing short videos on their own phone, with no prior editing experience required.
- This feature covers a single video/photo track with audio carried by the clips; multiple tracks, overlays, text, effects, transitions, and export are separate later features.
- Export of the edited video is out of scope here; this feature ends at an editable, previewable, saved project.
- Supported import formats follow the project constitution (common H.264/HEVC/VP9 video, AAC/MP3/WAV audio, JPEG/PNG/WebP images).
- Media is referenced from its original location rather than copied into the project; moved or deleted files are handled as in FR-008.
- Phone portrait orientation is the primary layout; tablet and landscape layouts are best-effort.
- Performance targets align with the project constitution's real-time preview budget.
