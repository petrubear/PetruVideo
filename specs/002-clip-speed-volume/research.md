# Research: Clip Speed and Volume

## 1. Speed in preview

- **Decision**: Apply speed per `EditedMediaItem` in `Media3PreviewEngine` using Media3's speed support (`EditedMediaItem.Builder.setSpeed` with a constant `SpeedProvider` and pitch maintained). Photos get no speed. Because speed shortens or lengthens each item, the composition is rebuilt on commit (as it already is for trims), not on every drag step.
- **Rationale**: Media3 keeps audio and video timestamps consistent when the speed is set on the item, which keeps A/V sync (SC-006). Pitch is preserved by default for audio, matching FR-009.
- **Alternatives considered**: Manual `SpeedChangeEffect` plus `SonicAudioProcessor` per stream (two places to keep in step, easy to drift); a second player for sped clips (breaks the single clock).
- **Spike S1 progress (2026-10-31, Pixel 7 Pro emulator API 34, Media3 1.11.1)**: `EditedMediaItem.Builder.setSpeed(SpeedParameters(constantSpeedProvider, shouldMaintainPitch = true))` works with `CompositionPlayer`: 2x of a 2 s clip gives a 1 s composition that plays to the end, 0.5x gives 4 s, and speed on an audio-sequence item plays with a live `GainAudioProcessor` (`PreviewEngineTest`). Not yet measured: pitch, A/V sync within 40 ms, and 30 fps at 0.25x, 2x, 4x on the Fold and a mid-range phone (T003, T032, T050).
- **Validate (spike S1, before UI work)**: on the Fold, confirm that 0.25x, 2x, and 4x of a 1080p H.264 clip with sound keep 30 fps (via `PlaybackStats`), keep pitch, and stay in sync with the video within 40 ms across a clip boundary. The Media3 speed APIs are experimental; the spike records the exact method names.

## 2. Volume in preview

- **Decision**: A small custom audio processor (`GainAudioProcessor`) per audio-carrying item with a volatile gain. A volume drag updates the gain directly (heard within one audio buffer); a commit stores the value in the clip and the next rebuild uses it. Gain above 100% applies a soft limiter to avoid harsh clipping.
- **Rationale**: Rebuilding the composition per drag step would stutter and miss the 100 ms target. A live gain avoids it.
- **Alternatives considered**: `ChannelMixingAudioProcessor` with a scaled matrix (needs a rebuild per change); player-level volume (affects every clip, not one).

## 3. Clip length and speed in the engine

- **Decision**: Store speed as whole thousandths (`speedMilli`: 1000 = 1x, 250 to 4000) and volume as whole percent (0 to 200). Timeline length is `(sourceEnd - sourceStart) * 1000 / speedMilli`, rounded to the nearest millisecond. Trim, split, and limits convert between source time and timeline time in one place.
- **Rationale**: Integers keep equality and undo exact, serialize without float drift, and match the engine's millisecond style.
- **Alternatives considered**: Float speed (equality and rounding surprises); storing timeline length separately (two sources of truth).
- **Rounding**: a split cuts in source time at the exact converted position, so the two halves cover the source range exactly; their timeline lengths may differ from the original by at most 1 ms, which the invariants allow.

## 4. Limits and notices

- **Decision**: `SetSpeed` clamps to the valid range, then to the minimum length (0.1 s) on the fast side; on the audio lane it also clamps to the slowest speed that avoids overlap with the next clip. The command returns `Applied` with a `SpeedLimited` notice when it clamped; the view model shows a message.
- **Rationale**: Matches FR-008 and FR-011 and the 001 pattern (`Snapped`, `LengthChanged`).

## 5. Main-lane ripple

- **Decision**: No new ripple code. The video lane is a gapless ordered list, so changing a clip's length already moves later clips.
- **Rationale**: 001 derives video positions from order.

## 6. Persistence

- **Decision**: Room schema version 2 adds `speedMilli` (default 1000) and `volumePercent` (default 100) to both clip tables, with a migration test and the exported schema committed.
- **Rationale**: Additive columns with defaults keep existing projects valid.

## 7. UI

- **Decision**: One bottom sheet for both tools (clarified): a title, a numeric readout, quick choices (speed only), and a slider; speed commits on release, volume streams live and commits on release. Badges on clips; inspector sections on expanded width; the sheet fills the lower half in tabletop.
- **Rationale**: Keeps the timeline and preview visible while adjusting, as clarified.
- **Alternatives considered**: Replacing the tool bar, or a full-screen panel (both rejected in clarification).

## 8. Reference devices

- **Decision**: Same as 001 (Samsung Fold SM-F976B as primary); a mid-range phone is still needed for the 30 fps check at 4x.
