# Feature Specification: Audio Ducking

**Feature Branch**: `015-audio-ducking`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "Audio ducking: the user can turn on ducking for a music or other audio clip so its volume is lowered automatically while other sound plays on top of it, such as a voice-over, speech in a video clip, or another audio clip. The user chooses how much it is lowered (amount) and how quickly it fades down and back up (smoothness), sees at a glance on the timeline which clips are ducked, hears the effect live in the preview, and gets the same result in the exported video. Ducking is saved with the project, follows trims, splits, moves and speed changes, and works with the clip's volume, fade, and other audio tools."

## Scope note

A clip on the audio lane (music, voice-over, detached or added sound) can be set to duck: its volume goes down on its own while other sound is playing and comes back when that sound stops. Sounds that trigger ducking are the sound of video clips, and the sound of other audio-lane clips that are not ducking themselves. Not included: ducking for the sound of video clips, ducking by sound outside the project, a per-moment volume curve the user draws, and choosing which clips trigger it.

## Clarifications

### Session 2026-11-01

- Q: What triggers ducking? → A: Real loudness. The clip is lowered while another sound is actually audible, so pauses in speech let the music come back. Muted clips and clips at 0% volume do not trigger it.
- Q: Which controls does the user get? → A: Ducking on or off, Amount (how much the clip is lowered), and Smoothness (how fast it goes down and comes back).
- Q: Do two ducked clips duck each other? → A: No. A ducking clip is never a trigger, so two music tracks do not lower each other.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Keep music under speech (Priority: P1)

A user has music on the audio lane and a voice-over (or a talking video clip) at the same time. They select the music, open the Audio tool, and turn Ducking on. While the voice speaks the music is lowered; in the pauses it comes back up. They hear this at once in the preview and the exported video sounds the same.

**Why this priority**: This is the whole point of the feature and the most common use.

**Independent Test**: Put music and a voice-over with a 2 s pause in the middle, turn Ducking on for the music, play, and hear the music drop under the voice and rise in the pause; export and compare the music level in the voice and pause sections.

**Acceptance Scenarios**:

1. **Given** a music clip with Ducking on and another sound playing over part of it, **When** the project plays, **Then** the music is quieter by the chosen amount while the other sound is audible and at its normal level when it is not.
2. **Given** Ducking turned on, **When** the user changes Amount or Smoothness, **Then** the preview reflects it right away without a rebuild pause longer than a second.
3. **Given** Ducking is off (the default), **When** other sound plays, **Then** the clip is not changed.
4. **Given** the other sound is muted or at 0% volume, **When** the project plays, **Then** the music is not lowered.
5. **Given** the music is ducked, **When** the user exports, **Then** the exported music level in the voice section is lower than in the pause by the chosen amount, within a small tolerance.
6. **Given** one change of Ducking, Amount, or Smoothness, **When** it is made, **Then** it is one undo step.

---

### User Story 2 - See which clips are ducked (Priority: P2)

On the timeline a ducked audio clip shows a small badge, and the Audio panel tells the user how it is set. When nothing can trigger ducking (no other sound over the clip), the panel says so instead of leaving the user guessing why nothing changes.

**Why this priority**: Without a visible sign it is hard to know why a clip sounds quieter or why it does not.

**Independent Test**: Turn Ducking on for a clip with nothing over it and see the hint; add a voice-over and see the hint go away; check the badge.

**Acceptance Scenarios**:

1. **Given** a clip with Ducking on, **When** it is on the timeline, **Then** it shows a ducking badge that is readable by screen readers.
2. **Given** a ducked clip with no other sound over it, **When** its Audio panel is open, **Then** a short note says nothing is triggering ducking.
3. **Given** a video clip or a voice-over, **When** the user looks for the Ducking control, **Then** it is offered for audio-lane clips only.

---

### User Story 3 - Kept through edits, saved, and exported (Priority: P3)

Ducking stays right when the clips are moved, trimmed, split, duplicated, or have their speed changed, is saved with the project, and works together with the clip's volume, fades, noise reduction, equalizer, reverb, and voice effects. It works in the unfolded and tabletop layouts.

**Why this priority**: Persistence and the interplay with earlier audio tools are required but build on P1.

**Independent Test**: Duck a clip, split it, move the voice-over, change the music's volume and fade, save and reopen, unfold, export, and check the levels.

**Acceptance Scenarios**:

1. **Given** a ducked clip, **When** it is split, duplicated, trimmed, moved, or its speed changes, **Then** each resulting clip keeps its Ducking, Amount, and Smoothness, and ducking follows the sound that is now over it.
2. **Given** a trigger clip is moved, trimmed, deleted, muted, or its volume changes, **When** the project plays, **Then** the ducking follows at once, in the preview and in the export.
3. **Given** a ducked clip with a volume of 50% and a fade out, **When** it plays, **Then** the lowering is applied on top of the volume and the fade, never raising the level above what they set.
4. **Given** a project with ducking, **When** it is closed and reopened, **Then** the settings are unchanged.
5. **Given** a ducked clip, **When** reverb, noise reduction, equalizer, or voice effects are on, **Then** they still apply and ducking lowers the final result.

---

### Edge Cases

- A clip with Ducking on that is also the only sound in the project is never lowered.
- Two ducked clips never lower each other; each is lowered by the clips that are not ducked.
- Very short sounds (shorter than the smoothness time) lower the clip only as far as the smoothness allows, with no clicks or pops.
- A trigger that starts at the very beginning lowers the clip from the first moment, not after a delay, in preview and export.
- A ducked clip placed over silence plays at full level.
- Amount 0% leaves the clip unchanged; the maximum amount lowers it to a low but still audible background level, never complete silence.
- Missing media for a trigger clip means it does not trigger; the project still plays.
- Reduced preview quality may simplify effects but ducking still applies.
- Photo clips and clips without sound never trigger ducking.
- Undo and redo restore the exact earlier settings.
- Relink, remove-missing, project duplicate, and templates keep each clip's settings.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Each audio-lane clip MUST have Ducking settings: on or off (default off), an Amount, and a Smoothness.
- **FR-002**: While Ducking is on, the clip's level MUST be lowered by the Amount whenever another sound is audible, and MUST return to its normal level when that sound stops.
- **FR-003**: The sounds that trigger ducking MUST be the audible sound of video clips and of audio-lane clips whose Ducking is off; muted clips, clips at 0% volume, clips without sound, and the ducked clip itself MUST NOT trigger it.
- **FR-004**: Smoothness MUST set how quickly the level goes down and comes back; the change MUST be free of clicks.
- **FR-005**: The lowering MUST apply on top of the clip's own volume, fade, and effects and never raise the level above what those set.
- **FR-006**: Users MUST be able to turn Ducking on or off and set Amount and Smoothness from the clip's Audio panel; every change MUST be one undo step.
- **FR-007**: The preview MUST play the ducking live and react to Ducking, Amount, and Smoothness changes within one second.
- **FR-008**: Export MUST produce the same ducking as the preview, within a small level difference.
- **FR-009**: A ducked clip MUST show a badge on the timeline with an accessible description; the panel MUST show a note when nothing is triggering ducking.
- **FR-010**: Split, duplicate, trim, move, speed change, delete, relink, project duplicate, and templates MUST keep each clip's Ducking settings, and ducking MUST follow the changed sound arrangement.
- **FR-011**: Ducking settings MUST be saved with the project and survive close, reopen, and process death.
- **FR-012**: The Ducking control MUST be offered for audio-lane clips only.
- **FR-013**: All new controls MUST work in the portrait, unfolded, and tabletop layouts, meet touch-target and contrast rules, and have accessible labels.

### Key Entities *(include if feature involves data)*

- **Ducking**: A clip's setting: on or off, Amount (how much it is lowered), and Smoothness (how fast it moves).
- **Trigger sound**: Any audible sound over the same time from a video clip or from an audio-lane clip that is not ducking.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can turn ducking on for a music clip and hear it work in under 15 seconds from selecting the clip.
- **SC-002**: With the default settings, music under speech is lowered by the chosen amount (within 10%) when the speech starts, and is back within 10% of its normal level 2 s after the speech ends, in preview and export.
- **SC-003**: An exported project's music level in a voice section and in a pause differs by the chosen amount within 2 dB in an automated measurement.
- **SC-004**: No audible click: the level never changes by more than 3 dB within any 5 ms in the exported audio.
- **SC-005**: 100% of Ducking settings survive close and reopen, split, duplicate, trim, speed change, undo, and redo in automated tests.
- **SC-006**: Preview playback stays smooth with a ducked clip and two trigger clips: the playhead never stalls longer than 0.5 s.
- **SC-007**: Every ducking control is reachable in 3 taps or fewer from a selected audio clip.

## Assumptions

- Ducking is decided from the actual loudness of the trigger sounds, with one fixed sensitivity; the user does not set a threshold.
- Amount runs from 0% (no change) to 100% (a low background level, not silence), default 60%; Smoothness runs from quick to slow, default medium (a few hundred milliseconds).
- The video clips' own sound is a trigger but is never ducked by this feature.
- Fold, device, and designer reviews follow the earlier features and are done after the build, not before.
- Existing limits apply: undo depth, project length, and preview-quality rules stay as they are.
