# UI Contract: Clip Transforms

- **Transform tool**: shown in the clip tool bar for a selected main-track clip. Opens a panel (phone sheet, unfolded inspector section, tabletop lower half) with Rotate, Flip horizontal, Flip vertical (a toggle each), Crop, and Reset.
- **Crop screen**: full screen over the editor; still of the clip, a box with eight handles (48 dp targets), chips Free, 1:1, 4:5, 9:16, 16:9, 4:3, a Full button, Cancel and Done. Works in portrait, unfolded (still beside the controls), and tabletop.
- **Feedback**: Rotate and Flip apply at once and the preview reloads; Done applies the crop; Cancel drops it; every action is one undo step.
- **Accessibility**: handles have labels ("Crop left edge" and so on) and arrow-button alternatives to dragging (nudge by 1%); contrast and touch-target rules as elsewhere.
