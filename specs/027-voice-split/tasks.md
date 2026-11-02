# Tasks: Voice Split

## Part 1: Layered audio

- [ ] T001 `AudioPlacement`: `MAX_AUDIO_LAYERS`, `layersAt`, `fits`, `nextFreeStart`; `AudioRows.of`; tests
- [ ] T002 Update callers: `DetachAudio`, `MoveAudioClip`, `DuplicateAudio`, `AddAudioClips`, `AddVoiceOver`, trim and speed limits; invariant "at most 3 at once"; update existing tests
- [ ] T003 Builder: one audio sequence per row; ducking test with overlapping clips; export test (two overlapping clips are mixed, no overlap is unchanged)
- [ ] T004 Audio lane view with rows, view model hooks, goldens, accessibility

## Part 2: Split voice

- [ ] T005 Dependency, model asset, licence note, checksum test
- [ ] T006 DSP: `Stft`, `WavWriter`, resampler, JVM tests
- [ ] T007 `OnnxVoiceSplitter`, `VoiceSplitStore`, decode, chunking, sweep; instrumented model test on the emulator
- [ ] T008 `SplitVoice` command and tests
- [ ] T009 Editor: tool and disabled reasons, state, progress card, cancel on edit, failure message, tests
- [ ] T010 Goldens, accessibility, full suite on the emulator only, commit
