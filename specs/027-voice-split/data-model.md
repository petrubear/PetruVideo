# Data Model: Voice Split

No Room schema change. Layered audio needs none (audio clips already store their own start). The two result files are ordinary `MediaItem`s (kind Audio, `file://` URI in `filesDir/voicesplit/`) referenced by two `AudioClip`s.

**Audio rows** (derived, not stored): `AudioRows.of(lane)` gives each clip a row number by earliest-free-row assignment; the lane view and the builder both use it.

**Split result index** (derived from the files): `voicesplit/<fingerprintHash>-<startMs>-<endMs>-voice.wav` and `...-backing.wav`; present means reusable.
