# Feature Specification: Overlay Audio

**Feature Branch**: `029-overlay-audio`

**Created**: 2026-11-04

**Status**: Draft

**Input**: User description: "The picture-in-picture videos should be treated as a base video: with audio, and with the capacity of detaching audio and adding audio effects. The user wants to record several music instruments, put each as an overlay over a base video, and hear them all together, mixed with the base video, in the preview and in the export."

## Scope note

Today an overlay clip (picture-in-picture) is silent: only the base video's sound and the audio lane are heard. This feature gives an overlay with a video that has sound the same sound abilities a base-lane video clip has: its own sound plays, mixed with everything else, and the user can change its volume, mute it, add the same audio effects (reverb, equaliser, noise reduction, voice effects, fades), detach its sound to the audio lane, and split its voice from the rest. The use case is a multi-instrument performance: each instrument is recorded on its own video, placed as an overlay over a base video, and all are heard together. Not included: speed change or reverse for overlays (they have none today), ducking of other sound under an overlay, and separate sound for photo overlays.

## Clarifications

### Session 2026-11-04

- Q: If the phone preview cannot play overlay sound reliably, what should the editor preview do? -> A: Try to play every overlay sound; if it cannot, keep the picture running, play the base sound only, and show a notice. The export always has everything.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Hear the overlays together with the base video (Priority: P1)

A user has a base video and two overlays, each a video of a different instrument. They play the project and hear the base video and both overlays at once, each at the moment its overlay is on the timeline, in the preview and in the exported file.

**Why this priority**: This is the whole point: playing instruments together.

**Independent Test**: Make a project with a base video and two overlays whose sounds are different tones, export it, and check the exported sound contains all three at the right times.

**Acceptance Scenarios**:

1. **Given** a base video and an overlay with sound, **When** the user plays the project, **Then** both sounds are heard together while the overlay is on the timeline, and only the base sound before and after.
2. **Given** three overlays that overlap in time, **When** the user exports, **Then** the exported sound has all of them mixed with the base video.
3. **Given** an overlay trimmed to start 2 seconds into its source and placed at 5 seconds, **When** the project plays, **Then** its sound starts at 5 seconds and begins at the trimmed point of the source.
4. **Given** an overlay with no sound track, or a photo overlay, **When** the project plays, **Then** it adds no sound and nothing fails.

---

### User Story 2 - Volume and mute for each overlay (Priority: P1)

The user selects an overlay and sets how loud it is, or mutes it, so the instruments can be balanced.

**Why this priority**: Without a balance control the mix is unusable; it is needed with the first story.

**Independent Test**: Select an overlay, set its volume to 50% and then mute it, and check the preview and export follow each change.

**Acceptance Scenarios**:

1. **Given** a selected overlay with sound, **When** the user opens its sound controls, **Then** a volume control and a mute control are shown.
2. **Given** the overlay is muted, **When** the project plays or is exported, **Then** the overlay adds no sound and its picture still shows.
3. **Given** the user changes the volume while playing, **When** they let go, **Then** the new volume is heard from then on without restarting playback.
4. **Given** a volume or mute change, **When** the user presses undo, **Then** the earlier value returns.

---

### User Story 3 - Audio effects on an overlay (Priority: P2)

The user adds the same audio effects they can add to a base-lane video clip (reverb, equaliser, noise reduction, voice effects, fade in and out) to an overlay's sound.

**Why this priority**: It gives each instrument its own character; useful, but the mix works without it.

**Independent Test**: Add reverb and a fade-in to one overlay and check that only that overlay's sound changes, in the preview and the export.

**Acceptance Scenarios**:

1. **Given** a selected overlay with sound, **When** the user opens audio effects, **Then** they see the same effects as for a base-lane video clip.
2. **Given** reverb on one overlay, **When** the project plays, **Then** only that overlay's sound has reverb and the base video and other overlays are unchanged.
3. **Given** a fade-in on an overlay, **When** the project plays, **Then** the overlay's sound rises over that time at its start.

---

### User Story 4 - Detach an overlay's sound (Priority: P2)

The user detaches the sound of an overlay so it becomes an audio clip on the audio lane, and the overlay becomes silent, like detaching from a base-lane clip.

**Why this priority**: Matches the base video behaviour the user asked for; lets them move or edit the sound on its own.

**Independent Test**: Detach the sound of an overlay and check an audio clip appears at the overlay's start with its length, the overlay is silent, and playing sounds the same as before.

**Acceptance Scenarios**:

1. **Given** a selected overlay with sound, **When** the user chooses Detach audio, **Then** an audio clip with that sound appears on the audio lane at the overlay's start and length, and the overlay no longer plays sound.
2. **Given** the audio lane has no free layer at that time, **When** the user detaches, **Then** nothing changes and a clear message says why.
3. **Given** a detached overlay, **When** the user presses undo, **Then** the sound returns to the overlay and the audio clip is removed.

---

### User Story 5 - Split the voice of an overlay (Priority: P3)

The user can split the voice from the other sound of an overlay, the same as for a base-lane clip, so for example a singing voice and a guitar on one recording can be treated apart.

**Why this priority**: The existing voice split is for base clips only; extending it is useful but not needed for the first mix.

**Independent Test**: Split an overlay's voice and check two audio clips appear at the overlay's place, Voice and Backing, and the overlay is silent.

**Acceptance Scenarios**:

1. **Given** a selected overlay with sound up to the supported length, **When** the user chooses Split voice, **Then** the same flow as for a base-lane clip runs and ends with Voice and Backing audio clips at the overlay's place.
2. **Given** the overlay is too long for the split, **When** the user opens the tool, **Then** it is disabled with the same reason shown for base-lane clips.

---

### Edge Cases

- An overlay whose source has no sound track: no sound control is offered, or it is disabled with "No sound in this clip".
- A photo overlay: no sound controls.
- An overlay trimmed or moved: its sound follows the picture exactly, with no drift against the picture over the whole project.
- Moving, trimming, splitting, duplicating or deleting an overlay moves, cuts, copies, or removes its sound with it; splitting an overlay gives each part its own volume, mute and effects as they were.
- Several overlays at the same time, plus the audio lane's layers and the base video: all are heard; the sound never clips harshly (loud sums are limited, not distorted).
- Existing projects made before this feature: their overlays stay silent so old exports do not change.
- An overlay whose source file is missing: no sound, and the existing missing-media message applies.
- The overlay's keyframed opacity at 0% hides the picture but does not mute the sound.
- A very long overlay or many overlays on a low-memory phone: the preview may lower sound effects quality the same way it does for base clips, and never stops playing.
- The project is exported at a different length than the overlay: overlay sound outside the project's length is cut.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: An overlay clip with a video that has a sound track MUST play that sound, mixed with the base video's sound, the audio lane, and all other overlays, in the preview and in the export.
- **FR-002**: An overlay's sound MUST start, stop, and be trimmed together with its picture (same start on the timeline, same source range) and MUST stay in sync with its picture for the whole length.
- **FR-003**: Each overlay with sound MUST have a volume control (0% up to the same maximum as base-lane clips) and a mute control; muting MUST NOT hide the picture.
- **FR-004**: Each overlay with sound MUST offer the same audio effects as a base-lane video clip: reverb, equaliser, noise reduction, voice effects, and fade in and out, applying to that overlay's sound only.
- **FR-005**: The user MUST be able to detach an overlay's sound to an audio-lane clip at the overlay's start and length; the overlay then plays no sound. Detaching MUST be refused with a clear message when the audio lane has no free layer there.
- **FR-006**: The user MUST be able to split the voice of an overlay with sound, with the same limits and flow as for base-lane clips, giving Voice and Backing audio clips.
- **FR-007**: Overlays with no sound track, photo overlays, and overlays that are not on the timeline at the moment MUST add no sound and MUST NOT cause errors.
- **FR-008**: New overlays with sound MUST start with sound on at full volume. Overlays that exist in projects made before this feature MUST keep playing no sound until the user turns sound on.
- **FR-009**: Moving, trimming, splitting, duplicating, and deleting an overlay MUST apply to its sound, its volume, mute and effects, as one undoable step.
- **FR-010**: Every change to an overlay's sound (volume, mute, effects, detach, split voice) MUST be undoable and MUST be saved with the project.
- **FR-011**: The mixed sound MUST NOT be distorted when many sources play at once; at most the same number of simultaneous sounds as the current limits MUST be supported without stopping playback or export.
- **FR-012**: The preview MUST try to play every overlay sound together with the base video. If it cannot do so reliably on the phone, it MUST keep the picture running without freezing, play the base sound only, and show a notice that overlay sound is heard in the export; the export MUST always contain all overlay sounds.
- **FR-013**: The overlay sound controls MUST work at compact, medium and expanded window sizes and in tabletop posture, and keep their state across fold and unfold.

### Key Entities *(include if feature involves data)*

- **Overlay sound settings**: For each overlay: whether it has sound on, its volume, its mute, and its audio effects (reverb, equaliser, noise reduction, voice effects, fades). Saved with the overlay in the project.
- **Overlay clip**: The existing picture-in-picture clip with its place, size, opacity, chroma key and mask; it now also carries its sound settings.
- **Detached overlay sound**: An audio-lane clip made from an overlay's sound, with the overlay's start, length, volume and effects at the time.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In a project with a base video and three overlays, the exported file contains the sound of all four at the right times, checked by comparing the exported sound with the sum of the sources, with a start offset of at most 40 ms.
- **SC-002**: Muting an overlay removes its sound from the export completely (no audible sound from it) and does not change the other sounds.
- **SC-003**: A user can set up a four-instrument mix (add three overlays, set volumes, add reverb to one) in under 3 minutes.
- **SC-004**: Detaching an overlay's sound and playing gives the same sound as before detaching, within the same tolerance as SC-001.
- **SC-005**: Opening a project made before this feature gives exactly the same exported sound as before, 100% of the time.
- **SC-006**: Playing a project with three sounding overlays for 30 seconds shows no picture freeze and no sound dropout on the reference phones.

## Assumptions

- "Treated as a base video" means the same sound abilities as a base-lane video clip; speed change and reverse stay out of scope because overlays have neither today.
- The audio effects available for overlays are exactly the ones base-lane clips have at the time of this feature; new effects added to base clips later should reach overlays the same way.
- Sound on by default for new overlays, off for existing ones, is the safest default so old projects do not change.
- The existing limit of three simultaneous audio-lane layers stays; overlay sounds are mixed on their own and do not use audio-lane layers until detached.
- Ducking (lowering other sound under an overlay or voice) is out of scope.
- The preview picture freeze seen on some phones with several video inputs is a separate known issue; this feature needs it solved or worked around for overlay playback to be reliable in the editor, and the export is the reference for the final result.
