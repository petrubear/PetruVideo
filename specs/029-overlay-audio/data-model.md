# Data Model: Overlay Audio

## OverlayClip (changed)

New fields, same types as `VideoClip`:

| Field | Type | Default (new row) | Default (old row) | Notes |
|---|---|---|---|---|
| `attachedAudio` | Boolean | true if the source has sound, else false | false | Off after Detach or Split voice |
| `muted` | Boolean | false | false | Keeps the picture |
| `volumePercent` | Int | 100 | 100 | 0 to `VOLUME_MAX_PERCENT` |
| `reverb` | Reverb | off | off | |
| `fx` | AudioFx | neutral | neutral | noise, equalizer, voice fx, fade |

Rules: the sound range is the picture range (`sourceStartMs..sourceEndMs` at `startMs`). Photo overlays and sources without a sound track have `attachedAudio = false` and reject sound commands (`NoAttachedAudio`). `normalized()` clamps volume and fades (fade limited to half the length). Split, duplicate, and delete copy or remove the whole overlay with these fields.

## overlay_clip table (schema 22 to 23)

New columns with defaults that keep old rows silent: `attachedAudio` (0), `muted` (0), `volumePercent` (100), reverb columns and fx columns with the defaults used by the `video_clip` table. Exported schema `23.json` is added; the migration is tested with `MigrationTestHelper`.

## Detached overlay sound

An `AudioClip` with `origin = Detached`, `mediaItemId` and source range of the overlay, `timelineStartMs = overlay.startMs`, `volumePercent`, `muted`, `reverb`, `fx` copied from the overlay.

## State transitions

`attachedAudio`: true to false on Detach or Split voice (undo reverses); false to true only by undo or by a new overlay. (Turning sound on for an old silent overlay is a plain "sound on" toggle, a `SetOverlaySound` command, per FR-008.)
