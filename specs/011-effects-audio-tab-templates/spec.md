# Feature Specification: Effects, Audio Tab, and Templates

**Feature Branch**: `011-effects-audio-tab-templates`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "run all the tree remaining items in this spec" (the three entries still disabled in the design: the Effects tab, the main Audio tab, and Templates)

## Scope note

Three entries that the design shows as "coming soon" become active: the Effects tab of the editor, the Audio tab of the editor, and Templates on the home screen. Not included: voice-over recording, music from online libraries, downloadable or user-made templates, accounts, keyframed effects, and effects on text or overlay clips.

## Clarifications

### Session 2026-11-01

- Q: Which transitions and effects ship? → A: Transitions: Dissolve, Fade through black, Slide, Cover, Zoom. Clip effects: Blur, Pixelate, Mirror, Shake. A short fixed set, each with one strength control.
- Q: Where does music come from? → A: An audio file picked from the phone. No online library and no recording in this feature.
- Q: Does a transition change the project length? -> A: Yes. The next clip starts earlier by the transition length, so the project gets shorter by that amount. Every other lane (text, overlay, audio including detached audio) keeps its times, as it does when a video clip is deleted today.
- Q: What do templates contain? → A: A built-in set of 6 ready-made starts. Each defines slots for the user's photos and videos, slot lengths, a filter look, a title, and transitions. No network, no downloads, no account.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Transitions and clip effects (Priority: P1)

A user opens the Effects tab. With a video lane that has two neighbouring clips, they pick the cut between them and choose a transition (Dissolve, Fade through black, Slide, Cover, Zoom) with a length from 0.2 s to 2 s. They can also select a clip and apply a clip effect (Blur, Pixelate, Mirror, Shake) with a strength slider. Both show live in the preview and export looks the same.

**Why this priority**: This turns the disabled Effects tab into the biggest missing creative tool.

**Independent Test**: Put two clips side by side, add Dissolve of 1 s at the cut, play across it and see the blend; apply Blur to one clip; export and compare frames with the preview; undo each.

**Acceptance Scenarios**:

1. **Given** two neighbouring video clips, **When** the user selects the cut and picks a transition, **Then** a marker appears at the cut on the timeline and the preview blends the clips over the chosen length.
2. **Given** a transition longer than half of either neighbouring clip, **When** the user sets it, **Then** the length is limited to that half and a short message says so.
3. **Given** a selected clip, **When** the user picks a clip effect and drags Strength, **Then** the preview changes live and the clip shows an "Fx" badge.
4. **Given** a transition or effect, **When** the user removes it or taps Reset, **Then** the clip or cut returns to normal.
5. **Given** a gesture that changes a value, **When** it ends, **Then** it is one undo step.
6. **Given** a clip with a transition on one side, **When** it is split, duplicated, deleted, or moved, **Then** transitions that no longer have two neighbours are removed, and effects stay with their clip parts.
7. **Given** a transition of length T at a cut, **When** it is added, **Then** the later clips start T earlier, the project is T shorter, other lanes keep their times, and removing the transition restores the length.
8. **Given** a transition and effects, **When** the project is saved, closed, and reopened, **Then** they are unchanged.

---

### User Story 2 - Audio tab and music (Priority: P2)

A user opens the Audio tab and sees the audio tools for the selected clip (Fade, Noise reduction, Equalizer, Voice effects, Volume) in one bar, plus an Add music button. Add music lets them pick an audio file from the phone; it appears in the audio lane at the playhead as its own clip with the same tools.

**Why this priority**: The tab is already visible but dead; music is the most asked-for audio feature.

**Independent Test**: Add a music file at 2 s, play and hear it under the video, fade it out, export and hear the same.

**Acceptance Scenarios**:

1. **Given** the Audio tab, **When** a clip with sound is selected, **Then** its audio tools are shown; with nothing selected, only Add music is active and the others are dimmed with the reason.
2. **Given** Add music, **When** the user picks an audio file, **Then** a clip starts at the playhead in the audio lane, is selected, and plays in the preview mixed with the main sound.
3. **Given** a file that is not audio or cannot be read, **When** it is picked, **Then** a short message says so and nothing is added.
4. **Given** a music clip, **When** the user moves, trims, splits, duplicates, deletes, sets volume, fade, noise reduction, EQ, or voice effects, **Then** it behaves as a detached audio clip does today.
5. **Given** a music clip longer than the video, **When** the project plays, **Then** the project length follows the longest lane.

---

### User Story 3 - Templates (Priority: P3)

A user taps Templates on the home screen and sees 6 ready-made starts with a preview of their style, length, and number of slots. They pick one, choose photos and videos for the slots in one multi-select, and a new project opens with the media placed, trimmed to the slot lengths, with the template's filter look, title, and transitions applied. They can edit it like any project.

**Why this priority**: Fastest path to a finished video, but it relies on the editing tools above.

**Independent Test**: Pick a 4-slot template, choose 4 videos, see a ready project, play it, change the title, export.

**Acceptance Scenarios**:

1. **Given** the home screen, **When** the user taps Templates, **Then** a list of 6 templates opens, each with name, length, slot count, and a sample of its look.
2. **Given** a template, **When** the user taps Use, **Then** a media picker opens that accepts up to the slot count of videos and photos.
3. **Given** fewer items than slots, **When** the user confirms, **Then** the template is filled in order and unused slots are dropped; at least one item is required.
4. **Given** more items than slots, **When** the user confirms, **Then** only the first slot-count items are used and a message says so.
5. **Given** a video shorter than its slot, **When** the template is applied, **Then** the slot shortens to the video length.
6. **Given** a filled template, **When** the project opens, **Then** it is a normal project: every clip, title, filter, and transition can be edited, undone, saved, and exported.
7. **Given** the user cancels the picker, **When** they return, **Then** no project is created.

---

### Edge Cases

- A transition at the first or last clip edge, or between a clip and a gap, is not offered.
- A clip may have a transition on both sides; each is limited to half of the clip, so the two always fit.
- Two transitions at the same cut cannot exist; choosing a new one replaces the old one.
- A transition between a photo and a video, or two photos, works the same.
- Effects on a clip with a filter stack in the defined order: filter, then effect.
- Reduced preview quality (Settings or device pressure) may drop effects and turn transitions into hard cuts in the preview only; export always renders them.
- A music file with a very long length is shown trimmed to the project length only on export; the clip keeps its full length.
- A template picked while media access is missing shows the same permission message as importing.
- Project duplicate and relink handle music clips and effects like other media.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The Effects tab MUST be active in the editor tab bar and the unfolded rail, and MUST offer transitions and clip effects.
- **FR-002**: Users MUST be able to add, change, and remove a transition at the cut between two neighbouring video-lane clips, choosing one of Dissolve, Fade through black, Slide, Cover, Zoom and a length from 0.2 s to 2 s.
- **FR-003**: A transition length MUST NOT exceed half of the shorter neighbouring clip, and the user MUST be told when it is limited.
- **FR-004**: Users MUST be able to apply one clip effect (Blur, Pixelate, Mirror, Shake) to a video-lane clip with a strength from 0% to 100%, and remove or reset it.
- **FR-005**: Transitions and effects MUST show live in the preview, with each value change as one undo step.
- **FR-006**: Transitions and effects MUST be saved with the project, kept by duplicate project and relink, and handled by split, duplicate, delete, and move so the timeline never holds a transition without two neighbours.
- **FR-006a**: A transition MUST shorten the video by its length; text, overlay, and audio clips (including detached audio) MUST keep their times.
- **FR-007**: Export MUST render transitions and effects as the preview shows them.
- **FR-008**: The timeline MUST show a marker for each transition and a badge for each clip with an effect.
- **FR-009**: The Audio tab MUST be active and show the audio tools for the selected clip, with unavailable tools dimmed with a reason.
- **FR-010**: Users MUST be able to add an audio file from the phone as a music clip in the audio lane at the playhead, and reject files that are not usable audio with a message.
- **FR-011**: Music clips MUST support move, trim, split, duplicate, delete, volume, mute, fade, noise reduction, equalizer, and voice effects, and appear in preview and export.
- **FR-012**: Templates MUST be active on the home screen and list 6 built-in templates with name, length, slot count, and a style sample.
- **FR-013**: Applying a template MUST create a new project from the user's chosen media following FR-014 and open it in the editor.
- **FR-014**: A template defines ordered slots with lengths, a filter look, a title, and transitions; filling places media in order, trims videos to the slot (shortening the slot when the video is shorter), uses photos for the slot length, and drops unused slots.
- **FR-015**: A project made from a template MUST be an ordinary project with no link back to the template.
- **FR-016**: Cancelling any picker MUST leave the project list and the timeline unchanged.
- **FR-017**: All new controls MUST work in the portrait, unfolded, and tabletop layouts, meet touch-target and contrast rules, and have accessible labels.

### Key Entities *(include if feature involves data)*

- **Transition**: Sits at the cut between two neighbouring video-lane clips; has a kind and a length.
- **Clip effect**: Belongs to one video-lane clip; has a kind and a strength. At most one per clip.
- **Music clip**: An audio-lane clip from a user-picked audio file; shares the audio settings of detached audio clips.
- **Template**: A built-in definition of ordered slots (length, media kind), a filter look, a title, and transitions; not stored with projects.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can add a transition at a cut in 4 taps or fewer from the Effects tab.
- **SC-002**: A user can make a finished project from a template, from tapping Templates to the editor, in under 60 seconds with 4 items chosen.
- **SC-003**: A user can add music to a project in 5 taps or fewer from the Audio tab.
- **SC-004**: The preview keeps its normal responsiveness with one transition and two effects on screen; the playhead never stalls longer than 0.5 s.
- **SC-005**: An exported frame inside a transition or effect matches the preview frame at the same time within a small visual difference in an automated comparison.
- **SC-006**: 100% of transitions, effects, and music clips survive close and reopen, split, duplicate, undo, and redo in automated tests.
- **SC-007**: The three entries no longer show "coming soon" in any layout.

## Assumptions

- Audio files the phone's picker returns in common formats (MP3, AAC/M4A, WAV) are usable; other formats are tried and rejected with a message if they cannot be read.
- Templates are shipped with the app, and previews are drawn from the template definition rather than from video files.
- Fold, device, and designer reviews follow the earlier features and are done after the build, not before.
- Existing limits apply: undo depth, project length, and preview-quality rules stay as they are.
- Voice-over recording is a later feature.
