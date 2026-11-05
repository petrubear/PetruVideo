# Changelog

All notable changes to PetruVideo are listed here. The format follows [Keep a Changelog](https://keepachangelog.com),
and versions follow [Semantic Versioning](https://semver.org): while the major version is 0, a minor bump (0.x.0) adds
features and a patch bump (0.x.y) only fixes. The version is set in one place, `appVersion` in
[app/build.gradle.kts](app/build.gradle.kts); the version code is derived from it (0.2.5 is 205).

To release: move the Unreleased notes under a new version heading, change `appVersion`, commit, tag the commit `vX.Y.Z`,
and push the commit and the tag. The Release workflow then publishes the signed APK and the notes of that version, which
are read from this file, so the heading must be exactly `## [X.Y.Z]` and its section must not be empty. The one-time key
setup and the checks are in the README under "Releasing".

## [Unreleased]

## [0.8.0] - 2026-11-05

### Added
- A Compressor in the audio tools of every clip with sound (video, audio and overlay clips): presets Gentle, Voice, Podcast and Punchy, a Strength and a Level control, and an Advanced section with Threshold, Ratio, Attack and Release. It evens out loud and quiet parts, sits after the Equalizer in the sound chain, is heard live in the preview and rendered by export, and is saved with the project. Existing projects are unchanged because it is off by default. It is available in English and Spanish.

### Removed
- The unused "coming soon" message and dimmed entries in the Projects navigation, the editor tool bar and the unfolded editor rail. Every entry has been active for a long time, so nothing changes on screen.

## [0.7.0] - 2026-11-05

### Added
- A Theme choice in Settings: Follow system (the default, as before), Light or Dark. The choice applies at once without restarting, is remembered, and the app opens in it from the first frame. It is available in English and Spanish.

### Changed
- Continuous integration now runs on branch pushes and pull requests only, so pushing a version tag starts just the Release workflow.

## [0.6.0]

### Added
- The app is available in Spanish (neutral Latin American Spanish) and follows the phone's language: Spanish phones get Spanish, every other language keeps English. On Android 13 and newer PetruVideo also appears in the phone's per-app language settings, and a language change updates the open app without restarting it.
- The continuous integration run checks that no screen holds a literal text, and Android lint fails the build when a Spanish text is missing.

### Changed
- Every text of the app now comes from string resources; the engine no longer returns English sentences (voice split reasons are codes the editor turns into text), and the notification of a running export, the default project name ("Untitled 1" / "Sin título 1"), and the voice-over and voice split names follow the language.
- The text of a template title and the default text of a new text clip are made in the phone's language at the moment they are created.

### Fixed
- Counts read correctly: "1 clip" instead of "1 clips" on project cards, and likewise for keyframes and captions.

### Fixed
- The Release workflow no longer ends when looking up `apksigner` on the runner; the lookup now tolerates unreadable folders and reports a clear error if the tool is missing.

## [0.5.1]

### Added
- Each version tag (`vX.Y.Z`) now publishes a GitHub release with the signed APK (`PetruVideo-X.Y.Z.apk`), its SHA-256 checksum, and the notes of that version from this file. A tag that already exists can be published by running the Release workflow by hand. The README explains how to install the APK and how to set up the signing key and cut a release.

### Fixed
- The minimized release build keeps the ONNX Runtime classes by name, so the voice split model can load in the app that is published.

## [0.5.0]

### Added
- The app has a light look next to the dark one and follows the phone's light or dark setting. It changes while the app is open, without restarting, and the status bar, navigation bar and first frame match the look. Dark looks as before.

## [0.4.0]

### Added
- Picture-in-picture videos now have sound. An overlay with a video that has sound is heard together with the base video and the other overlays, in the preview and in the export, so several instruments recorded on separate videos can play together.
- Each overlay with sound has volume, mute, fade, noise reduction, equalizer, voice effects and reverb, plus Detach audio and Split voice, like a clip on the main lane. Overlays made before this version stay silent until you tap Sound on.
- If the preview cannot play the overlay sound without stalling, it keeps the picture running without it and says so; the export always has it.

## [0.3.0]

### Added
- The Projects screen shows a picture of each project's first clip instead of the colored box (Continue editing card, grid, list, and detail pane). Projects that never had a picture, such as ones made from a template or by an older version, get one made in the background. Deleting a project removes its picture.

## [0.2.7]

### Fixed
- Exported videos with transitions: every second clip (the ones on the second video input) was blown up after its transition. The picture is now scaled by its own size.

## [0.2.6]

### Fixed
- Projects with transitions or templates play to the end in the preview. The preview shows the transitions as hard cuts at the same moments, because two video inputs froze the picture on some phones. The export still blends them.

## [0.2.5] - 2026-11-01

### Fixed
- The preview stayed black after the 0.2.3 change for returning from another app; it shows the picture again.
- Audio of every section of a project with transitions now plays (each stream advances on its own).

### Known issues
- On some phones the picture of a project with transitions stops at the first cut while the sound and the clock go on
  (seen with a Samsung SM-F976B and three clips from different sources). Under investigation.

## [0.2.4] - 2026-11-01

### Fixed
- Projects with transitions (including every template) stopped with an error in the last moments of playback, and Play
  then did nothing. An error in the last 0.6 seconds now counts as reaching the end.

## [0.2.3] - 2026-11-01

### Fixed
- After switching to another app and coming back, the preview went black and Play did nothing. Playback now pauses when
  the app leaves the screen, and the preview player is built again at the same position when it returns.

## [0.2.2] - 2026-11-01

### Fixed
- An error while saving an export's result no longer crashes the app or leaves the export stuck at Rendering; it ends as failed.
- Cancelling an export while it is saved to the gallery now ends as cancelled, not as a failed save.
- Templates no longer count audio-only or missing files as files that did not fit.

## [0.2.1] - 2026-11-01

### Fixed
- The app crashed after folding and unfolding the phone with a project open: the old layout's preview released the
  player after the new layout had taken it.

## [0.2.0] - 2026-11-01

### Added
- Split voice: separates a clip's sound into Voice and Backing clips on the device, with progress and Cancel (feature
  027). Uses the bundled Open-Unmix UMX-HQ model; clips up to 5 minutes.
- Layered audio: up to three audio clips can play at once, on extra rows in the audio lane, so music and a voice-over
  can overlap.
- Filter, Adjust, Fx, and Transition are now in the clip tools of a selected video clip, so they no longer depend on the
  Effects and Filters tabs.
- Templates fill every slot from the videos you choose: with fewer files than slots, the remaining slots continue
  through the unused footage, so one long video fills the whole template.
- This changelog, and a single source for the app version.

### Fixed
- Transitions: playing or seeking again after the end of a project with transitions froze the video and showed an error.
- Play after a player error did nothing; it now builds the player again, and the error message names the cause.
- Voice and Backing clips showed the same waveform because the waveform cache could not tell the two files apart.
- The Export button looked disabled; it now has its normal colour.
- Importing no longer leaves a previous project's picture in the preview of an empty project.

## [0.1.0] - 2026-10

First build, covering features 001 to 026:

- Projects, media import, and a timeline with trim, split, duplicate, reorder, delete, undo, and redo.
- Speed (constant, ramps, reverse, freeze frame), volume, mute, and detach audio.
- Audio: reverb, noise reduction, equaliser, voice effects, ducking, voice-over recording, and music.
- Picture: looks, adjustments, stacked effects (Blur, Pixelate, Mirror, Shake, Glitch, RGB split, Glow, VHS, Grain),
  transitions, transform, and canvas background.
- Keyframes for video, overlays, and text.
- Overlays with opacity, chroma key, and shape masks.
- Text, stickers, shapes, and auto captions.
- Templates, an exports list, settings, and export from 720p to 4K.
- Layouts for portrait, unfolded, and tabletop foldables.
