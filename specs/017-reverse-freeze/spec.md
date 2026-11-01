# Feature Specification: Reverse and Freeze Frame

**Feature Branch**: `017-reverse-freeze`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "Reverse and freeze frame: on a selected video clip the user can reverse it so it plays backwards (picture and its attached sound), and can freeze a frame: at the playhead, a still of that frame is inserted as its own clip of a chosen length (default 2 seconds, from 0.5 to 10 seconds) splitting the clip into two around it so the video continues after the freeze. Both work with the clip's trim, speed, volume, fade, audio tools, keyframes, transform, filter, effect and transitions, show in the preview, are saved with the project, are exported exactly as in the preview, follow split, duplicate and relink, and each change is one undo step with a Reset for reverse. Photos cannot be reversed."

## Scope note

Two tools for video clips on the main track. Reverse makes a clip play backwards, picture and attached sound together, and can be turned off again. Freeze frame holds one frame of the video for a chosen time: the clip is cut at the playhead, a still of that frame goes in between as its own clip, and the video continues after it. Not included: reversing photos, reversing only the picture or only the sound, freezing at the end of the last clip, and reversing clips longer than 30 seconds.

## Clarifications

### Session 2026-11-01

- Q: How long can a reversed clip be? → A: Up to 30 seconds of the clip as trimmed. Longer clips are refused with a short message that suggests trimming or splitting first.
- Q: Is reversing instant? → A: No. The reversed picture and sound are prepared on the phone with a progress indicator; the clip becomes reversed when the preparation is done, and the user can cancel. Until then it plays as before.
- Q: What does the freeze still become? → A: A photo-style clip with no sound. It can be moved, trimmed (0.5 to 10 seconds), split, duplicated, deleted, and given a transform, filter, effect, keyframes, and transitions like any other clip.
- Q: What is the "Reset" for reverse? → A: Tapping Reverse again returns the clip to playing forwards; the Reverse control shows whether it is on.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Freeze a frame (Priority: P1)

A user plays a clip, stops on a moment, and taps Freeze frame. The clip is cut at the playhead, a still of that exact frame appears between the two parts for 2 seconds, and the video continues after it. They can change the still's length with its trim handles, or choose a length in the Freeze panel before inserting.

**Why this priority**: Freezing is the quickest visible win and the building block for effects like title moments.

**Independent Test**: Put a clip in the project, stop at a distinctive frame, tap Freeze frame, see the three clips, play across the freeze and see the still, export and compare the frame in the freeze with the frame at the playhead.

**Acceptance Scenarios**:

1. **Given** a selected clip with the playhead inside it, **When** the user taps Freeze frame, **Then** the clip is split at the playhead, a still clip of 2 seconds holding the frame shown at the playhead is inserted between the parts, and everything after it moves later by the still's length.
2. **Given** the Freeze panel, **When** the user chooses a length from 0.5 to 10 seconds, **Then** the still is inserted with that length.
3. **Given** the playhead at the very start or end of a clip, **When** the user taps Freeze frame, **Then** the still is inserted before or after the clip without making an empty part.
4. **Given** a frozen still, **When** the user plays across it, **Then** the picture does not move and there is no sound from it, while sound of other lanes continues.
5. **Given** a clip with speed, a filter, or a transform, **When** a frame is frozen, **Then** the still looks exactly like that moment in the preview (the transform, filter, and effect of the clip are kept on both parts and on the still).
6. **Given** a freeze, **When** it is made, **Then** it is one undo step that restores the original clip.

---

### User Story 2 - Reverse a clip (Priority: P2)

A user selects a clip and taps Reverse. A progress indicator shows while the backwards version is prepared; when ready, the clip plays backwards with its attached sound. Tapping Reverse again returns it to forwards.

**Why this priority**: Reverse is a popular effect, but it is more work for the phone than a freeze.

**Independent Test**: Reverse a 5 second clip with a clear start and end, wait for it, play it and see and hear it backwards, export and compare frames at the start and end, then turn Reverse off.

**Acceptance Scenarios**:

1. **Given** a selected clip of at most 30 seconds, **When** the user taps Reverse, **Then** a progress indicator with a Cancel button shows until the backwards version is ready, and then the clip plays backwards (picture and attached sound).
2. **Given** a clip longer than 30 seconds, **When** the user taps Reverse, **Then** a short message says the clip is too long and suggests trimming or splitting it.
3. **Given** a reversing in progress, **When** the user taps Cancel or leaves the project, **Then** nothing changes in the clip and no partial result is kept.
4. **Given** a reversed clip, **When** the user taps Reverse again, **Then** it plays forwards again, in one undo step.
5. **Given** a photo clip, **When** it is selected, **Then** Reverse is shown as unavailable with the reason "Photos cannot be reversed".
6. **Given** a reversed clip, **When** the user trims, changes the speed, volume, fade, or audio tools, adds keyframes, a transform, a filter, an effect, or a transition, **Then** each works as on a forward clip and is applied to the backwards playback.

---

### User Story 3 - Kept, saved, exported (Priority: P3)

Reverse and freeze stay right when clips are split, duplicated, or relinked; are saved with the project; and appear in an export exactly as in the preview. The tools work in the unfolded and tabletop layouts.

**Why this priority**: Persistence and edit rules from earlier features apply.

**Independent Test**: Reverse a clip, split it, duplicate it, freeze a frame in another clip, save and reopen, unfold, export, and compare frames.

**Acceptance Scenarios**:

1. **Given** a reversed clip, **When** it is split, **Then** both parts play backwards and the part that was later in time is first in time order after the split, as the picture shows.
2. **Given** a reversed clip, **When** it is duplicated or its project is duplicated, **Then** the copies are reversed too.
3. **Given** a reversed clip, **When** it is trimmed, **Then** the part that remains is the matching part of the backwards playback.
4. **Given** a project with reversed clips and freezes, **When** it is closed and reopened, **Then** they are unchanged and a reversed clip plays at once if its backwards version still exists, or is prepared again in the background if it does not.
5. **Given** a clip that is relinked to another file, **When** it was reversed, **Then** it is prepared again for the new file; freezes keep their still.
6. **Given** an exported video, **When** a frame is compared with the preview at the same time, **Then** picture and sound match within a small difference, for reversed clips and freezes.

---

### Edge Cases

- Reversing uses the clip's trimmed part only; changing the trim later keeps the reversed state.
- A reversed clip's attached sound plays backwards; if the clip is muted or has no sound, only the picture is reversed.
- Freezing a clip that is already reversed freezes the frame the viewer sees at the playhead.
- Freezing inside a transition overlap uses the frame of the clip the playhead is on, before the transition.
- A freeze at a transition boundary keeps each transition with its clip's neighbour; the transition out of the original clip stays on the last part.
- Freeze length is limited to 10 seconds and cannot be shorter than 0.5 s; the still can be lengthened by trimming up to that limit and lengthened beyond only by inserting another freeze.
- Missing media: Reverse and Freeze are unavailable with a short reason.
- A very short clip (under 1 second) can be frozen but only reversed if it is at least the minimum clip length.
- Low storage: Reverse refuses with a message when the space for the backwards version is not there.
- Reduced preview quality still plays reversed clips and freezes.
- Undo and redo restore the exact earlier state; undoing a reverse that was already prepared does not delete the prepared version.
- Prepared backwards versions are removed when no clip uses them any more.
- Relink, remove-missing, project duplicate keep the reversed state; templates carry none.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Users MUST be able to freeze a frame of a selected main-track video clip at the playhead, with a length of 0.5 to 10 seconds (default 2 seconds); the still MUST be inserted as its own clip between the two parts of the original and later clips MUST move later by its length.
- **FR-002**: A freeze MUST hold exactly the frame the preview shows at the playhead, including the clip's transform, filter, effect, and speed, and the still MUST have no sound.
- **FR-003**: A freeze at the very start or end of a clip MUST insert the still before or after it without an empty part.
- **FR-004**: Users MUST be able to reverse a selected main-track video clip of up to 30 seconds (as trimmed); picture and attached sound MUST play backwards.
- **FR-005**: Reversing MUST show progress, MUST be cancelable, MUST leave the clip unchanged on cancel or failure, and MUST refuse with a short message for clips that are too long, photos, missing media, or too little storage.
- **FR-006**: Users MUST be able to turn Reverse off again in one step; every reverse change and every freeze MUST be one undo step.
- **FR-007**: Reverse and freeze MUST work with the clip's trim, speed, volume, fade, audio tools, keyframes, transform, filter, effect, and transitions.
- **FR-008**: The preview MUST play reversed clips and freezes correctly, including across transitions.
- **FR-009**: Export MUST produce the same picture and sound as the preview for reversed clips and freezes.
- **FR-010**: Split, duplicate, trim, move, speed change, delete, relink, project duplicate, and undo/redo MUST keep the reversed state and freezes consistent as in User Story 3.
- **FR-011**: Reversed state and freezes MUST be saved with the project and survive close, reopen, and process death; backwards versions MUST be rebuilt when missing and removed when unused.
- **FR-012**: The tools MUST be offered for main-track video clips only; Reverse MUST be shown unavailable with a reason where it cannot be used.
- **FR-013**: All new controls MUST work in the portrait, unfolded, and tabletop layouts, meet touch-target and contrast rules, and have accessible labels, including the progress indicator.

### Key Entities *(include if feature involves data)*

- **Reversed clip**: A video clip marked to play backwards, with a prepared backwards version of its trimmed part.
- **Freeze clip**: A still clip made from one frame of a video clip, with a chosen length and no sound.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can freeze a frame in under 10 seconds from selecting the clip.
- **SC-002**: A 10 second clip is reversed within 60 seconds on a mid-range phone, with a visible progress indicator the whole time.
- **SC-003**: The exported frame at any moment of a reversed clip or freeze matches the preview frame within a small visual difference in an automated comparison.
- **SC-004**: The sound of a reversed clip is the original sound played backwards: in an automated comparison the exported audio matches the reversed source within a small tolerance.
- **SC-005**: 100% of reverse and freeze settings survive close and reopen, split, duplicate, trim, speed change, undo, and redo in automated tests.
- **SC-006**: Cancelling a reverse leaves no files behind and no change in the project in 100% of tested cases.
- **SC-007**: Every control is reachable in 3 taps or fewer from a selected clip.

## Assumptions

- Reversed pictures keep the original resolution and frame rate of the clip's trimmed part.
- The backwards version is kept in the app's own storage and is not shown in the user's gallery.
- A freeze still is stored as a picture in the app's storage with the project and is not shown in the gallery.
- Fold, device, and designer reviews follow the earlier features and are done after the build, not before.
- Existing limits apply: undo depth, project length, and preview-quality rules stay as they are.
