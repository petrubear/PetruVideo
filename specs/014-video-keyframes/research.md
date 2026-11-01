# Research: Keyframes for Video Clips

## 1. Meaning of the pose for a video clip
- **Decision**: Pose(x, y, size, opacity) with size in permille of the fitted frame (1000 = 100%, range 1000 to 4000) and x, y the permille point of the clip at the frame center (500, 500 = centered). Opacity multiplies the picture toward black.
- **Why**: Reuses `Pose` and all keyframe code unchanged. Size 1000 and center is exactly today's look, so FR-005 and "first keyframe changes nothing" hold.
- **Rejected**: a separate video pose type (duplicates 012's commands and UI).

## 2. Cover rule
- **Decision**: `Framing.clamp(pose)` limits size to 1000..4000 and each of x and y to `[500000/size, 1000 - 500000/size]` (size in permille), i.e. the visible window (width 1000000/size permille) stays inside the clip. Applied when a pose is set (commands, gestures) and again at render time, so imported or interpolated values can never show empty space. Interpolating two valid poses stays valid because the allowed region is convex in (x, y) per zoom and zoom is blended linearly (clamp at render is the safety net).
- **Why**: SC-006 forbids empty edges; doing it in model keeps preview and export equal.

## 3. Rendering
- **Decision**: a `ClipFramingEffect` (BaseGlShaderProgram) per clip with keyframes, last in the clip's chain (after filter and picture effect), so the effect and filter apply to the full frame and the zoom crops the result. It samples the input at `uv' = center + (uv - 0.5) / zoom` and multiplies RGB by opacity. The handle holds the keyframe list and the clip length in a `@Volatile` field, updated live like `ClipFilterEffects`, so editing needs no rebuild.
- **Time basis**: the effect receives `presentationTimeUs` of the clip item. Spike (task T001): confirm it is item-relative and measured after speed change, so it equals clip time on the timeline; if it is source time, scale by the speed.
- **Transitions**: the framing runs per clip before the compositor, so the outgoing and incoming clips are each framed and then dissolved/slid as today (FR-012).
- **Opacity**: multiplying RGB shows the dark background in a single stream; in a transition the compositor blends the darker picture, which looks like the clip fading.
- **Rejected**: compositor `OverlaySettings` scale/anchor for the main input (only exists with multiple inputs, which would force the heavy path for all projects with a keyframe).

## 4. Editing rules
Same as 012 section 6, for video clips: a gesture on a keyframed clip is `SetPoseAt(clipId, playheadRel, clamp(pose))`; on a clip without keyframes a gesture creates a keyframe at the playhead, and also a start keyframe holding the plain look when the playhead is not at the clip start, so the look before the playhead is unchanged and the new values hold from there on.
- Add keyframe: pose at playhead.
- Delete of the last keyframe: back to plain full frame and opacity 100 (spec), unlike overlays which keep values.

## 5. Edits
- **Split at s**, **trim**, **duplicate**: as 012 (reuse `Keyframes.window`).
- **Speed change**: keyframe times scale by oldLength/newLength and are re-normalised to the new length (>= 33 ms apart).
- **Move / reorder / delete neighbours**: keyframes are relative, nothing to do.

## 6. Persistence
No new table. Save also writes `videoLane` keyframes (owner = clip id) and load attaches them. Project duplicate already remaps clip ids; keyframes are re-saved through `saveTimeline`, which maps them. Templates (`TemplateFiller`) build clips from a catalog and carry none.

## 7. Editor
The Keyframes clip tool, panel, diamonds, and gesture auto-key become available when a video clip is selected. The video lane clip cell draws diamonds in a thin strip inside the clip so trim handles stay untouched. Preview pinch/drag on a selected video clip edits zoom/pan (opacity via the slider). Preview gestures need a transform layer over the player; reuse the overlay gesture layer with a video-clip mode.
