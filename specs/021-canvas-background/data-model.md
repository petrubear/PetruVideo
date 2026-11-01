# Data Model: Canvas Background

## CanvasBackground
Sealed: `Black`, `Colour(argb)`, `Blur(strength)` with `BlurStrength` Light, Medium, Strong. Default Black.

## Timeline (extended)
`canvas: CanvasBackground = CanvasBackground.Black`.

## Persistence
Room v18, `project.canvasKind TEXT NOT NULL DEFAULT 'Black'`, `canvasColor INTEGER NOT NULL DEFAULT -16777216`, `canvasBlur TEXT NOT NULL DEFAULT 'Medium'`.
