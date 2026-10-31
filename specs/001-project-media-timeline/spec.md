# Feature Specification: Project Creation, Media Import, and Basic Timeline with Trim

**Feature Branch**: `001-project-media-timeline`

**Created**: 2026-10-30

**Updated**: 2026-10-30 (aligned to the designer's UI reference and the foldable-first constitution)

**Status**: Draft

**Input**: User description: "project creation, media import, and basic timeline with trim; respect the design the designer implemented for all the screens"

## Design Reference *(mandatory)*

The visual and interaction design is defined by the designer's **PetruVideo UI Reference** canvas (https://claude.ai/artifact/D8dMeHZE4XUAvNj4wERyNo). It is the source of truth for layout, hierarchy, labels, colors, and component behavior. Where this spec and the design disagree on how something looks or is arranged, the design wins; where the design is silent, the project constitution applies.

| Design screen (artboard) | Used by | Scope in this feature |
|--------------------------|---------|-----------------------|
| Projects (phone) | Story 1 | Fully in scope. Bottom navigation entries other than Projects belong to later features. |
| Multi-import (phone) | Story 2 | Fully in scope. The design now has a single tray action ("Add to timeline in order") and no header "Add N" button. |
| Timeline and trim (phone) | Stories 3, 4 | In scope for the main video lane, the audio lane (attached sound display, detached and added audio clips, mute), playhead, trim, split, duplicate, delete, detach and replace audio, undo/redo. |
| Detached audio selected (phone) | Story 4 | Fully in scope: detached audio clip with trim handles and the Split, Mute, Replace, Duplicate, Delete tool bar. |
| Foldable: Add media panel; Foldable: Add media in tabletop | Story 5 | Fully in scope (FR-027a): centered panel with 6 columns on expanded width; lower half below the hinge in tabletop. |
| Foldable: unfolded editor | Story 5 | In scope for the multi-pane layout; panels for later features are present but inactive. |
| Foldable: tabletop posture | Story 5 | Fully in scope. |
| Foldable: projects and media | Story 5 | Fully in scope. |
| Video effects, Picture in picture, Audio editing, Export | Later features | Out of scope. Their entry points appear in the editor as drawn in the design but are inactive (see Assumptions). |

The design uses placeholder gradients where real footage will appear; real thumbnails replace them.

## Clarifications

### Session 2026-10-30

- Q: How should a new project's format (aspect ratio, resolution, frame rate) be set? → A: Automatically from the first clip added (aspect ratio kept, resolution capped at 1080p, frame rate rounded to 24/30/60); later clips are fitted inside the frame.
- Q: What should happen when a clip's source file has been moved or deleted? → A: The clip is shown as "missing" with Relink (pick a replacement file; the trim is kept) or Remove.
- Q: How should tools that aren't built yet appear in the editor? → A: Shown as drawn but dimmed; tapping shows a "coming soon" message.
- Q: How should a video's audio behave? → A: Audio stays attached to its video by default. A selected video clip has a "Detach" action that moves the sound to its own clip on the audio lane. A detached audio clip can be trimmed, moved, split, duplicated, deleted, muted, or replaced with another audio file, and the video becomes silent. Audio-only files from the Audio tab go on the audio lane. Volume, fades, and audio effects stay in the later Audio feature. Because attached audio is part of its video clip, no linking or unlinking is needed.
- Q: Which add buttons stay on Add media? → A: Only the tray button, "Add to timeline in order"; the header has just the close button and title (the designer's header "Add N" button is removed).
- Q: How should test devices be covered? → A: The owner's physical Samsung Fold is the primary foldable target and the feature must work flawlessly on it: every quickstart scenario passes on that device (cover screen, inner display, flat and half-open, split-screen, pop-up window) with no workaround. A tablet emulator profile is added, and the low-end, mid-range, and high-end smoke test is a release-checklist step outside this feature.
- Q: How many thumbnail columns does Add media use at each width? → A: Fixed counts: 3 on compact, 5 on medium, 6 on expanded.
- Q: Who updates the design for the expanded Add media panel, the tabletop placement, the tray without "Add to library", and the progress bar caption? → A: Claude updates the design canvas directly after the analysis cleanup, so no designer gate task is needed.
- Q: What should the app do when the device is low on memory or overheating? → A: Degrade quietly (lower preview resolution and frame rate, smaller thumbnails) with a small non-blocking notice, never lose edits, and pause playback with a message only if it still cannot continue.
- Q: What should mouse and stylus input support? → A: Full pointer parity (everything touch can do works with a mouse or stylus), a hover highlight on controls for a mouse, and wheel or trackpad scrolling of the timeline. Mouse, keyboard, and stylus support comes from constitution Principle VIII and is low cost; it is not part of the designer's screens.
- Q: Do we keep an explicit project library with an "Add to library" action? → A: No. Add media has a single action, "Add to timeline in order". The project still records every file added (needed for trim, relink, and missing-file detection), and the unfolded Media panel and the project detail show that media, but the user never manages a library.
- Q: How does a user change a photo's duration? → A: By dragging the photo clip's trim handles, which can shorten and lengthen it between 0.5 and 60 seconds, with the duration badge updating live; the unfolded inspector also shows the value.
- Q: How should overlapping audio behave? → A: Audio clips never overlap each other (a clip that would overlap snaps to the next free spot and the user is told), but an audio clip may overlap a video's attached sound and both play together. To hear only new audio, the user detaches the original sound and deletes or mutes it.
- Q: Where do items added from Add media land on the timeline? → A: Video and photos go on the video lane after the clip under the playhead (at the playhead if on a boundary, at the start if the timeline is empty); audio-only files start at the playhead on the audio lane.
- Q: How should Add media appear on expanded width and in tabletop posture? → A: As a large centered panel over the editor with the same content (more grid columns, tray at the bottom); in tabletop posture it fills the lower half below the hinge. Medium width (600 to 839 dp) uses the compact layouts with wider content; multi-pane layouts start at expanded width (840 dp and up).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Create and manage projects (Priority: P1)

A user opens the app and lands on the **Projects** screen: a "Projects" title with search and settings buttons, a "Continue editing" card for the most recent project (thumbnail, name, last edited time, clip count, length, and a progress bar), filter chips (All, Drafts, Exported with counts), and a two-column grid of project cards that begins with a dashed "New project" tile. Each card shows a thumbnail, duration badge, name, date, and an options menu; exported projects carry an "Exported" badge. Work saves automatically.

**Why this priority**: A project is the container for all editing; nothing else can be saved or resumed without it.

**Independent Test**: Create a project, close the app, reopen it, and confirm the project appears in the grid and in "Continue editing" and opens in its previous state.

**Acceptance Scenarios**:

1. **Given** the Projects screen, **When** the user taps the "New project" tile, **Then** an empty project opens in the editor and later appears in the grid.
2. **Given** at least one project, **When** the user opens the app, **Then** the "Continue editing" card shows the most recently edited project and tapping it resumes editing.
3. **Given** projects in different states, **When** the user selects the Drafts or Exported chip, **Then** only matching projects are shown and the chip counts stay accurate.
4. **Given** a project card, **When** the user opens its options menu, **Then** they can rename, duplicate, or delete it; deleting asks for confirmation and never touches the original media files on the device.
5. **Given** the user taps the search button, **When** they type part of a project name, **Then** the list narrows to matching projects.

---

### User Story 2 - Import multiple media items (Priority: P1)

From the editor, the user opens the **Add media** screen: a close button and title; a Videos / Photos / Audio selector; album chips (Recent, Camera, Screen recordings, Downloads); and a three-column grid of thumbnails with duration labels. Tapping items selects them with numbered badges showing selection order. A tray at the bottom lists the selected items as thumbnails, the selected count, the total duration, and offers a single action, "Add to timeline in order".

**Why this priority**: Editing needs source material, and selecting many items at once is a core part of the design.

**Independent Test**: Open a project, select five mixed items in a chosen order, tap "Add to timeline in order", and confirm the timeline contains them in exactly that order.

**Acceptance Scenarios**:

1. **Given** the Add media screen, **When** the user taps several thumbnails, **Then** each shows a numbered badge in tap order, tapping a selected item deselects it and renumbers the rest, and the tray count and total duration update.
2. **Given** items are selected, **When** the user chooses "Add to timeline in order", **Then** videos and photos are inserted on the video lane in selection order after the clip under the playhead (at the playhead if it sits on a boundary, at the start if the timeline is empty), each video keeps its sound attached, audio-only files start at the playhead on the audio lane, and the files are recorded as the project's media so they can be trimmed, relinked, and shown in the media panel on large screens.
3. **Given** an unsupported or corrupted file is selected, **When** the import runs, **Then** the user sees a clear message naming that file and the remaining items still import.
4. **Given** the user has not granted media access, **When** they open Add media, **Then** the app explains why access is needed and offers a way to choose files without granting broad access.
5. **Given** the user switches between Videos, Photos, Audio, or an album chip, **When** the grid reloads, **Then** their existing selection is kept.

---

### User Story 3 - Arrange clips on the editor timeline (Priority: P1)

The **editor** matches the designed layout: a top bar (back button, project name with resolution, frame rate, and save status, and an Export button), the preview, a transport row (current time / total time, undo, play, redo, full-screen), and the timeline. The timeline has a time ruler, a fixed center playhead with a handle, a main video lane of clip thumbnails, an audio lane that shows each video's attached sound as a waveform under its clip and holds detached or added audio clips, and an "add clip" button. A bottom tool bar lists Edit, Audio, Text, Overlay, Effects, and Filters, with Edit active.

**Why this priority**: The timeline is the core editing surface that trim and every later feature build on.

**Independent Test**: Add three clips, scrub, play, drag one to a new position, and confirm the preview, time display, and total length follow.

**Acceptance Scenarios**:

1. **Given** clips in the project, **When** the user views the timeline, **Then** video clips appear in sequence with thumbnails, each video's attached sound appears as a waveform on the audio lane aligned under it, and the ruler shows time marks with the current second highlighted.
2. **Given** a timeline, **When** the user scrolls it horizontally, **Then** the playhead stays centered, the preview shows the frame under it, and the current time updates.
3. **Given** a timeline, **When** the user presses play, **Then** video and audio play continuously across clip boundaries until the end or until paused.
4. **Given** the user taps the add clip button, **When** they pick media, **Then** it is inserted after the clip under the playhead (at the playhead if it sits on a clip boundary, at the start if the timeline is empty) and the timeline updates.
5. **Given** a clip, **When** the user taps it, **Then** it becomes selected (highlighted outline with trim handles) and the clip tool bar (Split, Speed, Volume, Duplicate, Delete) replaces the idle state.
6. **Given** a selected clip, **When** the user long-presses and drags it, **Then** it moves to the new position and the total length is recalculated.
7. **Given** a still photo on the timeline, **When** it is selected, **Then** its trim handles can shorten or lengthen it between 0.5 and 60 seconds with the duration badge updating live, starting from a 5 second default.
8. **Given** the user taps the full-screen button, **When** the full-screen preview opens, **Then** playback continues from the same position.

---

### User Story 4 - Trim, split, duplicate, and delete clips (Priority: P2)

With a clip selected, the user drags the white **trim handles** on its start and end to shorten it. A duration badge on the clip shows its current length. They can also split at the playhead, duplicate the clip, or delete it from the clip tool bar. Undo and redo are available at all times. Originals are never changed.

**Why this priority**: Trimming is the most fundamental edit but depends on the timeline in Story 3.

**Independent Test**: Trim a clip's start and end, split another at the playhead, duplicate and delete clips, then undo and redo each change and confirm the timeline and preview match at every step.

**Acceptance Scenarios**:

1. **Given** a selected clip, **When** the user drags the end handle inward, **Then** the clip shortens, the duration badge updates live, the preview shows the frame at the handle, and later clips close the gap.
2. **Given** a previously trimmed clip, **When** the user drags a handle back outward, **Then** the hidden footage returns up to the original length.
3. **Given** the playhead is inside a clip, **When** the user taps Split, **Then** the clip becomes two adjacent clips with no gap and no lost frames.
4. **Given** a selected clip, **When** the user taps Duplicate, **Then** an identical clip is inserted right after it; **When** they tap Delete, **Then** it is removed and the timeline closes the gap.
5. **Given** any edit, **When** the user taps undo then redo, **Then** the timeline returns to the exact previous and next states; the undo and redo buttons appear dimmed when there is nothing to undo or redo.
6. **Given** any trim, **When** the user checks the source file on the device, **Then** it is unchanged.
7. **Given** a clip is trimmed to its minimum length, **When** the user keeps dragging, **Then** the handle stops and the clip cannot reach zero length.
8. **Given** a selected video clip that has sound, **When** the user taps "Detach", **Then** the sound becomes its own audio clip on the audio lane at the same position and length, the video clip becomes silent, and undo restores the attached state.
9. **Given** a detached or added audio clip, **When** the user selects it, **Then** it shows trim handles and the Split, Duplicate, and Delete actions, plus Mute and "Replace"; **When** they replace it, **Then** the new audio takes the same position and length (trimmed to fit if longer) and the video clip is unchanged.
10. **Given** an audio clip is muted, deleted, or replaced, **When** the user plays the timeline, **Then** the picture plays unchanged and only the sound differs.
11. **Given** a video clip with attached sound, **When** the user moves, splits, duplicates, trims, or deletes the clip, **Then** its sound moves, splits, duplicates, trims, or is removed with it and stays in sync.
12. **Given** a music file placed over a video that still has its own sound, **When** the user plays, **Then** both play together.
13. **Given** a video whose sound the user wants to replace, **When** they Detach the audio and then Delete or Mute the detached clip, **Then** only the added audio is heard and the video clip is unchanged.
14. **Given** a placement or move that would overlap another audio clip, **When** the user places or moves a clip, **Then** the clip snaps to the next free position automatically, a message says so, and the change can be undone.

---

### User Story 5 - Use the editor comfortably on foldable devices (Priority: P1)

On a foldable device the app adapts to the screen as the designer drew it:
- **Unfolded editor**: a tool rail on the left (Media, Audio, Text, Effects, Filters), a Media panel showing the media already used in the project with an Import button, a large preview with transport controls, an inspector panel on the right (selected clip name and length, with Video / Audio / Speed tabs), and a full-width timeline with labeled tracks along the bottom.
- **Tabletop posture** (half-folded): the preview sits above the hinge; below it are the transport controls (previous clip, play, next clip, split), the timeline, and the tool bar, within thumb reach.
- **Projects and media on the unfolded screen**: a two-pane view with the project list on the left and the selected project's details (thumbnail, clip count, length, format, "Open editor", Duplicate, Export, Delete) and its media on the right.
- The compact (phone) layouts described in Stories 1 to 4 remain fully functional on their own, and the cover screen uses them.

**Why this priority**: The constitution makes foldable support a release requirement, not an enhancement.

**Independent Test**: Start editing on the cover screen, unfold mid-edit, then half-fold into tabletop posture, and confirm the project, playhead position, selection, and playback are unchanged at every step.

**Acceptance Scenarios**:

1. **Given** the user is editing on the cover screen, **When** they unfold the device, **Then** the multi-pane editor appears with the same project, playhead, and selection, and playback is not interrupted.
2. **Given** the unfolded editor, **When** the user drags a clip from the media panel onto the timeline, **Then** it is inserted where dropped.
3. **Given** the device is half-folded, **When** the editor is open, **Then** the preview is entirely on one side of the hinge, controls are entirely on the other, and nothing interactive sits under the hinge.
4. **Given** the unfolded Projects view, **When** the user selects a project in the list, **Then** its details and media appear in the right pane without leaving the screen.
5. **Given** the user rotates the device or enters split-screen, **When** the window resizes, **Then** the layout re-adapts without losing unsaved work.
6. **Given** a physical keyboard, mouse, or stylus is in use on a large screen, **When** the user uses it, **Then** common actions work: space toggles play, arrow keys step the playhead, Delete removes the selected clip, undo/redo shortcuts work, and a mouse or stylus can do everything touch can.

---

### Edge Cases

- Importing very large files (over 2 GB or over 30 minutes) must not freeze the app; import shows progress and can be cancelled.
- Videos with rotation metadata, unusual orientation, or variable frame rate display and play correctly.
- Videos with no audio track create no audio clip; audio-only files are handled without errors.
- A detached audio clip is moved or trimmed so it no longer lines up with its video; playback keeps both as placed and the audio lane does not shift the video.
- A video clip with detached audio is deleted or duplicated; the detached audio clip is left as it is and is not duplicated or removed.
- A replacement audio file is shorter than the clip it replaces; the audio clip takes the shorter length, the rest of the span stays silent, and the user is told the length changed.
- The app is closed or killed mid-edit; on reopen the project is restored to within the last few seconds of work.
- The device runs low on storage while saving; the user is warned and existing project data is not corrupted.
- The device runs low on memory or overheats during editing or playback; preview resolution and frame rate drop and thumbnails shrink, a small non-blocking notice appears, edits are never lost, and if playback still cannot continue it pauses with a message.
- Importing the same file twice creates two independent references without duplicating the file.
- An empty project shows a helpful empty state in the timeline and playback controls are disabled.
- The device is folded or unfolded during an import or while a dialog is open; the in-progress action continues or its state is restored.
- The hinge or fold occlusion overlaps a control; the control is moved out of the occluded area.
- The first clip is removed from a project whose format came from it; the format stays as set and is not recalculated.
- Clips with a different aspect ratio than the project are shown fitted inside the frame with empty bars, never stretched.
- A project has many clips (over 100); the timeline remains scrollable and responsive.
- A tool whose feature is not yet available is tapped; the user gets a clear "coming soon" indication and nothing changes.

## Requirements *(mandatory)*

### Functional Requirements

**Projects**

- **FR-001**: Users MUST be able to create, rename, duplicate, and delete projects from the Projects screen as designed, with a confirmation before deleting.
- **FR-002**: The Projects screen MUST show a "Continue editing" card for the most recently edited project and a grid of all projects, most recent first, beginning with a "New project" tile.
- **FR-003**: Each project card MUST show thumbnail, duration badge, name, relative date, an options menu, and an "Exported" badge when applicable.
- **FR-004**: Users MUST be able to filter projects by All, Drafts, and Exported with accurate counts, and search projects by name.
- **FR-005**: The system MUST autosave project changes so no more than 5 seconds of work are lost after an unexpected exit, and the editor MUST show a "saved" status.

- **FR-005a**: A new project MUST take its format from the first clip added: the clip's aspect ratio, a resolution capped at 1080p, and a frame rate rounded to 24, 30, or 60. Later clips with a different size or frame rate MUST be fitted inside the project frame without cropping or distortion. The format MUST be shown in the editor top bar.

**Media import**

- **FR-006**: The Add media screen MUST provide Videos, Photos, and Audio views, album chips, and a three-column thumbnail grid with duration labels.
- **FR-007**: Users MUST be able to multi-select items; selection order MUST be shown as numbered badges and MUST be preserved when the items are added.
- **FR-008**: The selection tray MUST show selected thumbnails, the count, and the total duration, and MUST offer a single action, "Add to timeline in order". There is no separate "Add to library" action; media becomes part of the project only when it is added to the timeline.
- **FR-009**: The system MUST reject unsupported or unreadable files with a clear per-file message without blocking the rest of the import.
- **FR-010**: The system MUST request media access only when needed, and MUST offer a way to import without granting broad storage access.
- **FR-011**: Import MUST show progress for operations longer than one second and long imports MUST be cancellable.
- **FR-012**: The system MUST NOT modify or move the user's original media files and MUST detect missing source media when a project opens, marking affected clips as "missing" instead of failing. For a missing clip the user MUST be able to Relink it (choose a replacement file, keeping the clip's trim and position) or Remove it; playback MUST skip missing clips with a visible placeholder.

**Timeline and playback**

- **FR-013**: The editor MUST follow the designed layout: top bar, preview, transport row (time readout, undo, play, redo, full-screen), timeline, clip tool bar, and main tool bar.
- **FR-014**: The timeline MUST show a time ruler, a fixed playhead, a main video lane with thumbnails, and an audio lane, plus an add clip button.
- **FR-014a**: A video's sound MUST stay attached to its video clip by default and be shown as a waveform on the audio lane under the clip. Moving, splitting, duplicating, trimming, or deleting the video clip MUST apply to its attached sound and keep them in sync. A video with no sound MUST show no waveform.
- **FR-015**: Users MUST be able to scrub, play, and pause the full timeline with audio, and the time readout MUST show current and total time.
- **FR-016**: Users MUST be able to reorder clips by dragging. New video and photo clips MUST be inserted after the clip under the playhead (at the playhead if it sits on a clip boundary, at the start if the timeline is empty), in selection order.
- **FR-017**: Photos on the timeline MUST default to 5 seconds and MUST be adjustable between 0.5 and 60 seconds by dragging their trim handles, which can both shorten and lengthen them; the unfolded inspector MUST also show the value.
- **FR-018**: Selecting a clip MUST show it highlighted with trim handles, a duration badge, and the clip tool bar.

**Editing**

- **FR-019**: Users MUST be able to trim a clip's start and end with handles and restore trimmed footage by dragging them back; a clip MUST NOT be trimmable below 0.1 seconds.
- **FR-020**: Users MUST be able to split a clip at the playhead, duplicate a clip, and delete a clip, for both video clips and audio clips.
- **FR-020a**: A selected video clip that has sound MUST offer a "Detach" action that turns its sound into a separate audio clip on the audio lane at the same position and length and makes the video silent. The action MUST be undoable.
- **FR-020b**: Users MUST be able to trim, move, mute, and unmute a detached or added audio clip independently of any video clip, and replace it with another audio file chosen from the device; the replacement MUST keep the original position and length.
- **FR-020c**: Audio-only files MUST be addable to the audio lane, starting at the playhead and following each other in selection order.
- **FR-020d**: Audio clips MUST NOT overlap each other on the audio lane: when an add or move would overlap another audio clip, the clip MUST snap to the next free position and the user MUST be told. An audio clip MAY overlap a video's attached sound, and both MUST play together. To hear only new audio, the user can detach the original sound and delete or mute it.
- **FR-021**: Every timeline change MUST be undoable and redoable, and the undo and redo buttons MUST reflect availability.
- **FR-022**: Tools belonging to later features (Speed, Volume, Audio, Text, Overlay, Effects, Filters, Export) MUST appear where designed but be inactive, with a clear "coming soon" indication when tapped.

**Foldable and large-screen**

- **FR-023**: The app MUST provide the compact layout and the designed unfolded multi-pane layouts (editor, projects and media), and MUST choose between them based on available window size, not device model.
- **FR-024**: Folding, unfolding, rotating, resizing, and split-screen MUST NOT lose the open project, playhead position, selection, unsaved edits, or interrupt playback.
- **FR-025**: In tabletop posture the preview MUST sit on one side of the hinge and controls on the other, with no interactive content under the hinge or fold occlusion.
- **FR-026**: The cover screen MUST support project browsing, project opening, and basic trim using the compact layout.
- **FR-027**: On large screens, users MUST be able to drag media from the Media panel (media already used in the project) onto the timeline and use keyboard shortcuts for play, step, delete, undo, and redo. A mouse or stylus MUST be able to do everything touch can do (select, drag trim handles, scrub, reorder, drag and drop); controls MUST show a hover highlight for a mouse, and the mouse wheel or trackpad MUST scroll the timeline sideways.

- **FR-027a**: On expanded width the Add media screen MUST appear as a large centered panel over the editor with the same content as the phone layout (6 grid columns, selection tray at the bottom); in tabletop posture it MUST fill the half below the hinge. On medium width it MUST use the phone layout with 5 grid columns (the phone layout uses 3).
- **FR-027b**: Medium width (600 to 839 dp) MUST use the compact layouts with wider content; multi-pane layouts MUST start at expanded width (840 dp and up). In tabletop posture the Projects screen MUST use the expanded two-pane layout confined to the area on one side of the hinge, or the compact layout if that area is too narrow.

**General**

- **FR-028**: The editor MUST work fully offline.
- **FR-029**: The interface MUST follow the design's visual language (dark theme, accent color for playhead and selection, fixed track colors, type scale) consistently on every screen.
- **FR-030**: Icon-only controls MUST have accessible labels, touch targets MUST be at least 48dp, and text MUST scale with the system font size.
- **FR-031**: Under memory or thermal pressure the app MUST degrade preview resolution, frame rate, and thumbnail size without losing edits or crashing, MUST show a non-blocking notice, and MUST pause playback with a message only when it cannot continue.

### Key Entities

- **Project**: A named editing session with creation and modification times, status (draft or exported), thumbnail, format (aspect ratio, resolution, frame rate), the record of media it uses, and a timeline.
- **Media Item**: A reference to an imported video, photo, or audio file, with type, duration, dimensions where applicable, and source location. It never contains a modified copy of the original.
- **Timeline**: The ordered sequence of clips in a project, with total duration and playhead position.
- **Clip**: A placement of a media item on the timeline, with start and end offsets within the source (the trim) and its position in the sequence.
- **Audio Clip**: A placement of an audio source on the audio lane, either sound detached from a video or an added audio file, with its own start and end offsets (trim), position, and muted state. Sound still attached to a video is part of that video's clip, not a separate audio clip.
- **Selection**: The ordered set of items chosen in Add media, with their selection order.
- **Edit History**: The ordered record of timeline changes that supports undo and redo.
- **Layout Posture**: The current screen arrangement (compact, expanded, tabletop) that determines which designed layout is shown.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A new user can create a project, import a video, and see it on the timeline in under 60 seconds.
- **SC-002**: Users can trim a clip to a chosen length on their first attempt in under 20 seconds.
- **SC-003**: Scrubbing and playback preview stay smooth, at 30 frames per second for 1080p footage, on a mid-range device.
- **SC-004**: Importing 20 mixed media files completes with all thumbnails visible in under 15 seconds.
- **SC-005**: After force-closing the app mid-edit, 100% of projects reopen with no more than 5 seconds of work lost.
- **SC-006**: 100% of trim, split, duplicate, move, and delete actions can be undone and redone back to the identical timeline state.
- **SC-007**: Original media files are byte-identical before and after editing in 100% of tested cases.
- **SC-008**: On each designed screen, a design review finds no deviation from the designer's layout, labels, hierarchy, and colors other than documented, approved ones.
- **SC-009**: Folding, unfolding, or rotating during editing preserves project, playhead, selection, and playback in 100% of tested transitions, with the new layout visible in under 1 second.
- **SC-010**: In tabletop posture, 100% of controls are reachable and none are covered by the hinge.
- **SC-011**: 90% of first-time testers complete the create, import, arrange, and trim flow without help.

## Assumptions

- The designer's UI Reference canvas is the approved design for the screens in scope; any needed deviation will be agreed with the designer before implementation.
- Target users are casual creators editing short videos on their own phone or foldable, with no prior editing experience.
- This feature delivers the main video lane and one audio lane. Additional tracks (picture in picture, text, extra audio and music lanes), effects, filters, speed and volume controls, and export are separate later features. Their entry points appear as designed but are inactive until delivered.
- The designer's clip tool bar for a selected video clip is Split, Speed, Volume, Detach, Duplicate, Delete, with Speed and Volume dimmed until their features exist. A selected detached or added audio clip shows Split, Mute, Replace, Duplicate, Delete (the "Detached audio selected" screen). The main tool bar's Audio, Text, Overlay, Effects, and Filters entries are dimmed; Edit is active.
- The editor's Export button is shown as designed but inactive until the export feature exists.
- The Projects bottom navigation entries Templates and Exports belong to later features; they appear as designed but inactive.
- The inspector panel on the unfolded editor shows the selected clip's name, length, and trim details; its Video, Audio, and Speed controls become active as their features arrive.
- The design's in-app gallery grid requires media access; this is requested just in time as the constitution requires, and the system's file chooser remains available as the no-broad-access alternative.
- Media is referenced from its original location rather than copied into the project; moved or deleted files are handled as in FR-012.
- Supported import formats follow the project constitution.
- Phone portrait is the primary compact layout; foldable inner-display layouts (unfolded and tabletop) and the cover screen are required, per the constitution.
- Performance targets align with the project constitution's real-time preview budget. "Mid-range device" means a phone from the last three years with about 6 GB of RAM and a mid-tier chipset; the exact reference devices are chosen at planning and recorded in the plan.
- The "Continue editing" card's progress bar shows how far the saved playhead position is through the project's total length. This is an assumption to confirm with the designer.
- The project thumbnail is the first frame of the first video or photo on the timeline.
- The owner's physical Samsung Fold is the primary foldable target; the exact model, OS version, and window sizes are recorded during planning spikes and used to validate the layout rules.
