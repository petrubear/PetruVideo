# Research: Overlay Masks

- **Finding**: Media3 1.11.1 `OverlaySettings` has alpha, scale, rotation, anchors, but no blend mode, and `DefaultVideoCompositor` blends with a fixed function. Blend modes would need a custom `VideoCompositor`. Deferred.
- **Decision**: Masks in a per-input shader step (alpha multiply). Works with the existing compositor alpha blend, proven by chroma key (022).
- **Decision**: Shapes centred and fixed; no gestures. Keeps the feature small and the panel simple.
