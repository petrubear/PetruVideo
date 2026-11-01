# Feature Specification: Auto Captions

**Feature Branch**: `019-auto-captions`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "Auto captions: from the Text tab the user taps Auto captions to turn the speech in the project into subtitles. The phone listens only on the device (no network): the sound of the main-track clips that have sound is split into spoken phrases at pauses, each phrase is recognised by the phone's own on-device speech recognition in the project's language, and every phrase becomes an ordinary text clip placed at the time it is spoken, in a caption style (bottom of the frame, light text on a dark box). A progress indicator with Cancel shows while it works; nothing is added when it is cancelled or fails. The user can pick the language, choose whether captions replace earlier auto captions, and edits the results like any text clip (words, style, timing, delete). When on-device recognition is not available (old Android version, language pack missing) the tool says why and how to fix it. Captions are saved with the project, follow undo as one step, and are exported with the video."

## Scope note

The speech in the video clips of the main track becomes subtitles. The sound is cut into spoken phrases at pauses; each phrase is turned into text by the phone's own speech recognition and shown as a text clip for the time it is spoken. Nothing leaves the phone: if the phone cannot recognise speech on its own, the tool says so instead of using the internet. Not included: captions for audio-lane clips or voice-overs, word-by-word highlighting, translation, speaker names, and recognition with the network.

## Clarifications

### Session 2026-11-01

- Q: Which sound is used? → A: The attached sound of main-track video clips that are not muted and have a volume above 0%. Audio-lane clips are not used.
- Q: How is timing decided? → A: A phrase starts and ends where the sound starts and ends around a pause; the caption is shown for that time (mapped through the clip's trim, speed, and position). Words inside a phrase are not timed separately.
- Q: What happens to earlier auto captions? → A: The user chooses Replace (default) or Keep. Replace removes the earlier captions made by this tool, and only those, in the same undo step; text the user typed or edited as a title is never removed. A caption the user edits stays an auto caption for Replace purposes unless they delete it.
- Q: Which languages? → A: The phone's on-device recognition languages that are installed; the default is the phone's language and the user can pick another installed one.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Generate captions (Priority: P1)

A user opens the Text tab and taps Auto captions. A panel shows the language, a Replace earlier auto captions switch, and a Start button. While it works a progress bar with Cancel shows. When done, captions appear on the text lane, each at the time its phrase is spoken, at the bottom of the frame with light text on a dark box. All captions arrive in one undo step.

**Why this priority**: This is the whole feature.

**Independent Test**: Put a clip with clear speech in the project, tap Auto captions, Start, see the captions line up with the speech in the preview and the export, then undo once and see them gone.

**Acceptance Scenarios**:

1. **Given** a project with speech in a main-track clip, **When** the user taps Start, **Then** progress shows, and at the end one text clip per recognised phrase appears at the time it is spoken, in the caption style.
2. **Given** progress is showing, **When** the user taps Cancel or leaves the project, **Then** nothing is added and no partial result stays.
3. **Given** a finished run, **When** the user undoes, **Then** all the new captions disappear at once, and redo brings them back.
4. **Given** a phrase the phone cannot recognise, **When** the run ends, **Then** that phrase gets no caption and a short note says how many phrases were skipped.
5. **Given** a project with no speech or no clip with sound, **When** the user taps Start, **Then** a short message says there was nothing to caption and nothing is added.
6. **Given** the Replace switch is on and earlier auto captions exist, **When** a run finishes, **Then** the earlier captions are removed in the same undo step and typed titles are untouched; with it off, the new captions are added beside them.

---

### User Story 2 - Know when it cannot work (Priority: P2)

On a phone where on-device recognition is not available, Auto captions explains why (the Android version is too old, or the language pack for the chosen language is not installed) and, where possible, how to fix it (a button to open the phone's speech settings). The tool never falls back to the network.

**Why this priority**: Trust: the user must know nothing is uploaded and what is missing.

**Independent Test**: Open Auto captions on a phone without a language pack and read the message and the settings button.

**Acceptance Scenarios**:

1. **Given** the phone has no on-device recognition, **When** the panel opens, **Then** it says that captions need Android 13 or newer or an installed speech language, and Start is unavailable.
2. **Given** the chosen language is not installed, **When** the panel opens, **Then** it names the language, offers to open the speech settings, and lists the installed languages that can be picked instead.
3. **Given** any case, **When** a run starts, **Then** the app does not use the network for recognition.

---

### User Story 3 - Edit and export (Priority: P3)

Captions are ordinary text clips: the user can change their words, style, size, place, timing, animation, and delete them. They are saved with the project, kept by project duplicate, and exported with the video exactly as titles are. The tools work in the unfolded and tabletop layouts.

**Why this priority**: Recognition is never perfect, so editing is part of the flow.

**Independent Test**: Generate captions, correct one, change the style of another, move one, save and reopen, export, and compare frames.

**Acceptance Scenarios**:

1. **Given** captions, **When** the user selects one, **Then** the text tools open as for any text clip.
2. **Given** captions, **When** the project is closed and reopened, **Then** they are unchanged and are still recognised as auto captions for Replace.
3. **Given** captions, **When** the project is exported, **Then** each shows in the video in its time and place.
4. **Given** the main clips are edited after captioning (trimmed, split, moved), **When** the user wants new captions, **Then** Replace makes a fresh set that matches.

---

### Edge Cases

- Very short sounds (under 0.3 s) are not treated as phrases.
- A phrase longer than 8 seconds is cut at the quietest point so that captions stay readable; a caption is at most two lines of the frame width.
- Overlapping phrases from two clips with sound at the same time (a transition) are kept apart by the clip order.
- Speed changes and trims are respected: a caption sits where the speech is heard on the timeline.
- Reversed clips and clips with a speed curve are skipped with a note (the sound is not forward speech).
- Muted clips and clips at 0% volume are skipped.
- Missing media is skipped; the rest is captioned.
- A very long project is limited to 200 captions per run with a message.
- Low storage: a short temporary file per phrase is used and removed; if there is no room, the run stops with a message.
- Reduced preview quality does not affect captions.
- Undo and redo restore exactly the earlier state; Replace and the new captions are one step.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The Text tab MUST offer Auto captions, opening a panel with the language, a Replace switch (on by default), and Start.
- **FR-002**: A run MUST use the attached sound of main-track video clips that are not muted and have a volume above 0%, split into phrases at pauses, and place each caption at the timeline time where its phrase is heard.
- **FR-003**: Recognition MUST be done only by the phone's on-device speech recognition; the app MUST NOT use the network for it, and MUST NOT start a run when on-device recognition for the chosen language is missing.
- **FR-004**: Each recognised phrase MUST become a text clip in the caption style (bottom of the frame, light text, dark box), marked as an auto caption.
- **FR-005**: A run MUST show progress and be cancelable; a cancel, failure, or leaving the project MUST leave the project and storage unchanged.
- **FR-006**: The result MUST be added in one undo step; with Replace on, the earlier auto captions MUST be removed in the same step and no other text clip MUST be removed.
- **FR-007**: Phrases that cannot be recognised MUST be skipped with a count; a run with nothing to caption MUST say so.
- **FR-008**: When recognition is not available the panel MUST say why and how to fix it, and Start MUST be unavailable; users MUST be able to open the phone's speech settings from it.
- **FR-009**: Auto captions MUST be editable and deletable like any text clip, saved with the project, and exported with the video.
- **FR-010**: A run MUST be limited to 200 captions with a message when the limit is hit; clips that are reversed, have a speed curve, are muted, or are missing MUST be skipped with a note.
- **FR-011**: All new controls MUST work in the portrait, unfolded, and tabletop layouts, meet touch-target and contrast rules, and have accessible labels, including the progress indicator.

### Key Entities *(include if feature involves data)*

- **Auto caption**: A text clip made by this tool (marked so Replace can find it), with the words of one spoken phrase and the time it is spoken.
- **Phrase**: A stretch of speech between pauses in the attached sound of a main-track clip.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can start auto captions in 3 taps or fewer from the Text tab.
- **SC-002**: For a phrase recognised on the phone, the caption starts and ends within 300 ms of the speech in an automated check with known audio.
- **SC-003**: Cancelling leaves no files behind and no change to the project in 100% of tested cases.
- **SC-004**: With recognition missing, 100% of attempts show the reason and none starts a run or uses the network.
- **SC-005**: 100% of auto captions survive close and reopen, undo, redo, and project duplicate in automated tests.
- **SC-006**: Replace removes only auto captions in 100% of tested projects that also hold typed titles.

## Assumptions

- Speech recognition runs on the phone through Android's own on-device recognizer, available from Android 13; on older versions the tool is unavailable and says so.
- The quality of the words is the phone's: the app does not claim accuracy and the user is expected to correct them.
- Phrases are found from the sound's loudness (pauses), not from the recognizer, so timing does not depend on the recognizer reporting times.
- Fold, device, and designer reviews follow the earlier features and are done after the build, not before; real speech recognition is checked on the phone then.
- Existing limits apply: undo depth, project length, and preview-quality rules stay as they are.
