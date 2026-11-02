# PetruVideo

A native Android video editor. Everything runs on the device: no account, no network, no cloud.

## Features

- **Projects**: create, duplicate, and reopen projects; media is referenced in place and the originals are never changed.
- **Timeline**: trim, split, duplicate, reorder, and delete clips, with undo and redo.
- **Speed**: constant speed, speed ramps (curve editor), reverse, and freeze frame.
- **Picture**: filters and looks, adjustments (brightness, contrast, exposure, gamma, highlights, shadows, saturation, warmth, tint, hue, sharpen, vignette), up to three stacked effects per clip (Blur, Pixelate, Mirror, Shake, Glitch, RGB split, Glow, VHS, Grain), transitions, transform (turn, mirror, crop), and canvas background (colour or blur).
- **Keyframes**: zoom, pan, and fade over time for video, overlays, and text.
- **Overlays**: picture in picture with opacity, chroma key, and shape masks (circle, rectangle, linear).
- **Text and stickers**: styled text with animations, stickers and shapes, and auto captions from the on-device speech recogniser.
- **Audio**: volume, mute, detach, split voice into voice and backing stems, up to three overlapping audio layers, music, voice-over recording, reverb, noise reduction, equaliser, voice effects, and audio ducking.
- **Templates** for quick starts.
- **Export**: 720p to 4K at 24 to 60 fps, saved to `Movies/PetruVideo`, with an exports list.
- **Foldables**: portrait, unfolded, and tabletop layouts.

## Build

Requirements: JDK 17 or newer and the Android SDK (set `sdk.dir` in `local.properties`).

```bash
./gradlew :app:installDebug      # build and install on a connected device
./gradlew testDebugUnitTest      # unit tests
./gradlew verifyPaparazziDebug   # screenshot tests
./gradlew detekt lintDebug       # static checks
./gradlew connectedDebugAndroidTest   # instrumented tests (device or emulator)
```

`connectedDebugAndroidTest` runs on every connected device, so set `ANDROID_SERIAL` to the emulator when a phone is also connected.

## Stack

Kotlin, Jetpack Compose, Media3 (Transformer and CompositionPlayer), Room, Hilt, and Paparazzi for screenshot tests. Minimum Android version is 8.0 (API 26).

## Modules

| Module | Purpose |
| --- | --- |
| `app` | Application, navigation, dependency injection |
| `core:model` | Pure JVM editing engine: immutable timeline and commands |
| `core:data` | Room database, repositories, autosave |
| `core:media` | Preview engine, composition builder, GL effects, audio processors |
| `core:export` | Export pipeline and service |
| `core:designsystem` | Theme, components, adaptive layouts |
| `feature:projects`, `feature:importmedia`, `feature:editor`, `feature:export` | Screens |

The editing model is a single immutable timeline changed only by pure commands, so undo, redo, preview, and export all work from the same state. See [docs/architecture.md](docs/architecture.md).

## Development process

Features are developed with Spec Kit: each one has a folder in [specs/](specs) with its spec, plan, tasks, and test notes (001 to 027). Project rules for contributors are in [CLAUDE.md](CLAUDE.md).

## Licenses

The app bundles the Manrope and JetBrains Mono fonts under the SIL Open Font License; see [licenses/](licenses).
