# Implementation Plan: Reverse and Freeze Frame

**Branch**: `017-reverse-freeze` | **Date**: 2026-11-01 | **Spec**: [spec.md](spec.md)

## Summary

Two independent pieces, delivered in two steps.

**Freeze frame (P1)** is a pure timeline command, `FreezeFrame`, plus a still file. The editor extracts the frame at the playhead from the source (full resolution JPEG in app storage), probes it as a photo `MediaItem`, and sends the command, which splits the clip at the playhead and inserts a photo-style `VideoClip` of the chosen length between the parts. The still copies the clip's transform, filter, and effect (and one keyframe holding the animated pose at that moment), has no sound, and the transition out of the original stays on the last part. No database change: the still is an ordinary photo clip and its media item.

**Reverse (P2)** adds `VideoClip.reversed`. A `ReverseRenderer` in `core:media` writes a backwards copy of a clip's trimmed range into the app's storage: the range is first transcoded with Media3 Transformer to an all-intra H.264 file (every frame a sync frame, no B-frames), then its compressed samples are copied into a new MP4 in reverse order with new ascending times (a lossless remux, no second encode), and the attached sound is decoded to PCM, reversed, and encoded to AAC into the same file. A `ReverseStore` finds a file for a media item and range (any stored file that covers the needed range) and removes unused ones. The composition builder plays a reversed clip from that file (range mapped as `end - x`); the preview and export first make sure the file exists. Room v14 adds one `reversed` column to `video_clip`.

## Technical Context

**Language/Version**: Kotlin 2.4.21, AGP 9.5.0, Gradle 9.8 (offline)
**Primary Dependencies**: Media3 1.11.1 (Transformer for the all-intra pass), `MediaExtractor`, `MediaMuxer`, `MediaCodec` (audio), `MediaMetadataRetriever` (still), Room 2.8 (auto migration 13 to 14), Compose BOM 2026.09, Hilt
**Storage**: Room v14, `video_clip.reversed`; reversed copies under `filesDir/reversed/`, stills under `filesDir/freezes/`
**Testing**: JUnit4 + kotest (freeze command: split, insert, edges, lengths, transitions, copies of settings; reverse flag, range mapping, edit rules), androidTest (reverse render of a synthetic clip: frame order and audio reversed; cancel leaves nothing; freeze still frame equals source frame; export of both; migration; round trip), Paparazzi goldens, accessibility
**Target Platform**: Android minSdk 26
**Project Type**: mobile-app (existing multi-module)
**Performance Goals**: reverse of 10 s of 1080p within 60 s on a mid-range phone with progress; freeze insert under 1 s
**Constraints**: offline; detekt limits; clip reverse limit 30 s; needs free storage about the all-intra size (checked before starting)
**Scale/Scope**: about 22 new or changed files

## Constitution Check

| Principle | Result |
|---|---|
| I Native Kotlin, Media3 | PASS. Transformer, MediaExtractor, MediaMuxer, MediaCodec. |
| II Non-destructive | PASS. Sources untouched; reversed copies and stills are cache and app data owned by the project. |
| III Real-time preview | PASS with measure. Playback of prepared files is ordinary playback; preparation is background work with progress. |
| IV Incremental slices | PASS. Freeze first, then reverse; each ends green. |
| V Test-first on engine | PASS. Commands and mapping are pure and tested first. |
| VI Clean architecture | PASS. Commands in `core:model`, rendering in `core:media`, UI in `feature:editor`. |
| VII Privacy and safety | PASS. All on device; nothing is added to the gallery. |
| VIII Adaptive and foldable-first | PASS by design. Panel and progress reuse existing sheet code. Device and fold checks stay deferred until the user is back. |

Post-design re-check: PASS. No Complexity Tracking entries.

## Project Structure

### Documentation (this feature)

```text
specs/017-reverse-freeze/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/ui-screens.md
└── tasks.md          # created by /speckit-tasks
```

### Source Code

```text
core/model/.../Models.kt                       VideoClip.reversed, reversed range mapping helper
core/model/.../engine/FreezeCommands.kt        FreezeFrame
core/model/.../engine/ReverseCommands.kt       SetReversed
core/model/.../engine/SplitDuplicateDelete.kt, TrimCommands.kt   reversed clips split and trim in mirrored order
core/data/.../db                               column, mapper, migration 13 to 14
core/media/.../freeze/FreezeStills.kt          extract a frame to a JPEG file and probe it
core/media/.../reverse/ReverseRenderer.kt      all-intra pass, reverse remux, reverse audio
core/media/.../reverse/ReverseStore.kt         lookup by media and range, cleanup of unused files
core/media/.../composition/CompositionBuilder.kt   reversed clips read the reversed file
core/media/.../preview/Media3PreviewEngine.kt, core/export/.../TransformerRunner.kt   make sure files exist first
feature/editor/.../ReverseFreezeEditing.kt     view model helpers
feature/editor/.../ui/FreezePanel.kt, ReversePanel.kt, ReverseProgress.kt
```

**Structure Decision**: extend the existing modules; no new module.

## Complexity Tracking

None.
