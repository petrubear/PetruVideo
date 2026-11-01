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
            if (handle.kind == EffectKind.Blur || handle.kind == EffectKind.Pixelate) {
                program.setFloatUniform("uAspect", width.toFloat() / height)
            }
            if (handle.kind == EffectKind.Shake) {
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

        fun fragmentFor(kind: EffectKind): String = HEADER + when (kind) {
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
        }
    }
}
