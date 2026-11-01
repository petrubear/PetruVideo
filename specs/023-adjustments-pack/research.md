# Research: Adjustments Pack

- **Decision**: One extra shader pass for all seven. **Why**: they are non-linear or need neighbours, which a colour matrix cannot do.
- **Decision**: Exposure in approximate linear light via pow 2.2. **Alternative**: multiply in gamma space; rejected, it shifts hue.
- **Decision**: Highlights and Shadows by luminance masks `smoothstep`. **Alternative**: tone curves; out of scope.
- **Decision**: Hue rotation in YIQ space. **Why**: cheap, keeps luma roughly constant.
- **Decision**: Sharpen as unsharp mask over 4 neighbours. **Why**: one pass, no extra texture.
