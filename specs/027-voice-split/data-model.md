# Data Model: Voice Split

No new tables. The two result files are ordinary `MediaItem`s (kind Audio, `file://` URI in `filesDir/voicesplit/`) referenced by two `AudioClip`s. A side index keyed by fingerprint and range finds an earlier result.
