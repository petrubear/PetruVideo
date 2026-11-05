# PetruVideo

A native Android video editor. Everything runs on the device: no account, no network, no cloud.

## Download

Each version is published on the [Releases page](https://github.com/petrubear/PetruVideo/releases) as a signed APK
(`PetruVideo-X.Y.Z.apk`) with a checksum file and the notes of that version. It needs Android 8.0 or newer.

1. On the phone, open the latest release and download the `.apk` file.
2. Open the file. If Android asks, allow your browser or file manager to "install unknown apps" and go back.
3. Tap Install. Later versions install over this one without removing the app, because every release is signed with the same key.

To check the download, compare `sha256sum PetruVideo-X.Y.Z.apk` with the `.sha256` file of the release.

## Features

- **Projects**: create, duplicate, and reopen projects; media is referenced in place and the originals are never changed.
- **Timeline**: trim, split, duplicate, reorder, and delete clips, with undo and redo.
- **Speed**: constant speed, speed ramps (curve editor), reverse, and freeze frame.
- **Picture**: filters and looks, adjustments (brightness, contrast, exposure, gamma, highlights, shadows, saturation, warmth, tint, hue, sharpen, vignette), up to three stacked effects per clip (19 effects, from Blur, Glitch and VHS to Fisheye, Old film and Cartoon), 15 transitions, transform (turn, mirror, crop), and canvas background (colour or blur).
- **Keyframes**: zoom, pan, and fade over time for video, overlays, and text.
- **Overlays**: picture in picture with opacity, chroma key, and shape masks (circle, rectangle, linear).
- **Text and stickers**: styled text with animations, rotation, shadow, letter and line spacing, stickers and shapes, and auto captions from the on-device speech recogniser.
- **Audio**: volume, mute, detach, split voice into voice and backing stems, up to three overlapping audio layers, music, voice-over recording, reverb, noise reduction, equaliser, compressor, voice effects, and audio ducking.
- **Templates** for quick starts.
- **Export**: 720p to 4K at 24 to 60 fps, saved to `Movies/PetruVideo`, with an exports list.
- **Foldables**: portrait, unfolded, and tabletop layouts.
- **Theme**: follow the phone's light or dark setting, or choose Light or Dark in Settings.
- **Languages**: English and Spanish, following the phone's language (per-app language on Android 13 and newer). Translation rules are in [docs/glossary-es.md](docs/glossary-es.md); every text lives in string resources and the build checks it.

## Build

Requirements: JDK 17 or newer and the Android SDK (set `sdk.dir` in `local.properties`).

```bash
./gradlew :app:installDebug      # build and install on a connected device (as com.petruvideo.debug, beside the release)
./gradlew testDebugUnitTest      # unit tests
./gradlew verifyPaparazziDebug   # screenshot tests
./gradlew detekt lintDebug       # static checks
./gradlew connectedDebugAndroidTest   # instrumented tests (device or emulator)
```

`connectedDebugAndroidTest` runs on every connected device, so set `ANDROID_SERIAL` to the emulator when a phone is also connected.

## Releasing

A release is made by pushing a version tag; the [Release workflow](.github/workflows/release.yml) builds the signed
APK and publishes it with the notes from the CHANGELOG.

One-time setup of the signing key (keep a safe copy of the key file; if it is lost, users must uninstall to update):

```bash
keytool -genkeypair -v -keystore petruvideo-release.jks -alias petruvideo -keyalg RSA -keysize 4096 -validity 10000
base64 -i petruvideo-release.jks | pbcopy     # copy the key file as text
```

Then add four repository secrets (Settings, Secrets and variables, Actions): `RELEASE_KEYSTORE_BASE64` (the copied
text), `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS` (`petruvideo` above), and `RELEASE_KEY_PASSWORD`. The key is
never stored in the repository or printed in a log.

For each version:

1. Set `appVersion` in `app/build.gradle.kts` and move the Unreleased notes of [CHANGELOG.md](CHANGELOG.md) under a
   `## [X.Y.Z]` heading.
2. Commit, tag `vX.Y.Z`, and push the commit and the tag: `git tag vX.Y.Z && git push origin master vX.Y.Z`.
3. Open the Actions tab; when the Release run ends, the release is on the Releases page.

The run stops without publishing, and says why, when the tag does not match `appVersion`, the CHANGELOG has no section
for the version, a signing secret is missing, or the tests fail. To publish a tag that already exists, run the
"Release" workflow by hand from the Actions tab and give the tag (for example `v0.5.0`); running it again for the same
tag updates that release instead of making a second one.

To try the release build on your machine, set `RELEASE_KEYSTORE_FILE`, `RELEASE_KEYSTORE_PASSWORD`,
`RELEASE_KEY_ALIAS`, and `RELEASE_KEY_PASSWORD` and run `./gradlew :app:assembleRelease`; without them the release
build is left unsigned. `scripts/test-changelog-notes.sh` tests the script that reads the CHANGELOG.

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

Features are developed with Spec Kit: each one has a folder in [specs/](specs) with its spec, plan, tasks, and test notes (001 onward). Project rules for contributors are in [CLAUDE.md](CLAUDE.md).

## Versions

The version is set in `app/build.gradle.kts` and every change is listed in [CHANGELOG.md](CHANGELOG.md).

## Licenses

The app bundles the Manrope and JetBrains Mono fonts under the SIL Open Font License; see [licenses/](licenses).
