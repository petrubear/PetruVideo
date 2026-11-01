# Research: Speed Ramps

## 1. Curve in played source time
- **Decision**: points are `(positionMs, speedMilli)` where `positionMs` runs from 0 (start of playback) to the trimmed source length, in play direction (so reversing a clip needs no change to the curve). Speed changes linearly between points (0.1x to 10x, stored as 100..10000).
- **Why**: Media3's `SpeedProvider` is keyed by the input media time, which is exactly this axis (the reversed copy plays in the same direction). Trims and splits cut the axis cleanly.
- **Rejected**: a curve over the timeline (changes meaning when the length changes); a curve in permille of the clip (trims would stretch it).

## 2. One set of steps for model, preview, and export
- **Decision**: `SpeedCurve.steps` cuts the axis at boundaries about 100 ms of timeline apart (source step = clamp(100 ms x speed, 20 ms, 500 ms)); each step has a constant speed equal to the curve's speed at its middle. The timeline length is the sum of `step / speed`, rounded. `sourceAt(timelineMs)` and `timelineAt(sourceMs)` walk the same steps. The builder's provider returns exactly these steps.
- **Why**: the player only supports piecewise constant speed, so model and player must share the same pieces or lengths drift (SC-002). The sum is cached per curve (`lazy`).
- **Rejected**: a closed-form integral of the linear speed (exact for the curve but not for what the player plays).

## 3. Windowing
`windowed(fromMs, toMs)` keeps the points between `from` and `to`, adds a point at each new edge holding the speed there (the curve's speed, or the edge speed when the window goes beyond the curve), and shifts positions by `-from`. Used by Split (two windows), Trim, relink clamp, and Freeze (through Split). It keeps the speed at every moment, so lengths of the parts add up to the whole within a step.

## 4. Length and the rest of the model
`VideoClip.lengthMs` is the curve's timeline length when there is a curve, else the constant formula. An invariant says the curve's last position equals the clip's source length. Constant `speedMilli` is kept at its value and ignored while a curve is on; Reset removes the curve (speed returns to the stored constant, 1x after a reset).

## 5. Provider in the builder
`speedOf(clip)` returns `SpeedParameters(provider, true)` with the steps (microsecond boundaries); for a reversed clip the provider is the same because the reversed copy plays in the same direction. Sound keeps its pitch (the flag already used for constant speed).

## 6. Presets
Six shapes as fractions of the clip (see the spec's assumptions) scaled to the trimmed source length. A preset replaces the curve; its length may differ from the constant length, which is the point.

## 7. Edit limits
A change is refused (clamped with a message) when the resulting timeline length would be under `MIN_CLIP_MS` or over 10 minutes. Points: 2 to 12, positions strictly increasing at least 100 ms apart, ends fixed at 0 and the source length.

## 8. Persistence
Room 14 to 15 auto migration: `video_clip.speedCurve TEXT` (null = no curve). Points are stored as `position:speed` pairs joined by `;`; unreadable text loads as no curve.

## 9. Editor
A `Speed curve` clip tool opens a panel: six preset chips, a graph (time along x, speed on a log scale 0.1x to 10x on y) with draggable points, buttons Add point, Delete point, nudge left/right/faster/slower, a readout "Clip length 4.2 s (was 6.0 s)", and Reset. The constant Speed tool is dimmed with "This clip uses a speed curve". Dragging previews the readout only; the curve applies on release as one undo step.
