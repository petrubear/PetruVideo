# Data Model: Reverse and Freeze Frame

## VideoClip (extended)
`reversed: Boolean = false`. A reversed clip plays its trimmed source range backwards, picture and attached sound.

## Freeze clip
An ordinary `VideoClip` whose media item is a photo (`MediaKind.Photo`): `sourceStartMs = 0`, `sourceEndMs = length`, `attachedAudio = false`. Length 500..10 000 ms at insertion; trimmable like any photo clip.

## Reversed copy (not in the model)
File under `filesDir/reversed/` named by media fingerprint, source start, and source end; holds the picture and sound of that range backwards. Found by `ReverseStore.fileFor(media, startMs, endMs)`.

## Persistence
Room v14: `video_clip.reversed` default 0. Freeze stills are files under `filesDir/freezes/` referenced by their media items.

## Invariants
Only video-kind clips can be reversed; a reversed clip is at most 30 s as trimmed when the flag is set; a freeze still has no attached sound.
