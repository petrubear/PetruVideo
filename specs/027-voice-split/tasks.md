# Tasks: Voice Split

## Part 1: Layered audio

- [x] T001 `AudioPlacement`: `MAX_AUDIO_LAYERS`, `layersAt`, `fits`, `nextFreeStart`; `AudioRows.of`; tests
- [x] T002 Update callers: `DetachAudio`, `MoveAudioClip`, `DuplicateAudio`, `AddAudioClips`, `AddVoiceOver`, trim and speed limits; invariant "at most 3 at once"; update existing tests
- [x] T003 Builder: one audio sequence per row; ducking test with overlapping clips; export test (two overlapping clips are mixed, no overlap is unchanged)
- [x] T004 Audio lane view with rows, view model hooks, goldens, accessibility

## Part 2: Split voice

- [x] T005 Dependency, model asset, licence note, checksum test
- [x] T006 DSP: `Stft`, `WavWriter`, resampler, JVM tests
- [x] T007 `OnnxVoiceSplitter`, `VoiceSplitStore`, decode, chunking, sweep; instrumented model test on the emulator
- [x] T008 `SplitVoice` command and tests
- [x] T009 Editor: tool and disabled reasons, state, progress card, cancel on edit, failure message, tests
- [x] T010 Goldens, accessibility, full suite on the emulator only, commit
