# Feature Specification: Project Card Preview

**Feature Branch**: `028-project-card-preview`

**Created**: 2026-11-04

**Status**: Draft

**Input**: User description: "Show a preview of the project's first video on the project card in the Projects screen (and the Continue editing card) instead of the colored placeholder box. The preview comes from the first video clip on the project's timeline. It should update when the first clip changes, work for photos and videos, and fall back to the colored box when there is no clip or the media is missing. Investigate why the existing first-frame thumbnail is not shown today, and consider a short silent looping motion preview of the first clip as an enhancement over a still frame."

## Scope note

Every place that lists a project (the Continue editing card, the project grid cards, the project rows, and the project detail pane) shows a colored gradient box today. This feature replaces that box with a still picture of the first clip's first shown frame, so a project can be recognised at a glance. When there is no clip, or the source media is missing, the colored box stays. Not included: a moving preview of the clip (a possible later feature).

## Clarifications

### Session 2026-11-04

- Q: Is the motion preview part of this feature? → A: No. This feature delivers only the still picture; the silent looping preview is left for a later feature.

## Findings on the current behaviour

- The editor already makes a first-frame picture of the first clip and saves its location on the project whenever the first clip changes (the project has a thumbnail reference).
- The Projects screen never reads that reference: all four project views draw the colored box only. So the missing preview is a display gap, not a missing picture.
- The saved picture is made only while a project is open in the editor, so projects created by templates or imports and never opened, and projects made before the picture existed, have none until opened once.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - See what a project is by its picture (Priority: P1)

A user opens the Projects screen. Each project that has at least one clip shows the first frame of its first clip instead of a colored box, on the Continue editing card, the grid cards, the list rows, and the detail pane.

**Why this priority**: This is the whole value: recognising a project without opening it.

**Independent Test**: Make a project with a video as its first clip, close it, open the Projects screen, and check every project view shows that video's frame, not the colored box.

**Acceptance Scenarios**:

1. **Given** a project whose first clip is a video, **When** the Projects screen shows it, **Then** the card shows a frame of that video.
2. **Given** a project whose first clip is a photo, **When** the Projects screen shows it, **Then** the card shows that photo.
3. **Given** a project with no clips, **When** the Projects screen shows it, **Then** the colored box is shown as today.
4. **Given** the project's picture is shown, **When** the card is smaller or larger (grid, row, detail pane), **Then** the picture fills the area without stretching.

---

### User Story 2 - The picture follows the first clip (Priority: P1)

A user reorders, deletes, replaces, or trims the start of the first clip, and the project's picture changes to match when they return to the Projects screen.

**Why this priority**: A stale picture is worse than a colored box, because it names the wrong project.

**Independent Test**: Change the first clip of a project, go back, and check the card shows the new first clip.

**Acceptance Scenarios**:

1. **Given** a project showing clip A, **When** the user moves clip B to the start and goes back, **Then** the card shows clip B.
2. **Given** a project showing a frame, **When** the user trims the start of the first clip, **Then** the card shows the frame now at the start.
3. **Given** a project showing a frame, **When** the user deletes every clip, **Then** the card returns to the colored box.

---

### User Story 3 - Projects never opened still get a picture (Priority: P2)

Projects made from a template, or made before this feature, show their picture without the user opening each one.

**Why this priority**: Without it, most existing projects would stay colored boxes until opened.

**Independent Test**: Make a project from a template without opening it in the editor, go to the Projects screen, and check a picture appears; install over an older version and check old projects get pictures.

**Acceptance Scenarios**:

1. **Given** a project with clips and no saved picture, **When** the Projects screen is shown, **Then** the picture is made in the background and appears without leaving the screen, while the colored box shows until then.
2. **Given** many projects without pictures, **When** the Projects screen opens, **Then** the screen stays responsive and scrolling does not stutter.

---

### Edge Cases

- The first clip's source file was deleted or is unreadable: the colored box is shown, not a broken image, and the project still opens.
- The first clip is a very short or very large video (4K, HDR, odd rotation): the picture is the right way up and not stretched.
- A portrait, landscape, or square source: the picture is centered and cropped to fill the card without stretching.
- The first item is a gap, text, or an overlay with no video clip before it: the first video clip is used; with none, the colored box.
- The saved picture file is deleted by the system or the user clears storage: it is made again.
- Many projects scroll quickly: pictures load without blocking scrolling and stale cards do not show another project's picture.
- A project is deleted or duplicated: its picture is removed, or the copy gets its own.
- Several pictures are made at once on a low-memory phone: work is limited so the app does not slow down or run out of memory.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Every project view (Continue editing card, grid card, list row, detail pane) MUST show the project's picture when it has one and the colored box otherwise.
- **FR-002**: The picture MUST come from the first video or photo clip on the project's timeline, at the moment the clip starts playing in the project (so a trimmed start is respected).
- **FR-003**: The picture MUST fill its area without stretching, cropping the sides or top and bottom as needed, and MUST be the right way up.
- **FR-004**: The picture MUST be updated when the first clip changes, is trimmed at the start, is replaced, moved, or removed, and MUST be cleared when the project has no clip.
- **FR-005**: A project that has clips but no picture MUST get one made in the background when the Projects screen is shown, without blocking the screen; the colored box MUST show until it is ready.
- **FR-006**: A missing, unreadable, or deleted source MUST show the colored box and MUST NOT crash or leave a broken image.
- **FR-007**: Pictures MUST be kept on the phone only, MUST be removed when their project is deleted, and MUST be made again if the file is lost.
- **FR-008**: Making and showing pictures MUST NOT noticeably slow the Projects screen: scrolling stays smooth with 50 projects.
- **FR-009**: The picture MUST work on every window size (phone, folded, unfolded, tabletop) and keep its place when the screen changes size.

### Key Entities *(include if feature involves data)*

- **Project picture**: A small image of the first clip's first shown frame, stored on the phone and linked to one project; replaced whenever the first clip changes and removed with the project.
- **First clip**: The first video or photo clip on the main video lane of the project's timeline, with its media and its trimmed start.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of projects whose first clip is readable show a picture of it on the Projects screen after it has been shown once, in every project view.
- **SC-002**: After changing the first clip, the picture is correct the next time the Projects screen is shown, with no manual refresh.
- **SC-003**: A picture appears within 2 seconds of the Projects screen opening for a project that had none, for a typical short clip, without the screen freezing.
- **SC-004**: Scrolling a list of 50 projects with pictures shows no visible stutter or blank frames on a mid-range phone.
- **SC-005**: A project whose source was deleted opens and lists without error 100% of the time and shows the colored box.

## Assumptions

- The first clip is the first clip on the main video lane; overlays, text, and audio do not count.
- Only the still picture is in scope; a motion preview can be specified separately later.
- The picture shows the clip as it is, without the project's filters, effects, or text.
- The existing first-frame picture made by the editor is reused; this feature adds showing it, refreshing it for projects not opened.
- Making and showing the picture needs no new permission and no network.
- Projects shared with the Exports screen keep their current thumbnail behaviour.
