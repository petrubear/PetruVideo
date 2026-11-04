# Implementation Plan: Project Card Preview

**Branch**: `028-project-card-preview` | **Date**: 2026-11-04 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/028-project-card-preview/spec.md`

## Summary

Show the project's first-frame picture on every project view instead of the colored box. The picture already exists: `ProjectThumbnailUpdater` writes `files/project-thumbs/<projectId>.png` and stores its path in `Project.thumbnailRef` whenever the editor sees a new first clip. The Projects screen never reads it. The plan has three parts: (1) draw `thumbnailRef` in the four views with the gradient as the fallback, with a cache key so a replaced file shows; (2) make the picture for projects that have clips but no picture (templates never opened, older projects) from the Projects screen, one at a time in the background; (3) remove the file with the project and give a duplicate its own copy. No motion preview (clarified out of scope).

## Technical Context

**Language/Version**: Kotlin 2.4.21, Jetpack Compose

**Primary Dependencies**: Coil 3 (`coil-compose`, already used by the Export screen), existing `ThumbnailStripGenerator` and `ProjectThumbnailUpdater`, Room, Hilt

**Storage**: PNG files in app `filesDir/project-thumbs/`; path in the existing `Project.thumbnailRef` column (no schema change)

**Testing**: JVM unit tests (fake repository, fake refresher), Compose/Paparazzi screenshot tests for the cards with and without a picture, instrumented check on a phone

**Target Platform**: Android 8.0+ (minSdk 26), phone, foldable inner and cover screens, tablet

**Project Type**: mobile-app (multi-module: `feature:projects`, `core:media`, `core:data`)

**Performance Goals**: picture shown within 2 s for a project without one; no scroll stutter with 50 projects; one background extraction at a time

**Constraints**: offline, on-device only, no new permission; work off the main thread and cancellable (Principle III)

**Scale/Scope**: up to about 50 projects; picture height 256 px (existing constant)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Check |
|---|---|
| I. Native Android, Kotlin-first | Pass: Compose and existing Kotlin modules. |
| II. Non-destructive editing | Pass: sources are only read; the picture is a derived file. |
| III. Real-time preview performance | Pass: no playback or export path touched; extraction is off the main thread, one at a time, cancelled when the screen leaves. |
| IV. Incremental delivery | Pass: still picture alone is shippable. |
| V. Test-first on core logic | Pass: backfill selection and file cleanup get JVM tests first; cards get screenshot tests. |
| VI. Clean architecture | Pass: ViewModel exposes state; media work stays in `core:media`; injected through Hilt. |
| VII. Privacy and resources | Pass: files stay on the phone and are deleted with the project; extraction is limited to one at a time for low memory. |
| VIII. Adaptive and foldable-first | Pass: the picture fills whatever box the existing adaptive layouts give it; verified at compact, medium, expanded and tabletop, and across fold and unfold with no activity restart. |

No violations; Complexity Tracking is empty.

## Project Structure

### Documentation (this feature)

```text
specs/028-project-card-preview/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── tasks.md             # created by /speckit-tasks
```

No `contracts/`: the feature adds no external interface.

### Source Code (repository root)

```text
core/data/src/main/kotlin/com/petruvideo/core/data/repo/RoomProjectRepository.kt   # delete file with project; duplicate gets own copy
core/media/src/main/kotlin/com/petruvideo/core/media/thumbs/ProjectThumbnailUpdater.kt   # unique file per change; force refresh for backfill
feature/projects/src/main/kotlin/com/petruvideo/feature/projects/
├── ProjectsViewModel.kt            # background backfill of missing pictures, one at a time
└── ui/
    ├── PlaceholderThumbnail.kt     # becomes ProjectThumbnail: picture when present, gradient otherwise
    ├── ProjectCard.kt
    ├── ContinueEditingCard.kt
    ├── ProjectRow.kt
    └── ProjectDetailPane.kt
feature/projects/build.gradle.kts   # add coil-compose
feature/projects/src/test/ ...      # backfill tests; screenshot tests
```

**Structure Decision**: reuse the existing modules; the only new Gradle dependency is `coil-compose` in `feature:projects`.

## Complexity Tracking

None.
