# UI Contract: Reverse and Freeze Frame

- **Freeze frame tool** (main-track clip, playhead inside it): a panel with a length slider (0.5 to 10 s, default 2 s) and an Insert button; Insert is one undo step.
- **Reverse tool**: a toggle in the clip tool bar (On/Off state in its label); turning it on shows a progress bar with a percentage and a Cancel button above the tool bar; turning it off is immediate.
- **Unavailable states** with a reason: photos, missing media, clips longer than 30 s, too little storage.
- **A note** "Preparing the reversed clip" on a reversed clip whose backwards copy is being rebuilt.
- **Accessibility**: progress has a live description ("Reversing, 40 percent"); controls meet 48 dp and contrast rules; works in portrait, unfolded (inspector section) and tabletop.
