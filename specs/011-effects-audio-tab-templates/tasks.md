# Tasks: Effects, Audio Tab, and Templates

Format: `- [ ] ID [P] [Story] description with paths`. Stories: US1 effects, US2 audio tab, US3 templates. Tests come first inside each task (constitution V).

## Phase 1: Foundational

- [X] T001 Spike on the emulator: two alternating video sequences plus a `TransitionLayout` compositor blending by time (alpha, offset, scale, ordering, gap frames, tail gaps); record findings in plan.md (`core/media/.../composition/TransitionLayout.kt`, androidTest)
- [X] T002 Room v10 columns `trn_*`, `fxe_*` on `video_clip`, auto migration, `Migration9To10Test` (`core/data/.../db`)

## Phase 2: US1 Transitions and clip effects (P1)

- [X] T003 [US1] Model `Transition`, `TransitionKind`, `ClipEffect`, `EffectKind` and limits; derived start times and `videoLengthMs` with overlaps, other lanes keep their times (FR-006a); invariants (`core/model/.../Effects.kt`, `Models.kt`, `TimelineInvariants`)
- [X] T004 [US1] Commands `SetTransition`, `RemoveTransition`, `SetClipEffect`, `ResetClipEffect`; clamp with `Notice.TransitionLimited`; split, duplicate, delete, move and relink keep invariants; tests (`core/model/.../engine/EffectCommands.kt`, `EffectCommandsTest`)
- [X] T005 [US1] Persistence mapping, session save, duplicate project; round-trip test (`core/data`)
- [X] T006 [P] [US1] Live GL clip effects Blur, Pixelate, Mirror, Shake with `PreviewQuality.clipEffects` (`core/media/.../effects/ClipEffects.kt`, unit and frame tests)
- [X] T007 [US1] `CompositionBuilder`: A/B sequences and `TransitionLayout` when transitions exist, per-clip effect after filter, audio crossfade over the window; export frame tests for each kind (`CompositionBuilder.kt`)
- [X] T008 [US1] Preview engine: multi-input graph also for transitions, live effect strength, end by position; `TransitionPlaybackTest` including no frame gap above 0.5 s across a transition (`Media3PreviewEngine.kt`)
- [X] T009 [US1] Editor: Effects tab bar, cut markers and selection, transition panel (kinds, length), effect panel (kinds, strength, reset), "Fx" badge, view model events and tests, tool rail entry active (`feature/editor`)
- [X] T010 [US1] Goldens, accessibility and Compose tests for Effects in compact, unfolded, tabletop

## Phase 3: US2 Audio tab and music (P2)

- [X] T011 [US2] Reuse `AddAudioClips` for Add music (no new command); tests that unreadable files are rejected and picker cancel adds nothing
- [X] T012 [US2] Audio file picker and probe (`audio/*`, duration check), `MusicFlow.kt`, effect `PickMusic` (`feature/editor`)
- [X] T013 [US2] Audio tab bar (Fade, Noise, EQ, Voice, Volume, Add music) with dimmed reasons, rail entry active, view model tests
- [X] T014 [US2] Preview and export tests with a music clip (mix, fade, project length); goldens and accessibility

## Phase 4: US3 Templates (P3)

- [X] T015 [US3] `Template`, `Slot`, `TemplateCatalog` (6 templates), `TemplateFiller` with the FR-014 rules; tests first (`core/model/.../template`)
- [X] T016 [US3] Templates screen, view model, route, picker with slot limit, create project and open editor, cancel leaves nothing; Templates nav entry active (`feature/projects/templates`, `ProjectsScreen.kt`, `PetruNavHost.kt`)
- [X] T017 [US3] Drawn style samples, two columns from 840 dp, goldens, accessibility, flow test (fill four videos, edit, export)

## Phase 5: Polish

- [X] T018 Run `testDebugUnitTest verifyPaparazziDebug detekt lintDebug` and `connectedDebugAndroidTest`; stop the emulator; fix findings; record results in quickstart.md and the preview fps check
- [ ] T019 Device checks (fold, mid-range fps) and designer review: deferred until the user is back

## Dependencies

T001 and T002 first. US1: T003 → T004 → T005; T006 in parallel; T007 needs T001, T003, T006; T008 needs T007; T009 needs T004, T008; T010 last. US2 can start after T002; T013 needs T011 and T012. US3 needs T003 for transitions (cuts only if skipped). T018 after all.

## Implementation strategy

MVP is US1 with Dissolve only, then the other kinds. Each story ends green and committed before the next.
