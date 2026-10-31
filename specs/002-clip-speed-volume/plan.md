# Implementation Plan: Clip Speed and Volume

**Branch**: `002-clip-speed-volume` | **Date**: 2026-10-31 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/002-clip-speed-volume/spec.md`

## Summary

Activate the Speed and Volume tools on video and audio clips. Each clip gains a speed (0.25x to 4x) and a volume (0% to 200%). Speed changes a clip's timeline length, so the engine derives length from the source range and speed, and the main lane ripples. Technical approach: extend the pure-Kotlin engine with two commands and a derived length; add two Room columns per clip table with a version 2 migration; extend `Media3PreviewEngine` to apply per-item speed (pitch kept) and a live-adjustable gain; add a bottom-sheet control, timeline badges, and an inspector section, in all arrangements.

## Technical Context

**Language/Version**: Kotlin 2.4, JVM 17 toolchain (unchanged from 001)

**Primary Dependencies**: Media3 1.11 (CompositionPlayer, Transformer effects, audio processors), Room 2.8, Compose BOM 2026.09, Hilt, Paparazzi. No new libraries.

**Storage**: Room schema version 1 to 2: `speedMilli` and `volumePercent` columns on `video_clip` and `audio_clip`, defaults 1000 and 100, auto-migration or hand-written migration with a test. Originals untouched.

**Testing**: JVM tests for the new commands and the derived-length invariants (test-first, including the 200-seed property test extended to the new commands); Room migration test; view model tests; Compose UI tests and accessibility checks on the Fold; Paparazzi goldens for the sheet, badges, and inspector; playback measurements on the Fold and a mid-range phone.

**Target Platform**: Android, minSdk 26, unchanged. Primary foldable target is the owner's Samsung Fold (SM-F976B, inner 752x835 dp).

**Project Type**: mobile-app (multi-module Gradle build)

**Performance Goals**: 30 fps preview at 1080p with speed up to 4x on the Fold and a mid-range phone; volume drag audible within 100 ms; UI interactions under 100 ms.

**Constraints**: offline; no activity restart on fold or rotation, and an open sheet survives it; at most 5 seconds of work lost; no more than one undo step per gesture.

**Scale/Scope**: 2 tools, 1 bottom sheet used by both, 1 badge, 1 inspector section; touches `core:model`, `core:data`, `core:media`, `feature:editor`, and `core:designsystem` (icons and slider styling only).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Gate | Status |
|-----------|------|--------|
| I. Native Android, Kotlin-First | Media3 effects and audio processors; no new frameworks | PASS |
| II. Non-Destructive Editing | Speed and volume are clip settings; sources untouched; every change is a command with undo | PASS |
| III. Real-Time Preview Performance | 4x decode of 1080p is the heaviest case; measured on the Fold and a mid-range phone; speed rebuild is off the main thread; volume uses a live gain with no rebuild | PASS (verify in tasks; risk R1) |
| IV. Incremental Delivery | Independent slice on top of 001; ramps, keyframes, fades, reverse excluded | PASS |
| V. Test-First on Core Logic | Commands and derived length written test-first on the JVM; migration test | PASS |
| VI. Clean Architecture and Modularity | Engine stays Android-free; sheet state in the view model; preview detail behind `PreviewEngine` | PASS |
| VII. Privacy, Safety, Resources | On-device only; resource-pressure monitor steps preview quality down at high speed | PASS |
| VIII. Adaptive and Foldable-First UI | Sheet and inspector work in compact, medium, expanded, tabletop; open sheet survives fold, unfold, rotate | PASS |

No violations. Complexity Tracking is empty.

**Post-design re-check (after Phase 1)**: PASS. Length derivation stays inside `core:model`; the contract keeps state and events per arrangement; the schema change is additive with defaults.

## Project Structure

### Documentation (this feature)

```text
specs/002-clip-speed-volume/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── ui-screens.md
├── checklists/
│   └── requirements.md
└── tasks.md              # created by /speckit-tasks
```

### Source Code (repository root)

```text
core/model/src/main/kotlin/.../core/model/
├── Models.kt                    # VideoClip and AudioClip gain speedMilli, volumePercent; lengthMs derived
└── engine/
    ├── SpeedVolumeCommands.kt   # SetSpeed, SetVolume (+ limits and Notice.SpeedLimited)
    ├── TrimCommands.kt          # trim in timeline time
    ├── SplitDuplicateDelete.kt  # keep speed and volume; split cut in source time
    ├── AudioClipCommands.kt     # replace keeps settings; detach copies them
    └── TimelineInvariants.kt    # speed and volume ranges, length rule

core/data/src/main/kotlin/.../core/data/db/
├── Entities.kt, Mappers.kt, PetruDatabase.kt   # columns, version 2, migration
core/data/schemas/                               # exported v2 schema

core/media/src/main/kotlin/.../core/media/preview/
├── Media3PreviewEngine.kt       # per-item speed (pitch kept) and gain
├── GainAudioProcessor.kt        # live volume, no rebuild
└── PreviewEngine.kt             # setClipVolumeLive(clipId, percent)

feature/editor/src/main/kotlin/.../feature/editor/
├── EditorContract.kt, EditorViewModel.kt        # tools active, sheet state, events, notices
├── TrimLimits.kt                                # timeline-time limits
└── ui/
    ├── SpeedVolumeSheet.kt                      # bottom sheet, shared by both tools
    ├── ClipBadge.kt                             # "2x" / "40%" badge
    ├── ClipToolBar.kt, VideoLaneView.kt, AudioLaneView.kt
    ├── expanded/InspectorPanel.kt               # Speed and Volume sections
    └── tabletop/TabletopEditorLayout.kt         # sheet in the lower half

core/designsystem/                                # slider and icons if missing

tests: matching src/test and src/androidTest in each module
```

**Structure Decision**: Reuse the 001 multi-module layout. No new modules.

## Risks

- **R1**: 4x playback of 1080p may miss 30 fps on mid-range phones. Mitigation: measure early (spike), let the pressure monitor lower preview resolution, and keep 4x as the limit only if the budget holds; otherwise lower the maximum with a documented decision.
- **R2**: Media3 speed APIs are marked experimental. Mitigation: all use is behind `PreviewEngine`; a spike confirms pitch and A/V sync before the UI is built.
- **R3**: Existing code reads `sourceEndMs - sourceStartMs` as timeline length. Mitigation: make `lengthMs` the only way to read timeline length and add a source-length property; a grep-based review task and the property tests catch leftovers.
