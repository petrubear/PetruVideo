package com.petruvideo.core.media.effects

import android.content.Context
import android.opengl.GLES20
import androidx.annotation.OptIn
import androidx.media3.common.VideoFrameProcessingException
import androidx.media3.common.util.GlProgram
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BaseGlShaderProgram
import androidx.media3.effect.GlEffect
import com.petruvideo.core.model.ClipEffect
import com.petruvideo.core.model.EffectKind

/**
 * The picture effect of one clip (Blur, Pixelate, Mirror or Shake) as one shader pass. The strength is read at every
 * frame, so a slider drag needs no rebuild.
 */
@OptIn(UnstableApi::class)
class ClipEffectHandle(effect: ClipEffect) {
    val kind: EffectKind = effect.kind
    @Volatile private var strength: Float = effect.strength / PERCENT

    val strengthValue: Float get() = strength

    fun update(effect: ClipEffect) {
        strength = effect.strength / PERCENT
    }

    val glEffect: GlEffect = GlEffect { _: Context, _: Boolean -> EffectShader(this) }

    private companion object {
        const val PERCENT = 100f
    }
}

@OptIn(UnstableApi::class)
private class EffectShader(private val handle: ClipEffectHandle) : BaseGlShaderProgram(false, 1) {
    private val program: GlProgram = try {
        GlProgram(VERTEX, fragmentFor(handle.kind))
    } catch (e: GlUtil.GlException) {
        throw VideoFrameProcessingException(e)
    }
    private var width = 1
    private var height = 1

    override fun configure(inputWidth: Int, inputHeight: Int): Size {
        width = inputWidth
        height = inputHeight
        return Size(inputWidth, inputHeight)
    }

    override fun drawFrame(inputTexId: Int, presentationTimeUs: Long) {
        try {
            program.use()
            program.setSamplerTexIdUniform("uTexSampler", inputTexId, 0)
            program.setFloatUniform("uStrength", handle.strengthValue)
            // A uniform the shader does not use is optimized away and cannot be set.
            if (handle.kind in USES_ASPECT) {
                program.setFloatUniform("uAspect", width.toFloat() / height)
            }
            if (handle.kind in USES_TIME) {
                program.setFloatUniform("uTime", presentationTimeUs / MICROS_PER_SECOND)
            }
            program.setBufferAttribute(
                "aFramePosition", GlUtil.getNormalizedCoordinateBounds(), GlUtil.HOMOGENEOUS_COORDINATE_VECTOR_SIZE,
            )
            program.bindAttributesAndUniforms()
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VERTEX_COUNT)
        } catch (e: GlUtil.GlException) {
            throw VideoFrameProcessingException(e, presentationTimeUs)
        }
    }

    override fun release() {
        super.release()
        try {
            program.delete()
        } catch (e: GlUtil.GlException) {
            throw VideoFrameProcessingException(e)
        }
    }

    private companion object {
        val USES_ASPECT = setOf(EffectKind.Blur, EffectKind.Pixelate, EffectKind.Glow, EffectKind.Grain)
        val USES_TIME = setOf(EffectKind.Shake, EffectKind.Glitch, EffectKind.Vhs, EffectKind.Grain)
        const val MICROS_PER_SECOND = 1_000_000f
        const val VERTEX_COUNT = 4

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
            else -> error("not an effect of this group: $kind")
        }
    }
}
