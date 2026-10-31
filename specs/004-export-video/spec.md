# Feature Specification: Export Video

**Feature Branch**: `004-export-video`

**Created**: 2026-10-31

**Status**: Draft

**Input**: User description: "Export the project to a video file: choose resolution (720p, 1080p, 2K, 4K), frame rate (24, 30, 50, 60), quality (Smaller, Recommended, Higher); show format, bitrate and estimated size; render in the background with progress and cancel; save to the gallery; respects clip speed, volume, mute and reverb."

## Clarifications

### Session 2026-10-31

- Q: Can users export at a higher resolution than the project? → A: No; options above the cap are dimmed.
- Q: What is the resolution cap, given projects are capped at 1080p by 001? → A: The largest source resolution used in the project (photos count), not the project's own format.
- Q: Can users export at a higher frame rate than the footage? → A: No; capped at the highest source frame rate in the project (photos count as the project's rate).
- Q: Does the export get a watermark or end card? → A: No; the file contains exactly the project.
- Q: What happens if the app is killed mid-render? → A: Next time the app opens it says the export did not finish and offers Retry from the start; no automatic resume.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Export a project with recommended settings (Priority: P1)

A user finishes editing, taps Export, sees a preview of the project with its length and the recommended settings already chosen, taps "Save to gallery", watches the progress, and ends with a video in their phone's gallery that looks and sounds like the preview.

**Why this priority**: Without export the editor produces nothing the user can share. One tap with sensible defaults is the whole value.

**Independent Test**: Open a project with two clips (one at 2x, one with reverb), tap Export, tap Save to gallery, wait for completion, open the video from the gallery, and confirm length, speed, volume, and reverb match the preview.

**Acceptance Scenarios**:

1. **Given** an open project with at least one clip, **When** the user taps Export, **Then** an export screen opens showing a preview with the project length, the settings (defaulting to the project's own resolution and frame rate, and Recommended quality), the file format, the bitrate, and the estimated size.
2. **Given** the export screen, **When** the user taps Save to gallery, **Then** rendering starts, a progress bar with a percentage shows, and Cancel is available.
3. **Given** a render reaches 100%, **When** it finishes, **Then** the video is in the device gallery, the screen says it was saved, and the user can open or share it.
4. **Given** a project with an empty timeline, **When** the user looks at Export, **Then** it is unavailable and says why.
5. **Given** an export finished, **When** the user plays the file, **Then** its length equals the project length within one frame.

---

### User Story 2 - Choose resolution, frame rate, and quality (Priority: P1)

A user picks the resolution (720p, 1080p, 2K, 4K), the frame rate (24, 30, 50, 60), and the quality (Smaller, Recommended, Higher). The bitrate and estimated size update as they choose, so they can trade file size for quality before rendering.

**Why this priority**: Different destinations need different files (a chat message versus a high-quality upload), and the user asked for these controls explicitly.

**Independent Test**: Change resolution from 1080p to 720p and see the estimated size drop; pick Higher and see the bitrate rise; pick 4K on a device that cannot encode it and see the choice explained or unavailable.

**Acceptance Scenarios**:

1. **Given** the export screen, **When** the user selects a resolution, a frame rate, or a quality, **Then** the bitrate and estimated size update immediately (within 100 ms) and the selection is shown clearly.
2. **Given** a resolution or frame rate the device cannot encode, or above the project's cap (see FR-005a), **When** the options show, **Then** that option is dimmed with a short reason and cannot be started.
3. **Given** a project whose own format is portrait or square, **When** the resolution is chosen, **Then** the resolution refers to the short side (for example 1080p means 1080 pixels on the short side) and the aspect ratio is kept.
5. **Given** the user changed settings, **When** they leave and reopen Export for the same project, **Then** their last choices are remembered for that project.

---

### User Story 3 - Render in the background, cancel, and recover (Priority: P2)

A user starts an export and leaves the app or locks the phone. The render continues, a notification shows progress, and returning to the app shows the same state. They can cancel at any time, and a cancelled or failed export leaves nothing half-written in the gallery.

**Why this priority**: Rendering takes minutes for long or 4K projects, and users will not stay on the screen. It builds on a working export.

**Independent Test**: Start a 4K export, press Home, see the notification advance, return and see the progress, cancel, and confirm no partial file is in the gallery.

**Acceptance Scenarios**:

1. **Given** a render in progress, **When** the user leaves the app, **Then** rendering continues and a notification shows progress with a Cancel action.
2. **Given** a render in progress, **When** the user taps Cancel (in the app or the notification), **Then** the render stops within 2 seconds, temporary files are removed, nothing is added to the gallery, and the settings remain for a retry.
3. **Given** a render finishes while the app is closed, **When** the user taps the notification, **Then** the app opens on the finished export with the options to open or share the video.
4. **Given** the render cannot continue (storage full, encoder failure, source file missing), **When** it stops, **Then** the user sees what happened in plain words, partial output is removed, and they can retry.
5. **Given** the user edits the project while a render is running, **When** the edit is made, **Then** the running export still renders the project as it was when started.

---

### User Story 4 - Export layouts on the Fold (Priority: P3)

The export screen follows the existing layouts: a single column on the phone, preview beside the settings when unfolded, and in tabletop the preview above the hinge and settings below.

**Why this priority**: The product is foldable-first, but this builds on the working phone flow.

**Independent Test**: Open Export on the cover screen, unfold, and half-fold; confirm the layout changes and a running render and chosen settings are unaffected.

**Acceptance Scenarios**:

1. **Given** the export screen on the unfolded device, **When** it shows, **Then** the preview and the settings are side by side and nothing interactive sits under the hinge.
2. **Given** tabletop posture, **When** the screen shows, **Then** the preview is above the hinge and the settings and progress below.
3. **Given** a render in progress, **When** the device is folded, unfolded, or rotated, **Then** the render and its progress continue unchanged.

---

### Edge Cases

- The project has missing media: Export is blocked with a message naming the missing file and a way to relink or remove it first.
- The device does not have enough free storage for the estimated size plus a safety margin: Export warns before starting and offers smaller settings.
- The device overheats or memory runs low during the render: the render slows or pauses and says so rather than failing silently, and resumes or fails clearly.
- The app is killed or the phone restarts mid-render: the next time the app opens it shows that the export did not finish, with a Retry, and no partial file remains in the gallery.
- Two exports at once: a second export waits until the first finishes or is cancelled.
- Very long projects (over one hour): the estimate and the warning about time are shown before starting.
- A project of photos only: exports as a silent video with the photos' durations.
- Audio only on the audio lane (no video clips): exports with black video for the project length.
- Export always uses the project's own aspect ratio.
- Gallery access is not allowed on this version of the system: the file is saved through the system's own save flow without asking for broad access to the user's media.
- The project contains only 1080p footage: 2K and 4K are dimmed with the reason "above your footage"; a project with one 4K clip among 1080p clips allows 4K (the 1080p clips are upscaled in that file).
- HDR or high-bit-depth source clips: they are exported as standard-range video and the user is told on the screen before saving.
- Clips with reverb, speed changes, mute, or volume above 100%: the sound matches the preview, with peaks kept below clipping.

## Requirements *(mandatory)*

### Functional Requirements

**Export screen and settings**

- **FR-001**: Users MUST be able to open Export from the editor's Export button on every layout; it MUST be unavailable, with a reason, when the project has no clips.
- **FR-002**: The export screen MUST show a preview of the project with its length, the Resolution options 720p, 1080p, 2K, and 4K, the Frame rate options 24, 30, 50, and 60, and the Quality options Smaller, Recommended, and Higher.
- **FR-003**: Defaults MUST be the largest of 720p, 1080p, 2K, and 4K that does not exceed 1080p or the cap in FR-005a, the nearest option to the project's frame rate that does not exceed its cap, and Recommended quality.
- **FR-004**: The screen MUST show the file format, the bitrate, and the estimated file size for the current choices and update them within 100 ms of any change; the estimate MUST be within 15% of the real file size.
- **FR-005**: Resolutions and frame rates the device cannot encode MUST be dimmed with a short reason and MUST NOT be startable.
- **FR-005a**: The highest selectable resolution MUST be the largest source resolution used by any clip in the project (photos count at their pixel size), and the highest selectable frame rate MUST be the highest source frame rate used (photos count at the project's rate); options above these MUST be dimmed with a short reason. Clips smaller than the chosen output are scaled to fit without stretching.
- **FR-006**: The resolution MUST apply to the short side of the frame and keep the project's aspect ratio.
- **FR-007**: The last-used choices MUST be remembered per project.

**Rendering**

- **FR-008**: Tapping Save to gallery MUST render the project to a video file with no watermark, end card, or other content that is not in the project, matching the preview: clip order, trims, speed, volume, mute, reverb, photos with their durations, gaps as black or silence, and the project's frame.
- **FR-009**: Rendering MUST continue when the app is in the background or the screen is off, show a notification with progress and a Cancel action, and show progress and Cancel on the export screen.
- **FR-010**: Cancel MUST stop the render within 2 seconds, remove temporary files, and add nothing to the gallery.
- **FR-011**: The render MUST use a snapshot of the project taken when it starts; later edits MUST NOT change a running export.
- **FR-012**: The output length MUST equal the project length within one frame, and audio and video MUST stay in step within 40 ms.
- **FR-013**: Sound MUST match the preview, including reverb, clip volume up to 200%, and mute, and MUST NOT clip harshly.
- **FR-014**: Source media files and the project MUST NOT be modified by exporting.

**Saving and results**

- **FR-015**: A finished export MUST appear in the device gallery (in a "PetruVideo" album or folder) and be offered to open and to share.
- **FR-016**: A cancelled, failed, or interrupted export MUST NOT leave a partial or broken file in the gallery.
- **FR-017**: Saving MUST work without asking for broad access to the user's photos and videos.

**Problems and limits**

- **FR-018**: Export MUST be blocked, with a plain message and a fix, when media is missing or when free storage is not enough for the render and its saved copy (about twice the estimated size) plus a safety margin.
- **FR-019**: When a render stops for any reason the user MUST see what happened in plain words and be able to retry with the same settings; an app that was killed during a render MUST say, the next time it opens, that the export did not finish and offer Retry from the start (no automatic resume).
- **FR-020**: Only one export runs at a time; a second one waits or is refused with a message.
- **FR-021**: Under heavy memory or thermal pressure the render MUST lower its priority first; if it still cannot continue it MUST stop with a plain message ("The phone is too hot or low on memory") and offer Retry, never failing silently or leaving a partial file.

**Design and layout**

- **FR-022**: The export screen MUST follow the designer's Export artboard (preview with duration, segmented controls for resolution, frame rate, and quality, the format, bitrate, and estimated size rows, the progress card with Cancel, and the Save to gallery button) in the existing visual language.
- **FR-023**: It MUST work on compact, medium, expanded, and tabletop arrangements, keep the render and settings through fold, unfold, and rotation, and keep nothing interactive under the hinge.
- **FR-024**: Controls MUST have accessible labels, touch targets of at least 48 dp, scalable text, and values readable by a screen reader, including the progress.

### Key Entities

- **Export settings**: resolution, frame rate, and quality chosen for a project; remembered per project.
- **Export job**: one render of a snapshot of a project with its settings, its state (waiting, rendering with progress, finished, cancelled, failed), and its result file.
- **Quality level**: Smaller, Recommended, or Higher; maps to a bitrate for each resolution and frame rate.
- **Exported video**: the file in the gallery, with its length, resolution, frame rate, and size.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can export a project with the default settings in two taps (Export, Save to gallery) and find the video in the gallery.
- **SC-002**: A 1080p, 30 fps, one-minute project exports in no more than twice its length on the Samsung Fold and in no more than four times its length on a mid-range phone.
- **SC-003**: The estimated size is within 15% of the real size in 95% of test exports.
- **SC-004**: The exported file's length is within one frame of the project length, audio and video are in step within 40 ms, and speed, volume, mute, and reverb match the preview in 100% of test projects.
- **SC-005**: Cancelling stops the render within 2 seconds in 100% of tests and leaves no partial file in the gallery.
- **SC-006**: A render keeps running with the app in the background or the screen off for a one-minute 4K project on a device that supports 4K, and the notification progress is correct within 5%.
- **SC-007**: On each affected screen and arrangement (compact, expanded, tabletop), a design review finds no deviation from the Export artboard other than documented ones, and nothing sits under the hinge.
- **SC-008**: Source media files and the project are identical before and after exporting in 100% of tests.

## Assumptions

- The designer's Export artboard (canvas version 14) is the visual reference. Buttons below Save to gallery are cut off in the reference image; Share and one more action are assumed and confirmed with the designer.
- The output is an MP4 file with H.264 video and AAC audio, as the artboard states; other formats and codecs are out of scope.
- Quality levels map to bitrates (for example about 12 Mbit/s at 1080p 30 fps for Recommended, as in the artboard); exact tables are tuned during implementation.
- Resolutions use the short side; the project's aspect ratio is kept; the highest option is the largest source resolution, not the project's 1080p format.
- A frame rate below the footage drops frames; options above the highest source frame rate are not offered.
- Sharing uses the system share sheet; posting to specific social apps is out of scope.
- The Exports list in the projects navigation (history of finished exports) is a later feature; this feature only shows the result of the export just made.
- Export of effects that do not exist yet (video effects, overlays, text, filters) belongs to those features; the export pipeline is built so they can be added.
- Hardware encoder capabilities decide which options are available per device, and the bitrate is lowered to what the encoder accepts (the Bitrate row shows the real value).
- On Android 8 and 9 saving to the gallery needs the legacy storage write permission, which is asked at the first export; on Android 10 and up no storage permission is needed.
- Features 001 to 003 are in place: the timeline, speed, volume, mute, and reverb values are stored and are what the render uses.
