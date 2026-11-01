# Data Model: Effects, Audio Tab, and Templates

## VideoClip (extended)
| Field | Type | Rule |
|---|---|---|
| transitionOut | Transition? | Applies to the cut after this clip. Null on the last clip. |
| effect | ClipEffect? | At most one. |

## Transition
| Field | Type | Rule |
|---|---|---|
| kind | Dissolve, FadeBlack, Slide, Wipe, Zoom | |
| lengthMs | Long | 200 to 2000, multiple of 100, at most half of the shorter neighbour (clamped, `Notice.TransitionLimited`) |

## ClipEffect
| Field | Type | Rule |
|---|---|---|
| kind | Blur, Pixelate, Mirror, Shake | |
| strength | Int | 0 to 100, default 50 |

## Derived timeline
`startMs(clip i) = sum of lengths of clips before i - sum of transition lengths before i`. `videoLengthMs` is the end of the last clip. Invariant: a clip with `transitionOut` has a next clip; a transition never exceeds half of either neighbour; lengths of two adjacent transitions on one clip fit inside it together.

## AudioClip (extended)
Unchanged. Music uses `AudioOrigin.Added` and `AddAudioClips`.

## Template (code only)
| Field | Type |
|---|---|
| id, name | String |
| slots | List of Slot(lengthMs) |
| look | FilterLook? |
| title | TemplateTitle(text, style) |
| transition | Transition? between slots |

## Persistence
Room v10: `video_clip.trn_kind`, `trn_length_ms`, `fxe_kind`, `fxe_strength` (nullable). Duplicate project copies them; split keeps the effect on both parts and the transition on the second; delete or move runs `TimelineInvariants.fixTransitions`.
