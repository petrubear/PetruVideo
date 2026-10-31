# Research: Project Creation, Media Import, and Basic Timeline with Trim

All Technical Context unknowns are resolved here. Library versions are deliberately not pinned in this document; they are chosen and recorded in the version catalog at scaffolding. Decisions marked "validate" need a short spike before the dependent task starts.

## 1. Preview playback engine

- **Decision**: Put preview behind a `PreviewEngine` interface in `core:media`. First implementation uses Media3 ExoPlayer with a playlist of clipped media items for the video lane. For detached/added audio on its own lane, use Media3's composition-based playback if it proves stable in the spike; otherwise drive a second, synchronized audio player.
- **Rationale**: One interface keeps the editor independent of the playback choice, so the experimental pieces can be swapped without touching UI or engine code. ExoPlayer is the standard, hardware-accelerated path.
- **Alternatives considered**: Custom MediaCodec + OpenGL pipeline (more control, far more work, not needed for this slice); FFmpeg-based preview (bundle size, licensing, slower).
- **Validate**: Spike 1 - play a two-lane timeline (3 clips + 1 detached audio) with scrub and clip-boundary continuity on a mid-range device; confirm the 30 fps budget and A/V sync within 40 ms.

## 2. Timeline state, edits, and undo/redo

- **Decision**: Immutable `Timeline` value; every edit is a pure function `Timeline -> Timeline` wrapped in a `Command` with a label. History stores snapshots (structural sharing keeps them small), capped at 100 entries, held in memory per open project and not persisted across sessions.
- **Rationale**: Snapshot history makes undo/redo trivially correct for compound edits (detach audio, split with attached audio) and makes property-based testing easy.
- **Alternatives considered**: Inverse-command undo (more code, easy to get wrong for compound edits); persisted history (extra schema, not required by the spec).

## 3. Video lane and audio lane model

- **Decision**: Video lane is a gapless ordered list (position derived from order). Audio lane is a list of positioned clips (explicit start, gaps allowed). Sound attached to a video clip is a flag on that clip, not an audio clip; "Detach audio" creates an audio clip with the same source range and start and clears the flag.
- **Rationale**: Matches the clarified behavior exactly: attached sound moves with its clip automatically; detached audio is independent. Avoids a link/unlink concept.
- **Alternatives considered**: Always-separate audio clips with a link group (rejected in clarification); fully free-form positioned video lane (not needed; the design shows a sequential main track).

## 4. Project format from first clip

- **Decision**: On the first video or photo added, derive format: aspect ratio from display dimensions after rotation, resolution scaled down to fit 1080 on the short side or the nearest standard size, frame rate rounded to the nearest of 24/30/60 (default 30 when unknown). Store on `Project`; never recalculated. Photos-only first clip yields 1080p 30 fps with the photo's aspect ratio.
- **Rationale**: Implements clarification 1 with a deterministic, testable rule.
- **Alternatives considered**: Fixed 1080p 16:9 (distorts portrait footage); format picker (not in the design).

## 5. Persistence and autosave

- **Decision**: Room with tables for projects, media items, video clips, and audio clips; the timeline is rebuilt from clip rows ordered by index. Autosave writes the changed project in a single transaction 1.5 seconds after the last edit, and immediately on `onStop`/process-death signals. Thumbnails and waveforms are cache files keyed by media fingerprint and range, safe to delete and regenerate.
- **Rationale**: Transactional writes avoid half-saved projects; the debounce meets the 5-second loss budget without write storms; caches stay out of the source of truth.
- **Alternatives considered**: One JSON file per project (simpler, but harder to query for the project list and filters); DataStore (not suited to relational data).

## 6. Media references, access, and relink

- **Decision**: Store the content URI plus a fingerprint (display name, size, duration, last-modified, media kind). Query the library through the system media store with just-in-time media permissions (including partial "selected items" access), and offer the system file/photo picker as the no-broad-access path. A clip is "missing" when its URI cannot be opened or its fingerprint no longer matches; Relink lets the user pick a replacement and keeps trim and position.
- **Rationale**: Content URIs are the only portable handle on scoped storage; the fingerprint lets us detect moved or replaced files and suggest likely matches when relinking.
- **Alternatives considered**: Copying files into app storage (doubles space for multi-GB footage; rejected in clarification).
- **Validate**: Spike 2 - confirm which picker-returned URIs stay readable across process death and device restart on the minimum and target OS versions, and whether persistable grants are needed for the picker path.

## 7. Thumbnails and waveforms

- **Decision**: Generate timeline thumbnails at fixed time steps with the platform frame extraction API on a bounded background dispatcher, cached to disk. Compute waveform peaks once per media item by decoding audio to PCM and keeping a downsampled peak array in a cache file; render from peaks only.
- **Rationale**: Decoding once and drawing from small arrays keeps scrolling at frame rate and works offline.
- **Alternatives considered**: Re-decoding per draw (too slow); heavyweight third-party waveform libraries (unneeded dependency).

## 8. Adaptive layout and fold posture

- **Decision**: A single `AdaptiveEditorScaffold` chooses the layout from window size class and `FoldingFeature` state: compact -> phone layout; expanded and flat -> multi-pane (rail, library, preview, inspector, timeline); half-opened with horizontal fold -> tabletop (preview above the hinge, controls below); separating hinge -> content split so no interactive element crosses it. Layout choice is a pure function of an `ArrangementInput` so it is unit-testable. The activity handles resize and fold configuration changes itself so the player and view models survive.
- **Rationale**: Satisfies Principle VIII and FR-023 to FR-027 without per-device branches. A pure function makes SC-009 and SC-010 testable without hardware.
- **Alternatives considered**: Separate activities per posture (state loss, restarts); device-model checks (fragile).

## 9. Design conformance

- **Decision**: Implement the designer's tokens (colors, typography, spacing, radii, track colors) in `core:designsystem` and a screenshot test per designed artboard and layout. Reference images are exported from the designer's canvas into the repo's test resources; differences beyond a tolerance fail the build, and approved deviations are recorded in the design notes.
- **Rationale**: Gives SC-008 an objective check instead of eyeballing.
- **Alternatives considered**: Manual design review only (misses regressions).
- **Open**: Choose the screenshot-testing tool at scaffolding (Paparazzi or the Compose preview screenshot testing library) based on current Gradle/AGP compatibility.

## 10. Large-screen input

- **Decision**: Compose drag-and-drop for library-to-timeline; key event handling for Space (play/pause), Left/Right (step), Delete (remove selection), and undo/redo chords, enabled only when a hardware keyboard is present.
- **Rationale**: Directly fulfills FR-027 with standard Compose APIs.

## 11. Dependency injection and navigation

- **Decision**: Hilt for DI; Navigation Compose with a three-destination graph (Projects, Add media, Editor). Add media opens as a full-screen destination on compact and as a pane/dialog on expanded.
- **Rationale**: Standard, testable, and compatible with the module split.

## 12. Minimum clip length and photo duration

- **Decision**: Minimum clip length 100 ms; photo default 5 s, adjustable 0.5 s to 60 s.
- **Rationale**: Prevents zero-length clips (FR-019) and gives photos a sensible range; both are constants in `core:model` and easy to change.
