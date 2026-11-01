# Feature Specification: Voice Split (Advanced Detach Audio)

**Feature Branch**: `027-voice-split`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "Advanced detach audio: separate the sound of a clip into a Voice track and a track with everything else (the instrument), so effects such as reverb can be applied to the instrument only. Voice versus everything else is wanted; one instrument is played at a time. On the device, no network."

## Scope note

A main-track clip that has its own sound gets a tool, Split voice, that makes two audio clips from it: Voice and Backing (everything that is not the voice, e.g. a guitar, piano, cello, violin, bass, flute, or harmonica). The two sit on the audio lane at the clip's place, the clip's own sound is turned off, and every audio tool (volume, reverb, equaliser, effects, ducking) then works on each separately. Not included: separating several instruments from each other, splitting audio-lane clips, clips that are reversed or on a speed curve, and drums or bass as their own parts.

## Clarifications

### Session 2026-11-01

- Q: Which model? → A: An on-device voice separation model (Open-Unmix UMX-HQ vocals, MIT licence, about 17 MB) bundled with the app; no download and no network.
- Q: How is Backing made? → A: The original sound minus the separated voice, so Voice and Backing together equal the original.
- Q: Where is the tool? → A: On the clip tools of a selected main-track clip with attached sound, next to Detach audio.
- Q: What if the clip is trimmed or sped up? → A: Only the trimmed range is separated; a constant speed is kept on both new clips; a reversed clip or a speed curve is refused with a message.
- Q: How long does it take, and can it be cancelled? → A: It shows a progress bar with Cancel; nothing changes when cancelled or when it fails.
- Q: Quality? → A: Not studio quality; some voice may remain in Backing and the voice may sound slightly thin. Said once in the panel.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Reverb on the instrument only (Priority: P1)

A user recorded themselves singing while playing guitar. They select the clip, tap Split voice, wait for the progress bar, and get two audio clips named Voice and Backing. They select Backing and add reverb; the voice stays dry.

**Why this priority**: This is the whole feature.

**Independent Test**: Split a clip that has a voice and an instrument, check that two audio clips appear at the clip's start with the clip's length, that the original clip's sound is off, that the sum of the two stems is the original within a small tolerance, and that the Voice stem has more energy where the voice is.

**Acceptance Scenarios**:

1. **Given** a main-track clip with sound, **When** the user selects it, **Then** a Split voice tool is offered next to Detach audio.
2. **Given** Split voice, **When** tapped, **Then** a progress bar with Cancel shows and the editor stays usable for viewing.
3. **Given** the separation finished, **When** it completes, **Then** two audio clips, Voice and Backing, are on the audio lane at the clip's place with its length, the clip's own sound is off, and this is one undo step.
4. **Given** the two clips, **When** the user adds reverb to one, **Then** only that clip has it, in the preview and the export.
5. **Given** Cancel or a failure, **When** it happens, **Then** nothing is added and the clip is unchanged, with a message on failure.
6. **Given** a clip without sound, a reversed clip, or a clip on a speed curve, **When** selected, **Then** the tool is unavailable with the reason.

---

### User Story 2 - Kept, saved, exported (Priority: P2)

The two clips are saved with the project, survive reopen and duplicate, and are exported like any audio clip. Splitting the same range twice does not redo the work.

**Acceptance Scenarios**:

1. **Given** a split, **When** the project is reopened or duplicated, **Then** Voice and Backing play as before.
2. **Given** an export, **When** compared with the preview, **Then** they match within a small difference.
3. **Given** the same clip range split again (or after undo and redo), **When** requested, **Then** the earlier result is reused and no new processing runs.

## Edge Cases

- Mono recordings are separated as stereo with the same signal in both channels; sources at 48 kHz or other rates are converted.
- Very long clips take proportionally longer; the progress bar shows the share done.
- Silence or music without a voice gives a quiet Voice clip and a Backing clip like the original.
- Low storage or a failed read stops with a message.

## Requirements *(mandatory)*

- **FR-001**: A main-track clip with attached sound, constant speed, not reversed, MUST offer Split voice; otherwise it MUST say why not.
- **FR-002**: Split voice MUST separate the clip's trimmed range into Voice and Backing on the device with no network, where Backing is the original minus Voice.
- **FR-003**: It MUST show progress and allow Cancel; cancel and failure MUST leave the project unchanged.
- **FR-004**: On success it MUST add two audio clips (Voice, Backing) at the clip's start with its length and speed, turn off the clip's own sound, and be one undo step.
- **FR-005**: The two clips MUST be ordinary audio clips: all audio tools, ducking, and export work on each.
- **FR-006**: The results MUST be stored in the app's storage, saved with the project, reused when the same range is split again, and removed when no project uses them.
- **FR-007**: The panel MUST state once that the split is approximate.
- **FR-008**: The tool MUST work in all layouts, meet touch-target and contrast rules, and have accessible labels.

### Key Entities *(include if feature involves data)*

- **Split result**: Two audio files (Voice, Backing) for one source range, keyed by the source's fingerprint and range.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can split a clip in 2 taps after selecting it (Split voice, and wait).
- **SC-002**: Voice plus Backing equals the original within a small tolerance (error below 1% of full scale) in an automated test.
- **SC-003**: On a flagship phone a one-minute clip is separated in under about 60 seconds; the progress bar updates at least every 2 seconds.
- **SC-004**: A repeated split of the same range returns in under 1 second.
- **SC-005**: 100% of results survive close and reopen, undo, redo, and project duplicate in automated tests.

## Assumptions

- The first model is Open-Unmix UMX-HQ (chosen over Demucs for its 17 MB size, confirmed MIT licence, and low memory use). A heavier Demucs model (136 MB or more, about 1 GB of memory) is a possible later "High quality" option behind the same interface, only if real recordings show too much leakage.
- Quality is judged by the user on real recordings; the model is behind an interface so a better one can replace it.
- The model works at 44.1 kHz stereo with a 4096-point transform and hop 1024, and only the vocals graph is bundled.
- The results are stored as 16-bit stereo WAV files at 44.1 kHz.
- Fold, device, and designer reviews follow the earlier features and are done after the build.
