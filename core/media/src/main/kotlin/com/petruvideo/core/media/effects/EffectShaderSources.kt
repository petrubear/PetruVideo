package com.petruvideo.core.media.effects

import com.petruvideo.core.model.EffectKind

/**
 * The shader source of every picture effect. [USES_ASPECT] and [USES_TIME] list the kinds whose shader reads that
 * uniform: a uniform the shader does not use is optimized away and cannot be set.
 */
internal object EffectShaderSources {
    val USES_ASPECT = setOf(
        EffectKind.Blur, EffectKind.Pixelate, EffectKind.Glow, EffectKind.Grain, EffectKind.Fisheye,
        EffectKind.Cartoon,
    )
    val USES_TIME = setOf(
        EffectKind.Shake, EffectKind.Glitch, EffectKind.Vhs, EffectKind.Grain, EffectKind.ZoomPulse,
        EffectKind.Flash, EffectKind.LightLeak, EffectKind.Ripple, EffectKind.Strobe, EffectKind.OldFilm,
    )

    const val VERTEX = """
        attribute vec4 aFramePosition;
        varying vec2 vUv;
        void main() {
          gl_Position = aFramePosition;
          vUv = aFramePosition.xy * 0.5 + 0.5;
        }
    """

    const val HEADER = """
        precision highp float;
        uniform sampler2D uTexSampler;
        uniform float uStrength;
        uniform float uAspect;
        uniform float uTime;
        varying vec2 vUv;
    """

    fun fragmentFor(kind: EffectKind): String = HEADER + bodyFor(kind)

    fun bodyFor(kind: EffectKind): String = when (kind) {
        EffectKind.Blur -> """
            void main() {
              float r = uStrength * 0.02;
              vec4 sum = vec4(0.0);
              for (int i = -2; i <= 2; i++) {
                for (int j = -2; j <= 2; j++) {
                  sum += texture2D(uTexSampler, clamp(vUv + vec2(float(i) * r / uAspect, float(j) * r), 0.0, 1.0));
                }
              }
              gl_FragColor = sum / 25.0;
            }
        """
        EffectKind.Pixelate -> """
            void main() {
              float blocks = mix(320.0, 14.0, uStrength);
              vec2 grid = vec2(blocks, blocks / uAspect);
              gl_FragColor = texture2D(uTexSampler, (floor(vUv * grid) + 0.5) / grid);
            }
        """
        EffectKind.Mirror -> """
            void main() {
              vec4 plain = texture2D(uTexSampler, vUv);
              vec4 mirrored = texture2D(uTexSampler, vec2(vUv.x < 0.5 ? vUv.x : 1.0 - vUv.x, vUv.y));
              gl_FragColor = mix(plain, mirrored, uStrength);
            }
        """
        EffectKind.Shake -> """
            void main() {
              float amp = uStrength * 0.04;
              vec2 offset = vec2(sin(uTime * 47.0), cos(uTime * 61.0)) * amp;
              gl_FragColor = texture2D(uTexSampler, clamp(vUv + offset, 0.0, 1.0));
            }
        """
        else -> extraBodyFor(kind)
    }

    fun extraBodyFor(kind: EffectKind): String = when (kind) {
        EffectKind.Glitch -> """
            float hash(float n) { return fract(sin(n * 12.9898) * 43758.5453); }
            void main() {
              float band = floor(vUv.y * 24.0);
              float r = hash(band + floor(uTime * 10.0) * 7.0);
              float hit = step(1.0 - 0.35 * uStrength, r);
              float shift = (hash(band * 3.0 + floor(uTime * 10.0)) - 0.5) * 0.25 * uStrength * hit;
              vec2 uv = vec2(clamp(vUv.x + shift, 0.0, 1.0), vUv.y);
              float split = 0.01 * uStrength * hit;
              gl_FragColor = vec4(
                texture2D(uTexSampler, vec2(clamp(uv.x + split, 0.0, 1.0), uv.y)).r,
                texture2D(uTexSampler, uv).g,
                texture2D(uTexSampler, vec2(clamp(uv.x - split, 0.0, 1.0), uv.y)).b,
                1.0);
            }
        """
        EffectKind.RgbSplit -> """
            void main() {
              float off = uStrength * 0.02;
              gl_FragColor = vec4(
                texture2D(uTexSampler, clamp(vUv + vec2(off, 0.0), 0.0, 1.0)).r,
                texture2D(uTexSampler, vUv).g,
                texture2D(uTexSampler, clamp(vUv - vec2(off, 0.0), 0.0, 1.0)).b,
                1.0);
            }
        """
        EffectKind.Glow -> """
            void main() {
              vec3 base = texture2D(uTexSampler, vUv).rgb;
              float r = 0.012 + uStrength * 0.02;
              vec3 sum = vec3(0.0);
              for (int i = -2; i <= 2; i++) {
                for (int j = -2; j <= 2; j++) {
                  vec3 s = texture2D(uTexSampler, clamp(vUv + vec2(float(i) * r / uAspect, float(j) * r), 0.0, 1.0)).rgb;
                  sum += max(s - 0.55, 0.0);
                }
              }
              vec3 glow = sum / 25.0 * 2.2;
              gl_FragColor = vec4(1.0 - (1.0 - base) * (1.0 - glow * uStrength * 1.6), 1.0);
            }
        """
        else -> lastBodyFor(kind)
    }

    fun lastBodyFor(kind: EffectKind): String = when (kind) {
        EffectKind.Vhs -> """
            float hash(float n) { return fract(sin(n * 12.9898) * 43758.5453); }
            void main() {
              float line = floor(vUv.y * 240.0);
              float jitter = (hash(line + floor(uTime * 30.0)) - 0.5) * 0.012 * uStrength;
              vec2 uv = vec2(clamp(vUv.x + jitter, 0.0, 1.0), vUv.y);
              float bleed = 0.006 * uStrength;
              vec3 c = vec3(
                texture2D(uTexSampler, vec2(clamp(uv.x + bleed, 0.0, 1.0), uv.y)).r,
                texture2D(uTexSampler, uv).g,
                texture2D(uTexSampler, vec2(clamp(uv.x - bleed, 0.0, 1.0), uv.y)).b);
              float grey = dot(c, vec3(0.299, 0.587, 0.114));
              c = mix(c, vec3(grey), 0.25 * uStrength);
              c *= 1.0 - 0.18 * uStrength * (0.5 + 0.5 * sin(vUv.y * 240.0 * 3.14159));
              gl_FragColor = vec4(c, 1.0);
            }
        """
        EffectKind.Grain -> """
            float hash(vec2 p) { return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453); }
            void main() {
              vec3 c = texture2D(uTexSampler, vUv).rgb;
              vec2 cell = floor(vec2(vUv.x * uAspect, vUv.y) * 400.0);
              float n = hash(cell + floor(uTime * 24.0) * 17.0) - 0.5;
              gl_FragColor = vec4(clamp(c + n * 0.5 * uStrength, 0.0, 1.0), 1.0);
            }
        """
        else -> addedBodyFor(kind)
    }

    // The effects of feature 035. Every one returns the input untouched at strength 0, and the two that flash keep
    // their pace fixed (Flash once a second, Strobe 2.5 times a second) so the rate never rises with the strength.
    fun addedBodyFor(kind: EffectKind): String = when (kind) {
        EffectKind.ZoomPulse -> """
            void main() {
              float beat = pow(max(sin(uTime * 12.5664), 0.0), 3.0);
              float zoom = 1.0 + uStrength * 0.15 * beat;
              gl_FragColor = texture2D(uTexSampler, (vUv - 0.5) / zoom + 0.5);
            }
        """
        EffectKind.Flash -> """
            void main() {
              vec3 c = texture2D(uTexSampler, vUv).rgb;
              float burst = uStrength * exp(-7.0 * fract(uTime));
              gl_FragColor = vec4(mix(c, vec3(1.0), burst), 1.0);
            }
        """
        EffectKind.LightLeak -> """
            void main() {
              vec3 c = texture2D(uTexSampler, vUv).rgb;
              vec2 center = vec2(0.5 + 0.45 * sin(uTime * 0.6), 0.5 + 0.3 * cos(uTime * 0.45));
              float spread = 1.0 - smoothstep(0.0, 0.65, distance(vUv, center));
              float swell = 0.55 + 0.45 * sin(uTime * 1.1);
              vec3 leak = vec3(1.0, 0.45, 0.18) * spread * swell * uStrength * 1.1;
              gl_FragColor = vec4(1.0 - (1.0 - c) * (1.0 - leak), 1.0);
            }
        """
        EffectKind.Letterbox -> """
            void main() {
              float bar = uStrength * 0.15;
              vec3 c = texture2D(uTexSampler, vUv).rgb;
              float inside = step(bar, vUv.y) * step(vUv.y, 1.0 - bar);
              gl_FragColor = vec4(c * inside, 1.0);
            }
        """
        else -> moreBodyFor(kind)
    }

    fun moreBodyFor(kind: EffectKind): String = when (kind) {
        EffectKind.ZoomBlur -> """
            void main() {
              float amount = uStrength * 0.12;
              vec2 dir = vUv - 0.5;
              vec3 sum = vec3(0.0);
              for (int i = 0; i < 8; i++) {
                float k = 1.0 - amount * float(i) / 7.0;
                sum += texture2D(uTexSampler, 0.5 + dir * k).rgb;
              }
              gl_FragColor = vec4(sum / 8.0, 1.0);
            }
        """
        EffectKind.Fisheye -> """
            void main() {
              vec2 p = vUv - 0.5;
              p.x *= uAspect;
              float bulge = 1.0 - uStrength * 0.8 * dot(p, p);
              vec2 q = p * bulge;
              q.x /= uAspect;
              gl_FragColor = texture2D(uTexSampler, clamp(q + 0.5, 0.0, 1.0));
            }
        """
        EffectKind.Ripple -> """
            void main() {
              float amp = uStrength * 0.02;
              vec2 offset = vec2(sin(vUv.y * 28.0 + uTime * 5.0), cos(vUv.x * 28.0 + uTime * 4.0)) * amp;
              gl_FragColor = texture2D(uTexSampler, clamp(vUv + offset, 0.0, 1.0));
            }
        """
        EffectKind.Strobe -> """
            void main() {
              vec3 c = texture2D(uTexSampler, vUv).rgb;
              float lit = step(fract(uTime / 0.4), 0.6);
              gl_FragColor = vec4(c * mix(1.0, mix(0.1, 1.0, lit), uStrength), 1.0);
            }
        """
        else -> filmBodyFor(kind)
    }

    fun filmBodyFor(kind: EffectKind): String = when (kind) {
        EffectKind.OldFilm -> """
            float hash(float n) { return fract(sin(n * 12.9898) * 43758.5453); }
            void main() {
              vec3 c = texture2D(uTexSampler, vUv).rgb;
              float grey = dot(c, vec3(0.299, 0.587, 0.114));
              vec3 film = mix(c, vec3(grey * 1.12, grey * 0.96, grey * 0.78), 0.75);
              float tick = floor(uTime * 12.0);
              film *= 1.0 + (hash(tick) - 0.5) * 0.2;
              float line = step(abs(vUv.x - hash(tick * 3.7)), 0.0012) * step(0.55, hash(tick * 1.3));
              film += line * 0.35;
              float edge = distance(vUv, vec2(0.5));
              film *= 1.0 - 0.9 * edge * edge;
              gl_FragColor = vec4(mix(c, clamp(film, 0.0, 1.0), uStrength), 1.0);
            }
        """
        EffectKind.Cartoon -> """
            float luma(vec3 c) { return dot(c, vec3(0.299, 0.587, 0.114)); }
            float at(vec2 offset) { return luma(texture2D(uTexSampler, clamp(vUv + offset, 0.0, 1.0)).rgb); }
            void main() {
              vec3 c = texture2D(uTexSampler, vUv).rgb;
              vec2 d = vec2(0.003 / uAspect, 0.003);
              float gx = at(vec2(d.x, d.y)) + 2.0 * at(vec2(d.x, 0.0)) + at(vec2(d.x, -d.y))
                - at(vec2(-d.x, d.y)) - 2.0 * at(vec2(-d.x, 0.0)) - at(vec2(-d.x, -d.y));
              float gy = at(vec2(-d.x, d.y)) + 2.0 * at(vec2(0.0, d.y)) + at(vec2(d.x, d.y))
                - at(vec2(-d.x, -d.y)) - 2.0 * at(vec2(0.0, -d.y)) - at(vec2(d.x, -d.y));
              float outline = clamp(length(vec2(gx, gy)) * 3.0, 0.0, 1.0);
              vec3 poster = floor(c * 5.0 + 0.5) / 5.0;
              vec3 toon = mix(poster, vec3(0.05), outline);
              gl_FragColor = vec4(mix(c, toon, uStrength), 1.0);
            }
        """
        else -> error("not an effect of this group: $kind")
    }
}
