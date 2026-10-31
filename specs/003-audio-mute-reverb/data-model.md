# Data Model: Audio Tools - Mute and Reverb

Extends 001 and 002. Only additions are listed.

## Reverb (value, new)

| Field | Type | Rule |
|-------|------|------|
| enabled | Boolean | default false |
| preset | ReverbPreset | SmallRoom, MediumRoom, Hall, Cave, Plate, Custom; default MediumRoom |
| roomSize | Int | 0 to 100; default 50 |
| decayMs | Int | 200 to 10000; default 1500 |
| damping | Int | 0 to 100; default 50 |
| preDelayMs | Int | 0 to 200; default 20 |
| mix | Int | 0 to 100; default 30 |

Preset starting values (tuned by ear later; tests use these constants):

| Preset | roomSize | decayMs | damping | preDelayMs | mix |
|--------|----------|---------|---------|------------|-----|
| SmallRoom | 25 | 600 | 60 | 5 | 25 |
| MediumRoom (default) | 50 | 1500 | 50 | 20 | 30 |
| Hall | 80 | 3500 | 40 | 30 | 35 |
| Cave | 95 | 7000 | 30 | 60 | 45 |
| Plate | 60 | 2200 | 20 | 0 | 30 |

Processing order per clip: reverb first, then the clip's volume gain, so volume 0% silences the tail too. Presets are constants in `ReverbPresets`; `preset` is Custom when the five numbers match no preset. Changing a number sets `preset` to the matching preset or Custom.

## VideoClip (changed)

| Field | Type | Rule |
|-------|------|------|
| muted | Boolean | default false; meaningful only when `attachedAudio` |
| reverb | Reverb | default off; meaningful only when `attachedAudio` |

## AudioClip (changed)

| Field | Type | Rule |
|-------|------|------|
| reverb | Reverb | default off (`muted` already exists) |

## Invariants (added)

1. Every reverb number is inside its range.
2. A clip's reverb never changes its length or position (tail is cut).

## Commands (new)

| Command | Effect | Rejected |
|---------|--------|----------|
| SetVideoMuted(clipId, muted) | Sets the mute of a video clip's attached sound | no attached sound, photo, unknown, no change |
| SetReverbEnabled(clipId, enabled) | Turns reverb on or off, keeping settings | no sound, photo, unknown, no change |
| ApplyReverbPreset(clipId, preset) | Sets all five numbers from the preset and enables reverb | as above, Custom |
| SetReverbSettings(clipId, reverb) | Sets numbers (clamped), recomputes `preset` (Custom if no match), keeps `enabled` | as above, no change |

## Changed commands

| Command | Change |
|---------|--------|
| Split, Duplicate | Copies keep `muted` and `reverb` |
| DetachAudio | New audio clip copies `reverb` and sets `muted` from the video clip's `muted` |
| ReplaceAudio | Keeps `reverb` |

## Persistence

Room version 3 (auto migration from 2): `video_clip` gains `muted` and the seven reverb columns; `audio_clip` gains the reverb columns. See research 6.
