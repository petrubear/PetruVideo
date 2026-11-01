# Research: Keyframes

## 1. One pose function for preview and export
- **Decision**: `Keyframes.poseAt(base, keyframes, relMs)` returns `Pose(x, y, size, opacity)`. With no keyframes it returns the clip's plain values. With keyframes it holds the first keyframe before it and the last after it, and between two keyframes it blends each value from the earlier one to the later one with the earlier one's motion (Linear: t, Ease: smoothstep t*t*(3-2t)).
- **Why**: preview and export already share `OverlayLayout` and the text overlay; calling the same pure function in both guarantees the same values. Tests compare it against hand-computed values.
- **Rejected**: baking the animation into frames (breaks live editing); per-property keyframe lists (more diamonds, harder UI than the spec asks for).

## 2. Time basis
Keyframe time is relative to the clip's start, so moving a clip keeps its animation. At render time the clip's time is `presentationMs - clip.startMs`.

## 3. Plain values versus keyframes
With keyframes the plain x, y, size, and opacity are ignored. To keep one source of truth the commands keep them equal to the first keyframe's values, so code that still reads the plain values (lane previews, thumbnails) shows a sensible start state.

## 4. Text size animation
A text clip's bitmap is rendered once at one size. Size keyframes scale the drawn overlay, so the bitmap is rendered at the largest size among the keyframes and scaled down for the others; this keeps large text sharp. The text entrance and exit animation run on top: final alpha is keyframe alpha times animation alpha, final scale is keyframe scale times animation scale, and the slide offset adds to the keyframe position.

## 5. Overlay size
An overlay's size is its width share, already applied as a scale in `OverlayLayout`; it simply reads the animated width.

## 6. Editing rules
- A gesture on a clip with keyframes becomes `SetPoseAt(clipId, playheadRel, pose)`: it changes the keyframe at the playhead, or creates one there.
- A gesture on a clip without keyframes edits the plain values as today.
- Add keyframe makes one at the playhead holding the shown values. Delete of the last keyframe sets the plain values to that keyframe's values so the look does not jump.
- Two keyframes at least one frame (33 ms) apart; moving stops at neighbours.

## 7. Split, trim, duplicate, move
- **Split at s**: first part keeps keyframes before s plus an end keyframe holding `poseAt(s)`; second part keeps those after s shifted by s, plus a start keyframe holding `poseAt(s)`. If a keyframe sits exactly at s it is kept on both sides.
- **Trim start by d** (clip start moves later) or **trim end**: keyframes outside the new range are removed, and the value at each new edge is kept by a keyframe there (only when the clip had keyframes).
- **Duplicate**: copies the list. **Move**: nothing to change (relative times).

## 8. Persistence
Room 10 to 11, auto migration: new table `keyframe` with an index on `(projectId, ownerId)`. Save deletes and re-inserts a project's keyframes with its clips, like the other lanes. Project duplicate remaps owner ids with the clip ids it already remaps.

## 9. Editor
A `Keyframes` clip tool on overlay and text clips opens the panel: Add keyframe, Previous, Next, Copy here, Delete, and Linear or Ease for the selected keyframe. Diamonds draw on the selected clip in its lane; tap selects and seeks, drag moves with snapping to the playhead. The selected keyframe is local editor state.
