# Tasks: Effects and Transitions Pack

- [X] T001 Add the ten effect kinds and ten transition kinds in `core/model/.../Effects.kt`; test that the existing ones keep their order in `EffectKindsTest.kt`
- [X] T002 Move the transition math into a pure `TransitionPose` in `core/media/.../composition/TransitionPose.kt` with the five existing kinds unchanged, use it in `OverlayLayout.kt`; test existing values and continuity in `TransitionPoseTest.kt`
- [X] T003 Add the ten new transition poses and tests (first frame is the outgoing clip, last frame is the incoming clip)
- [X] T004 Add the ten effect shaders in `core/media/.../effects/ClipEffects.kt` and make the program source reachable for tests
- [X] T005 Add `EffectShadersTest.kt` (instrumented) that compiles every effect program on a device, and run it on the emulator
- [X] T006 Add the English and Spanish strings for 20 kinds and the flashing notice; test that every kind has a label
- [X] T007 Show the notice when Strobe or Flash is applied in `feature/editor`; test it in `EditorViewModelEffectsTest.kt`
- [X] T008 Run detekt, lint, unit tests, Paparazzi verify (existing goldens unchanged), assembleRelease; CHANGELOG and version 0.9.0; commit
