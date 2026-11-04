# Changelog

All notable changes to PetruVideo are listed here. The format follows [Keep a Changelog](https://keepachangelog.com),
and versions follow [Semantic Versioning](https://semver.org): while the major version is 0, a minor bump (0.x.0) adds
features and a patch bump (0.x.y) only fixes. The version is set in one place, `appVersion` in
[app/build.gradle.kts](app/build.gradle.kts); the version code is derived from it (0.2.5 is 205).

To release: move the Unreleased notes under a new version heading with its date, change `appVersion`, commit, and tag
the commit `vX.Y.Z`.

## [Unreleased]

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
