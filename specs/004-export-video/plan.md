# Implementation Plan: Export Video

**Branch**: `004-export-video` | **Date**: 2026-10-31 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/004-export-video/spec.md`

## Summary

Render a project to an MP4 (H.264 video, AAC audio) in the gallery with chosen resolution, frame rate, and quality, in the background with progress, cancel, and clear failures. Technical approach: build the render from the same `Composition` as the preview (one shared builder, so speed, volume, mute, and reverb match), run it with Media3 `Transformer` inside a foreground service, set bitrate through the encoder settings (constant bitrate for predictable size), probe the device's encoders for the options it can do, cap options by the project's sources, and save through MediaStore. Add a `core:export` module (pipeline, service, estimator, MediaStore saver), a `feature:export` module (screen and view model), and a Room v4 step for settings and the last job.

## Technical Context

**Language/Version**: Kotlin 2.4, JVM 17 toolchain (unchanged)

**Primary Dependencies**: Media3 1.11 (Transformer, effects, `DefaultEncoderFactory`, `EncoderUtil`), Room 2.8, Compose BOM 2026.09, Hilt, Paparazzi. No new libraries.

**Storage**: Room schema 3 to 4: `export_settings` (per project) and `export_job` (latest job per project) tables via auto migration. Output goes to MediaStore (`Movies/PetruVideo`); temporary render file in the app cache, deleted on finish, cancel, or failure.

**Testing**: JVM tests for the bitrate table, size estimate, caps from sources, defaults, and state machine; Room migration test; view model tests; device tests on the emulator and the Fold that export small projects and check the file with `MediaMetadataRetriever` (length, size, frame rate, audio track), cancel cleanup, and service behaviour in the background; Compose and accessibility tests; Paparazzi goldens for the screen in each arrangement; measured render speed on the Fold and a mid-range phone.

**Target Platform**: Android, minSdk 26, targetSdk 37. Saving: MediaStore without storage permission on API 29 and up; on API 26 to 28 the legacy write permission (`maxSdkVersion 28`) is requested at export time. Foreground service type `mediaProcessing` (API 35 and up declares it; lower versions use the plain foreground service). Notification permission (API 33 and up) is requested at first export and its refusal never blocks the export.

**Project Type**: mobile-app (multi-module Gradle build)

**Performance Goals**: 1080p 30 fps one-minute project renders in at most 2x its length on the Fold and 4x on a mid-range phone; settings changes update the estimate in under 100 ms; cancel stops within 2 s; progress updates at least once a second.

**Constraints**: offline; no activity restart on fold or rotation, the render and the screen state survive; one export at a time; never partial files in the gallery; sources and project untouched.

**Scale/Scope**: 1 screen, 1 service, 1 notification, 2 tables; touches `core:model` (caps and defaults), `core:media` (shared composition builder), new `core:export` and `feature:export`, `core:data`, `app` (navigation, manifest), `core:designsystem` (segmented control exists).

## Constitution Check

| Principle | Gate | Status |
|-----------|------|--------|
| I. Native Android, Kotlin-First | Media3 Transformer, MediaStore, foreground service | PASS |
| II. Non-Destructive Editing | Export writes a new file only; sources and project untouched; render uses a snapshot | PASS |
| III. Real-Time Preview Performance | Rendering runs off the main thread in a service; preview is unaffected unless the user previews during a render (render yields under pressure); export speed measured | PASS (verify, risks R1 and R2) |
| IV. Incremental Delivery | Independent slice; other formats, social posting, effects not yet existing are excluded | PASS |
| V. Test-First on Core Logic | Bitrate table, estimate, caps, defaults, state machine on the JVM first; device tests for the pipeline | PASS |
| VI. Clean Architecture and Modularity | Pure logic in `core:model`/`core:export`, UI state in the view model, service is a thin shell around a coordinator; the composition builder is shared, not duplicated | PASS |
| VII. Privacy, Safety, Resources | On-device only; no new permissions beyond the legacy write on API 26 to 28 and notifications; thermal and memory pressure slow or pause the render with a message | PASS |
| VIII. Adaptive and Foldable-First UI | Phone, unfolded (preview beside settings), tabletop layouts; render survives fold and rotation | PASS |

No violations. Post-design re-check: PASS (the shared composition builder keeps preview and export identical by construction).

## Project Structure

### Documentation (this feature)

```text
specs/004-export-video/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/ui-screens.md
├── checklists/requirements.md
└── tasks.md              # created by /speckit-tasks
```

### Source Code (repository root)

```text
core/model/src/main/kotlin/.../core/model/export/
├── ExportSettings.kt        # Resolution, FrameRate, Quality, defaults, caps from sources
├── BitrateTable.kt          # bitrate per resolution, frame rate, quality; size estimate
└── ExportState.kt           # Waiting, Rendering(progress), Finished(uri), Cancelled, Failed(reason)

core/media/src/main/kotlin/.../core/media/
├── composition/CompositionBuilder.kt   # moved out of Media3PreviewEngine; used by preview and export
└── preview/Media3PreviewEngine.kt      # now calls CompositionBuilder

core/data/.../db/             # export_settings and export_job tables, Room v4 (auto migration), schemas/4.json

core/export/src/main/kotlin/.../core/export/
├── ExportCoordinator.kt     # singleton: one job at a time, state flow, snapshot, start/cancel/retry
├── TransformerRunner.kt     # builds the export Composition, runs Transformer, reports progress
├── EncoderCapabilities.kt   # what the device can encode (resolution, frame rate), behind an interface
├── GallerySaver.kt          # MediaStore insert with IS_PENDING, copy, publish; deletes on failure
├── ExportService.kt         # foreground service, notification with progress and Cancel
└── ExportRecovery.kt        # marks an interrupted job as failed on app start

feature/export/src/main/kotlin/.../feature/export/
├── ExportContract.kt, ExportViewModel.kt
└── ui/ExportScreen.kt, ExportSettingsPanel.kt, ExportProgressCard.kt, ExportLayouts (compact, expanded, tabletop)

app/                          # navigation destination, manifest (service, permissions), DI

tests: matching src/test and src/androidTest in each module
```

**Structure Decision**: Two new modules (`core:export`, `feature:export`) because the pipeline and service are large and have their own manifest entries; everything else reuses the 001 layout.

## Risks

- **R1**: Render speed on mid-range phones at 4K. Mitigation: encoder capability probe hides what the device cannot do, a spike measures speed, the estimate of time is shown only as a range, and pressure handling slows instead of failing.
- **R2**: Preview and export drifting apart. Mitigation: one `CompositionBuilder`; a device test exports a project and compares length, audio presence, and sampled frames against expectations.
- **R3**: Foreground service rules differ by Android version (type, notification permission, background start limits). Mitigation: start the service only from the foreground user action, test on API 34 emulator and the Fold (API 37), and keep the render running even when the notification is hidden.
- **R4**: Size estimate accuracy (15%). Mitigation: constant bitrate mode and a measured correction factor per resolution recorded in research.
- **R5**: Partial files after kill. Mitigation: render to cache, publish to MediaStore with `IS_PENDING = 1` only after the render succeeds, and clean the cache on next start.
