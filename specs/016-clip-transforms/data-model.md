# Data Model: Clip Transforms

## ClipTransform
| Field | Type | Rule |
|---|---|---|
| quarterTurns | Int | 0..3, clockwise |
| flipH | Boolean | mirrored left to right after the turn |
| flipV | Boolean | mirrored top to bottom after the turn |
| crop | CropBox | default the whole picture |

## CropBox
`left, top, right, bottom` in permille of the turned picture; `0 <= left < right <= 1000`, `0 <= top < bottom <= 1000`, each side at least 100. `isFull` when 0, 0, 1000, 1000.

## VideoClip (extended)
`transform: ClipTransform = ClipTransform()`. `isNeutral` when no turn, no flip, full crop.

## Persistence
Room v13: `video_clip.xfTurns`, `xfFlipH`, `xfFlipV`, `xfCropLeft`, `xfCropTop`, `xfCropRight`, `xfCropBottom`. Auto migration 12 to 13 with the defaults above.

## Invariants
Values in range; the crop box is valid; overlay, text, and audio clips have no transform.
