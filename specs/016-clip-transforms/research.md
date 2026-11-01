# Research: Clip Transforms

## 1. Effect order
- **Decision**: per clip, `ScaleAndRotateTransformation` (rotation and flips) then `Crop`, then the existing fit (`Presentation`), then filter, picture effect, and keyframe framing. The builder puts the two new effects in front of the frame effects of the clip.
- **Why**: matches the spec order, reuses built-in effects, and leaves the fit and framing code untouched.
- **To confirm in the first task**: that `ScaleAndRotateTransformation` reports the swapped output size for 90 and 270 degrees, and that `Crop` takes NDC (-1..1) edges, flipped vertically from our top-left permille box. The export frame tests settle both.

## 2. Crop box model
- **Decision**: `CropBox(left, top, right, bottom)` in permille of the turned picture, default `0, 0, 1000, 1000` meaning no crop. Minimum 100 permille wide and tall (10%).
- **Preset shapes** (1:1, 4:5, 9:16, 16:9, 4:3) are not stored; a preset only places a box of that pixel shape, centered and as large as fits the turned picture, and resizing keeps the shape. The stored value is just the box.
- **Rotation of a cropped clip**: rotating turns the picture with its box, so the box is turned too; the helper `ClipTransform.rotated()` rotates the box permille corners (x, y) -> (1000 - y, x).
- **Flip of a cropped clip**: the box is mirrored with the picture (left, right swapped around 1000, or top, bottom for a vertical flip).

## 3. Turned size and the shown shape
`ClipTransform.turnedSize(width, height)` gives the picture size after the quarter turns (swapped for odd turns), and `croppedSize` the size of the kept part. The editor uses them to draw the box and keep a preset's shape in pixels; the fit effect does the rest.

## 4. Edit rules
- Rotate adds one quarter turn clockwise (mod 4) and turns the crop box with it; Flip toggles an axis and mirrors the box; SetCrop validates the box (inside, minimum size); Reset restores `ClipTransform()`.
- Each is one undo step. Split, duplicate, trim, move, speed copy the whole clip, so the transform follows. Relink to a media item of a different shape (width over height differs by more than 1%) resets only the crop.

## 5. Persistence
Room 12 to 13 auto migration: seven columns on `video_clip` with defaults (0, 0, 0, 0, 0, 1000, 1000). Project duplicate and relink copy the clip. Templates fill clips with no transform.

## 6. Preview
Each committed change reloads the composition (position and play state kept), as for filters and keyframes. While the crop screen is open it shows a still of the clip's first visible frame, drawn with the same rotation and flips as the Compose image transform; the preview plays again after Done. No live crop in the player during the drag.

## 7. Editor
A `Transform` clip tool on main-track clips opens a panel with Rotate, Flip horizontal, Flip vertical, Crop, and Reset (disabled when nothing is set). Crop opens a full-height screen with the still, a box with eight handles (48 dp touch targets), preset chips (Free, 1:1, 4:5, 9:16, 16:9, 4:3), Full, and Done and Cancel. The still comes from the existing thumbnail loader at high resolution.
