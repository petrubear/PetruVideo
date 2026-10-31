# Feature Specification: Clip Speed and Volume

**Feature Branch**: `002-clip-speed-volume`

**Created**: 2026-10-31

**Status**: Draft

**Input**: User description: "002 Clip speed and volume. The Speed and Volume tools on a clip."

## Clarifications

### Session 2026-10-31

- Q: Do detached or added audio clips get a Speed tool, or only Volume? → A: Both Speed and Volume.
- Q: How do the Speed and Volume controls appear on the phone layout? → A: A bottom sheet over the clip tool bar, with the timeline and preview still visible above it.
- Q: What speed range? → A: 0.25x to 4x.
- Q: What if slowing an audio clip would overlap the next one? → A: Limit the speed to the slowest value that fits and tell the user; no other clip moves.
- Q: What volume range? → A: 0% to 200%, with the part above 100% marked as a boost.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Change a clip's volume (Priority: P1)

A user selects a video clip that has sound, or a detached or added audio clip, taps Volume in the clip tool bar, and drags a slider to make the clip quieter or louder. The change is heard immediately in the preview, the clip shows its level, and the edit can be undone.

**Why this priority**: Balancing the original sound against added music is the most common audio fix, and 001 already lets users layer music over original sound with no way to balance them. It is also the smaller of the two tools, so it delivers value first.

**Independent Test**: Add a video with sound and a music file over it, select the music, lower its volume to 30%, play, and hear it quieter while the video's own sound is unchanged. Undo and hear the original balance.

**Acceptance Scenarios**:

1. **Given** a selected video clip with sound, **When** the user taps Volume, **Then** a volume control opens showing the clip's current level (100% by default) and the Volume button is active, not dimmed.
2. **Given** the volume control is open, **When** the user drags to 40%, **Then** playback of that clip is audibly quieter right away and the level shows as 40%.
3. **Given** a selected video clip that has had its sound detached, **When** the user selects it, **Then** Volume is dimmed for the video clip (it has no sound now), and the detached audio clip has its own Volume.
4. **Given** a volume change was applied, **When** the user taps Undo, **Then** the previous level returns exactly; Redo restores the change.
5. **Given** a clip at 0% volume, **When** the timeline plays, **Then** that clip is silent and still shown as a normal clip (distinct from Mute, which is a separate on/off switch).

---

### User Story 2 - Change a clip's speed (Priority: P1)

A user selects a video clip, taps Speed, and chooses a speed from slow motion to fast forward. The clip plays faster or slower, its length on the timeline changes to match, its sound follows (kept at the original pitch), and everything after it on the main video lane moves to stay gapless.

**Why this priority**: Slow motion and fast forward are core to the product's CapCut-level goal and are the second of the two tools reserved in 001. It is larger because it changes clip length and ripples the main lane.

**Independent Test**: Place three clips, set the middle clip to 2x, confirm its timeline length halves, the third clip slides left to close the gap, playback of the middle clip runs twice as fast, and undo restores the original lengths and positions.

**Acceptance Scenarios**:

1. **Given** a selected video clip, **When** the user taps Speed, **Then** a speed control opens showing the current speed (1x by default) with quick choices and a way to pick any value in the allowed range.
2. **Given** a 10-second clip, **When** the user sets 2x, **Then** the clip becomes 5 seconds long on the timeline, the clips after it move earlier by 5 seconds, and the total length updates.
3. **Given** a 10-second clip, **When** the user sets 0.5x, **Then** the clip becomes 20 seconds long and later clips move later by 10 seconds.
4. **Given** a clip with attached sound at 2x, **When** it plays, **Then** the sound plays at double speed and keeps its original pitch.
5. **Given** a sped-up clip, **When** the user trims, splits, duplicates, or detaches its sound, **Then** each result keeps the same speed, the detached audio stays in step with the video, and the visible content matches what plays.
6. **Given** a selected photo, **When** the user looks at the clip tool bar, **Then** Speed is shown dimmed (a still image has no motion to change).

---

### User Story 3 - Speed and volume on audio clips (Priority: P2)

A user selects a detached or added audio clip and changes its speed (to stretch music to fit, or to speed up a voice clip) and its volume, with the same controls as for video.

**Why this priority**: It completes the pair for the audio lane, but the video-clip flows deliver the main value without it.

**Independent Test**: Add a 7-second audio clip, set speed 0.5x, see it become 14 seconds on the audio lane, and set volume 60%.

**Acceptance Scenarios**:

1. **Given** a selected audio clip, **When** the user taps Speed or Volume, **Then** the same controls open as for video clips and apply to that clip only.
2. **Given** an audio clip followed closely by another, **When** a slower speed would make it overlap the next one, **Then** the speed is limited to the slowest value that fits and the user is told why (audio clips never overlap, as in 001).
3. **Given** a selected audio clip, **When** the clip tool bar shows, **Then** it offers Speed and Volume alongside Split, Mute, Replace, Duplicate, and Delete, with the layout still fitting on the compact width.

---

### User Story 4 - See and reach speed and volume everywhere (Priority: P3)

Clips with a changed speed or volume show a small badge on the timeline. On the unfolded editor, the inspector shows the same controls, and in tabletop posture the controls sit below the hinge.

**Why this priority**: Visibility and large-screen placement are expected by the product's foldable-first rule, but they build on the working tools.

**Independent Test**: Change a clip to 1.5x, see the badge on its thumbnail strip, unfold the device, and change the same speed from the inspector.

**Acceptance Scenarios**:

1. **Given** a clip at a speed other than 1x or a volume other than 100%, **When** it appears on the timeline, **Then** it shows a compact badge with the value (for example "2x" or "40%").
2. **Given** the unfolded editor with a clip selected, **When** the inspector shows, **Then** its Speed and Volume controls are active and change the same values as the phone controls.
3. **Given** the device is in tabletop posture, **When** the user opens Speed or Volume, **Then** the control appears on the side of the hinge with the other controls and nothing interactive sits under the hinge.
4. **Given** the controls are open, **When** the device is folded, unfolded, or rotated, **Then** the control stays open with the same value and nothing is lost.

---

### Edge Cases

- A clip is sped up so much that it would fall below the 0.1 second minimum: the speed is limited to the fastest value that keeps the minimum, and a message says so.
- A clip is slowed to the minimum speed (0.25x): its length quadruples, with no other cap (long projects are allowed).
- The user drags the control continuously: each drag is one undo step, not one per movement.
- A clip with attached sound has no audio track at all (video without sound): Volume is not offered, Speed still works.
- Missing source media: Speed and Volume stay available on the clip's settings (they do not need the file); the clip still shows as missing.
- A project is saved and reopened, or the app is force-closed after a change: speed and volume come back exactly as set, with at most 5 seconds of work lost.
- Split at the playhead on a sped-up clip: the cut falls at the playhead's exact timeline position, and both halves together cover the original source range.
- Undo after a speed change that moved later clips: every clip returns to its earlier position and length.

## Requirements *(mandatory)*

### Functional Requirements

**Volume**

- **FR-001**: Users MUST be able to set the volume of a video clip that has sound, and of any detached or added audio clip, from 0% to 200% in steps no larger than 5%, with 100% as the default.
- **FR-002**: The volume control MUST open as a bottom sheet from the Volume button in the clip tool bar (the timeline and preview stay visible above it), show the current level as a number with the range above 100% marked as a boost, and apply changes to playback immediately while the user drags.
- **FR-003**: Volume MUST be offered only for clips that have sound; for a video clip without sound, or whose sound has been detached, the Volume button MUST be dimmed with a short explanation.
- **FR-004**: Volume and Mute MUST be independent: Mute silences a clip without changing its volume level, and unmuting restores the level.
- **FR-005**: Volume applies to the whole clip; changing it MUST NOT affect other clips, including a video's attached sound versus music over it.

**Speed**

- **FR-006**: Users MUST be able to set the speed of a video clip and of an audio clip from 0.25x to 4x, with 1x as the default, using quick choices (0.25x, 0.5x, 1x, 2x, 4x) and a continuous control for values in between, in a bottom sheet like the volume control.
- **FR-007**: A clip's length on the timeline MUST equal its trimmed source length divided by its speed, and the timeline MUST show and use that length everywhere (ruler, total time, trim handles, duration badge, playback).
- **FR-008**: On the main video lane, a speed change MUST move all later clips so the lane stays gapless; on the audio lane, a speed change MUST keep the clip's start and MUST NOT make it overlap another audio clip, limiting the speed to the slowest value that fits and telling the user.
- **FR-009**: A clip's attached sound MUST follow its speed and keep its original pitch.
- **FR-010**: Speed MUST NOT be offered for photos (the button is dimmed).
- **FR-011**: A speed change MUST be limited so that the clip's timeline length is never below 0.1 seconds, and the user MUST be told when a limit applied.

**Interaction with existing editing**

- **FR-012**: Trimming a sped clip MUST work in timeline time: the handles move by what the user sees, and the duration badge shows timeline length.
- **FR-013**: Split, duplicate, and detach audio MUST preserve speed and volume: both halves of a split keep both values; a duplicate keeps both; detached audio keeps the speed and volume of its video clip and stays in step with it.
- **FR-014**: Replacing an audio clip with another file MUST keep that clip's speed and volume settings.
- **FR-015**: Every speed or volume change MUST be undoable and redoable as one step per gesture, and undo MUST restore the exact earlier lengths and positions of all clips affected.
- **FR-016**: Speed and volume MUST be saved with the project, restored when it reopens, and covered by the same autosave guarantee as other edits.
- **FR-017**: Source media files MUST NOT be modified by speed or volume changes.

**Design and layout**

- **FR-018**: Speed and Volume MUST become active in the clip tool bar where the designer placed them (dimmed in 001), and the inspector panel on the unfolded editor MUST host the same controls.
- **FR-019**: A clip whose speed is not 1x or whose volume is not 100% MUST show a compact badge on the timeline with the value.
- **FR-020**: The controls MUST follow the existing visual language (dark theme, accent color, type scale) and fit the compact, medium, expanded, and tabletop arrangements; in tabletop no interactive content may sit under the hinge, and an open control MUST survive fold, unfold, and rotation.
- **FR-021**: Speed and volume controls MUST have accessible labels, touch targets of at least 48dp, and scalable text, and values MUST be readable by a screen reader.

**Preview**

- **FR-022**: Preview MUST play sped and volume-adjusted clips correctly across clip boundaries, with audio and video in step.

### Key Entities

- **Clip speed**: A multiplier from 0.25x to 4x on a video or audio clip; determines the clip's timeline length from its trimmed source range. Photos have no speed.
- **Clip volume**: A level from 0% to 200% on a clip that has sound, applied during playback and export; separate from the clip's mute state.
- **Clip** (video or audio, from 001): gains a speed and a volume; its timeline length becomes derived from speed.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can lower a music clip's volume and hear the result within 15 seconds of selecting the clip, on their first attempt.
- **SC-002**: A user can change a clip's speed and see the new length and shifted timeline within 15 seconds of selecting the clip, on their first attempt.
- **SC-003**: Changing speed or volume never makes preview drop below 30 frames per second at 1080p on a mid-range device, and a volume drag is heard within 100 ms.
- **SC-004**: 100% of speed and volume changes can be undone and redone back to the identical timeline, including positions and lengths of all later clips.
- **SC-005**: After force-closing the app right after a change, 100% of projects reopen with the same speed and volume (at most 5 seconds of work lost).
- **SC-006**: A clip at 2x plays at exactly twice the speed: its timeline length is within 1 frame of half the source length, and attached sound stays in step with video within 40 ms.
- **SC-007**: On each affected screen and arrangement (compact, expanded with inspector, tabletop), a design review finds no deviation from the existing visual language other than documented ones, and no control sits under the hinge.
- **SC-008**: Source media files are byte-identical before and after speed and volume edits in 100% of tested cases.

## Assumptions

- The designer's UI reference (canvas version 14) has no dedicated Speed or Volume artboard; the controls are designed in the existing visual language and reviewed with the designer, with any deviation recorded.
- Speed and volume apply to a whole clip; changing them over time (speed ramps, keyframed volume, fades) belong to later features.
- Reverse playback, freeze frames, and audio effects (pitch change, equalizer) are out of scope.
- Speed applies to video clips and to detached or added audio clips (confirmed); photos have no speed, and volume applies to any clip with sound.
- The speed range is 0.25x to 4x and the volume range is 0% to 200%, both confirmed.
- Slowing a clip makes it longer; there is no upper cap other than the speed limit itself, since long projects are already allowed.
- Export of speed and volume belongs to the export feature; this feature makes the preview and the saved project correct, and records the values the export feature will use.
- Feature 001's engine, undo history, autosave, clip tool bar, inspector, and fold layouts are in place and are reused.
