<!--
Sync Impact Report
- Version change: (template, unversioned) -> 1.0.0
- Modified principles: none renamed (initial ratification, all placeholders filled)
- Added sections: Core Principles I-VII, Platform & Technical Constraints,
  Development Workflow & Quality Gates, Governance
- Removed sections: none
- Templates reviewed:
  - .specify/templates/plan-template.md: OK (generic "Constitution Check" gate; no edit needed)
  - .specify/templates/spec-template.md: OK (no constitution-specific references)
  - .specify/templates/tasks-template.md: OK (no constitution-specific references)
  - .claude/skills/speckit-*/SKILL.md: OK (no agent-specific outdated references found)
- Follow-up TODOs: none
-->

# PetruVideo Constitution

PetruVideo is a native Android video editing application whose product goal is functional
parity with CapCut: multi-track timeline editing, effects, transitions, text, audio, filters,
keyframes, templates, and export.

## Core Principles

### I. Native Android, Kotlin-First

The app MUST be a native Android application written in Kotlin, with a Jetpack Compose UI.
Media processing MUST use Android-native or well-maintained native libraries (Media3
Transformer/ExoPlayer, MediaCodec, OpenGL ES/Vulkan, FFmpeg only where Media3 cannot cover
the need). Cross-platform UI frameworks MUST NOT be used for the editor surface.
Rationale: frame-accurate editing and real-time preview require direct access to hardware
codecs and the GPU.

### II. Non-Destructive Editing

Source media MUST never be modified. All edits MUST be stored as a project model (tracks,
clips, effects, keyframes) that references source assets, and MUST be fully reversible via
undo/redo for every user-visible operation. Projects MUST be serializable, versioned, and
autosaved so that a crash or process death never loses more than the last few seconds of work.
Rationale: users trust an editor only if work is never lost or corrupted.

### III. Real-Time Preview Performance (NON-NEGOTIABLE)

Timeline scrubbing and playback preview MUST stay responsive on mid-range devices: target
30 fps preview at 1080p with up to 3 simultaneous video layers, and UI interactions under
100 ms. Heavy work (decoding, effects, thumbnails, waveforms) MUST run off the main thread
and be cancellable. Proxy/cached media MAY be used for preview but export MUST use original
quality. Any change that regresses these budgets MUST be justified in the plan.
Rationale: editing feels broken if preview stutters, regardless of feature richness.

### IV. Feature Parity Through Incremental Delivery

CapCut-equivalent functionality MUST be delivered as independent, prioritized vertical slices
(for example: import and trim, timeline and tracks, text, audio, filters and effects,
transitions, keyframes, speed, export, templates, AI-assisted tools). Each slice MUST be
specified, testable, and shippable on its own. No slice may depend on unfinished later slices.
Copying CapCut proprietary assets, code, branding, or content is prohibited; features MUST be
implemented originally.
Rationale: full parity is a large surface; slicing keeps the app releasable at every step.

### V. Test-First on Core Logic

The editing engine (project model, timeline operations, undo/redo, keyframe interpolation,
export pipeline configuration) MUST be developed test-first: tests written, observed failing,
then implemented. Pure logic MUST have JVM unit tests; media pipeline and UI flows MUST have
instrumented or Compose UI tests on at least one emulator and one physical-device profile
before release. Export output MUST be verified by automated checks (duration, resolution,
codec, audio presence).
Rationale: timeline logic is stateful and regression-prone.

### VI. Clean Architecture and Modularity

Code MUST follow unidirectional data flow (state holders/ViewModels exposing immutable UI
state) and be split into modules with explicit boundaries: UI, domain/editing engine, media
pipeline, and data/persistence. The editing engine MUST NOT depend on Android UI classes so it
can be tested on the JVM. Dependencies MUST be injected (Hilt or equivalent). Complexity
beyond this structure MUST be justified in the plan's Complexity Tracking table.
Rationale: separation keeps the engine testable and lets effects and tools be added without
rewriting the core.

### VII. Privacy, Safety, and Resource Respect

User media MUST stay on-device unless the user explicitly opts in to a cloud feature, which
MUST state what is uploaded. Permissions MUST be requested just in time using scoped storage
and the Android photo picker where possible. The app MUST handle low storage, low memory,
thermal throttling, and interrupted exports gracefully (resume or clear failure message), and
long exports MUST run in a foreground service. Accessibility basics (content descriptions,
touch targets of at least 48dp, scalable text) MUST be met.
Rationale: video files are personal and heavy; the app must not betray trust or kill the device.

## Platform & Technical Constraints

- Minimum SDK 26 (Android 8.0); target SDK MUST track the latest stable Android release
  within the Play Store deadline.
- Build with Gradle Kotlin DSL and a version catalog; dependencies MUST be pinned.
- Supported import: common H.264/HEVC/VP9 video, AAC/MP3/WAV audio, JPEG/PNG/WebP images.
  Export: MP4 (H.264, HEVC where hardware supports) at up to 4K, with selectable
  resolution, frame rate, and bitrate.
- Third-party libraries and bundled assets (fonts, music, stickers, effects) MUST have licenses
  compatible with commercial distribution, recorded in the repository.
- The editor MUST work fully offline; network features are optional enhancements.

## Development Workflow & Quality Gates

- Every feature MUST go through the Spec Kit flow: specify, clarify, plan, tasks, implement.
- The plan's Constitution Check MUST verify each principle; violations require an entry in
  Complexity Tracking with a rejected simpler alternative.
- A change is mergeable only when: build passes, lint and static analysis pass, unit tests
  pass, and relevant instrumented tests pass.
- Changes touching playback, rendering, or export MUST include a recorded performance check
  against the Principle III budgets.
- Releases MUST be smoke-tested on at least one low-end, one mid-range, and one high-end
  device (or emulator profile) before shipping.

## Governance

This constitution supersedes other practices for this project. Amendments require a written
change describing the rationale, an updated Sync Impact Report, and review of affected
templates and guidance documents before merge.

Versioning follows semantic versioning: MAJOR for removing or redefining a principle in a
backward-incompatible way, MINOR for adding a principle or materially expanding guidance,
PATCH for clarifications and wording fixes. All plans and reviews MUST verify compliance with
these principles; unjustified violations block merge. Runtime development guidance lives in
the Spec Kit skills under `.claude/skills/`.

**Version**: 1.0.0 | **Ratified**: 2026-10-30 | **Last Amended**: 2026-10-30
