# Feature Specification: Voice-over Recording

**Feature Branch**: `013-voice-over`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "Voice-over recording: in the Audio tab the user taps Record voice-over, speaks into the microphone while the project plays from the playhead (so they can talk over the video), and stops. The recording becomes a new audio clip at the point where recording started, with a live level meter and a count-up timer while recording, a short count-in of three seconds, microphone permission asked just in time, and the recording kept if the app is interrupted. The new clip behaves like any audio clip (move, trim, split, volume, fade, noise reduction, equalizer, voice effects), is saved with the project, plays in the preview, and is exported with the video."

## Scope note

The Audio tab gains a Record voice-over button. Recording uses the phone's microphone and the project plays during it, so the user can talk over the picture. The result is an ordinary audio clip. Not included: recording from external or Bluetooth microphones by choice (the phone's current input is used), echo cancellation of the project's sound (headphones are recommended), recording video, and live effects while recording.

## Clarifications

### Session 2026-11-01

- Q: Does the project's own sound play while recording? → A: Yes, so the user can time their words; the screen suggests headphones so the recording stays clean.
- Q: What if the recording would run into an existing audio clip? → A: Audio clips cannot overlap, so the recording stops when it reaches the next audio clip's start, with a short message, and what was recorded is kept.
- Q: How long can a recording be? → A: Up to 30 minutes, or until the phone runs low on storage; either way what was recorded is kept.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Record a voice-over over the video (Priority: P1)

A user opens the Audio tab, moves the playhead to where they want to start, and taps Record voice-over. The first time, the phone asks for microphone permission. A count-in of 3, 2, 1 shows, then the project plays from the playhead while the microphone records. A level meter moves with their voice and a timer counts up. They tap Stop; playback stops, and a new audio clip appears in the audio lane at the point where recording started, selected, with the words they spoke.

**Why this priority**: This is the whole feature.

**Independent Test**: Record 5 s over a video from the 2 s mark, stop, play the project and hear the voice at the right moments, undo and see the clip gone, export and hear it.

**Acceptance Scenarios**:

1. **Given** the Audio tab with the microphone allowed, **When** the user taps Record voice-over, **Then** a three second count-in shows and recording and playback start together from the playhead when it ends.
2. **Given** a recording in progress, **When** the user speaks, **Then** the level meter moves with the voice and a timer counts up in seconds.
3. **Given** a recording in progress, **When** the user taps Stop, **Then** recording and playback stop and a new audio clip starts where the recording started, with the length recorded, selected in the audio lane.
4. **Given** the count-in, **When** the user taps Cancel, **Then** nothing is recorded and nothing is added.
5. **Given** a finished voice-over, **When** the user taps Undo, **Then** the clip is removed in one step, and Redo brings it back.
6. **Given** a recording that passes the end of the project, **When** it goes on, **Then** the project length follows it, as for any audio clip.

---

### User Story 2 - Permission and interruptions (Priority: P2)

The microphone permission is asked only when the user first taps Record, with a short reason. If it is refused, a message explains and nothing happens; if it is blocked, the message points to the phone's settings. If a call, another app, or the user leaving the editor interrupts a recording, what was recorded so far is kept as a clip.

**Why this priority**: A recorder that loses a take or asks for permission at the wrong time feels broken, but a basic recording already delivers the value.

**Independent Test**: Deny permission and see the message; start a recording and put the app in the background, return and find the clip.

**Acceptance Scenarios**:

1. **Given** no microphone permission, **When** the user taps Record voice-over, **Then** the permission request appears with its reason; on allow the count-in starts, on deny a message says a voice-over needs the microphone.
2. **Given** the permission was blocked permanently, **When** the user taps Record voice-over, **Then** a message offers to open the phone's settings.
3. **Given** a recording in progress, **When** the app goes to the background, a call arrives, or the phone locks, **Then** the recording and playback stop and the recording so far becomes a clip.
4. **Given** the process was ended during a recording, **When** the user reopens the project, **Then** the recorded part is recovered as a clip, or a short message says it could not be.
5. **Given** the phone runs low on storage during a recording, **When** it reaches the limit, **Then** the recording stops, what was recorded is kept, and a message says why.

---

### User Story 3 - The clip is a normal audio clip (Priority: P3)

The voice-over can be moved, trimmed, split, duplicated, deleted, and given volume, fade, noise reduction, equalizer, and voice effects like any audio clip. It is saved with the project, plays in the preview, and is in the export. It also shows a waveform in the lane, in the unfolded and tabletop layouts too.

**Why this priority**: These behaviors already exist for audio clips, so it mostly confirms the recording joins them.

**Independent Test**: Record, then trim, fade out, and apply a voice effect; close and reopen; export and hear the same.

**Acceptance Scenarios**:

1. **Given** a recorded clip, **When** the user uses any audio clip tool, **Then** it behaves as for an added audio file.
2. **Given** a project with a voice-over, **When** it is closed and reopened, **Then** the clip is unchanged and plays.
3. **Given** an export, **When** it finishes, **Then** the voice-over is in the video's sound at the same time and level as in the preview.
4. **Given** the project is duplicated, **When** the copy is opened, **Then** the voice-over plays in the copy too.

---

### Edge Cases

- A recording shorter than 0.5 s is discarded with a short message ("Too short").
- A recording that would run into the next audio clip stops there, with a message; what was recorded is kept.
- With no video clips in the project, recording still works and the project length follows the recording.
- A second tap on the record button while recording is the same as Stop.
- Rotating, folding, unfolding, or resizing during a count-in or a recording does not stop it or lose the take.
- Another app holding the microphone gives a message and records nothing.
- The recorded file lives inside the app's own storage and is removed with the project when the project is deleted.
- Missing-file handling (relink, remove) treats a voice-over like an added audio file; a deleted recording file shows as missing.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The Audio tab MUST offer Record voice-over.
- **FR-002**: The app MUST ask for microphone permission only when the user first taps Record, with a short reason, and MUST explain a refusal and point to settings when it is blocked.
- **FR-003**: Starting a recording MUST show a three second count-in that can be cancelled, then start recording and project playback together from the playhead.
- **FR-004**: While recording the screen MUST show a live level meter and a timer counting up, and a Stop control.
- **FR-005**: Stopping MUST add one audio clip at the time the recording started, with the recorded length, selected, as one undo step.
- **FR-005a**: While the count-in or a recording is running, editing MUST be locked: the timeline, clip tools, and preview gestures do nothing, and only Stop, Cancel, and Back work.
- **FR-006**: A recording MUST stop when it reaches the next audio clip, passes 30 minutes, or usable storage drops below 20 MB, and MUST keep what was recorded with a message.
- **FR-006a**: If the microphone cannot be opened (for example another app is using it), the app MUST say so and add nothing.
- **FR-007**: An interruption (background, call, lock) MUST stop the recording and keep it as a clip; a recording lost to process death MUST be recovered when the project reopens when possible.
- **FR-008**: A recording shorter than 0.5 s MUST be discarded with a message.
- **FR-009**: The clip MUST support every audio clip tool, appear with a waveform, be saved with the project, play in the preview, and be rendered by export at the same time and level.
- **FR-010**: The recorded file MUST stay in the app's own storage, never be shared without the user's action, and be deleted with its project.
- **FR-011**: Recording MUST continue through rotation, fold changes, and resizing without losing the take.
- **FR-012**: All new controls MUST work in the portrait, unfolded, and tabletop layouts, meet touch-target and contrast rules, announce recording state to accessibility services, and have accessible labels.

### Key Entities *(include if feature involves data)*

- **Voice-over recording**: An audio file made from the microphone and kept in the app's storage, with its length and the project time where it started.
- **Voice-over clip**: An audio-lane clip made from a recording; shares every setting of an added audio clip.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can record and add a voice-over in 4 taps or fewer from the Audio tab (Record, allow once, Stop, nothing else) after the first permission.
- **SC-002**: The voice is heard in the preview and the export within 100 ms of where it was spoken against the picture.
- **SC-003**: The level meter reacts to speech within 150 ms.
- **SC-004**: A recording interrupted by a call or by leaving the app keeps at least the part recorded up to 1 s before the interruption in 100% of automated tests.
- **SC-005**: 100% of voice-overs survive close and reopen, split, trim, duplicate, undo, and redo in automated tests.
- **SC-006**: Recording does not make the preview skip frames or audio: playback during recording stays within the normal responsiveness budget.

## Assumptions

- The phone's default microphone input is used; the user chooses headphones, which the screen suggests.
- Recordings are made in a standard compressed audio format that the editor and export already read.
- Audio clips keep their rule that they cannot overlap; recording respects it by stopping at the next clip.
- Fold, device, and designer reviews follow the earlier features and are done after the build, not before.
- The emulator can feed a recording with a virtual microphone, so the pipeline is tested there; real microphone quality is a device check.
