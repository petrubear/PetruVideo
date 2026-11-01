package com.petruvideo.core.media.filter

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
import com.petruvideo.core.model.ClipFilter
import kotlin.math.PI

/**
 * Exposure, gamma, highlights, shadows, tint, hue and sharpen of one clip as one shader pass (feature 023). The values
 * are read at every frame, so a slider drag needs no rebuild; with all seven at 0 the pass copies the picture.
 */
@OptIn(UnstableApi::class)
class ClipAdjustEffect(filter: ClipFilter) {
    @Volatile private var values: FloatArray = pack(filter)

    fun update(filter: ClipFilter) {
        values = pack(filter)
    }

    val glEffect: GlEffect = GlEffect { _: Context, _: Boolean -> AdjustShader(this) }

    internal val current: FloatArray get() = values

    private fun pack(f: ClipFilter) = floatArrayOf(
        f.exposure / PERCENT * MAX_STOPS,
        f.gamma / PERCENT,
        f.highlights / PERCENT,
        f.shadows / PERCENT,
        f.tint / PERCENT,
        f.hue / PERCENT * PI.toFloat(),
        f.sharpen / PERCENT,
    )

    private companion object {
        const val PERCENT = 100f
        const val MAX_STOPS = 2f
    }
}

@OptIn(UnstableApi::class)
private class AdjustShader(private val handle: ClipAdjustEffect) : BaseGlShaderProgram(false, 1) {
    private val program: GlProgram = try {
        GlProgram(VERTEX, FRAGMENT)
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
            val v = handle.current
            program.use()
            program.setSamplerTexIdUniform("uTexSampler", inputTexId, 0)
            program.setFloatUniform("uExposure", v[0])
            program.setFloatUniform("uGamma", v[1])
            program.setFloatUniform("uHighlights", v[2])
            program.setFloatUniform("uShadows", v[3])
            program.setFloatUniform("uTint", v[4])
            program.setFloatUniform("uHue", v[5])
            program.setFloatUniform("uSharpen", v[6])
            program.setFloatUniform("uActive", if (v.any { it != 0f }) 1f else 0f)
            program.setFloatsUniform("uTexel", floatArrayOf(1f / width, 1f / height))
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
        const val VERTEX_COUNT = 4

        const val VERTEX = """
            attribute vec4 aFramePosition;
            varying vec2 vUv;
            void main() {
              gl_Position = aFramePosition;
              vUv = aFramePosition.xy * 0.5 + 0.5;
            }
        """

        const val FRAGMENT = """
            precision highp float;
            uniform sampler2D uTexSampler;
            uniform float uExposure;
            uniform float uGamma;
            uniform float uHighlights;
            uniform float uShadows;
            uniform float uTint;
            uniform float uHue;
            uniform float uSharpen;
            uniform float uActive;
            uniform vec2 uTexel;
            varying vec2 vUv;

            void main() {
              vec4 px = texture2D(uTexSampler, vUv);
              if (uActive < 0.5) {
                gl_FragColor = px;
                return;
              }
              vec3 c = px.rgb;
              vec3 near = (texture2D(uTexSampler, vUv + vec2(uTexel.x, 0.0)).rgb
                + texture2D(uTexSampler, vUv - vec2(uTexel.x, 0.0)).rgb
                + texture2D(uTexSampler, vUv + vec2(0.0, uTexel.y)).rgb
                + texture2D(uTexSampler, vUv - vec2(0.0, uTexel.y)).rgb) * 0.25;
              c += (c - near) * uSharpen * 2.0;
              c = max(c, vec3(0.0));
              c = pow(pow(c, vec3(2.2)) * exp2(uExposure), vec3(1.0 / 2.2));
              c = pow(c, vec3(exp2(-uGamma)));
              float luma = dot(c, vec3(0.2126, 0.7152, 0.0722));
              float low = 1.0 - smoothstep(0.0, 0.6, luma);
              float high = smoothstep(0.4, 1.0, luma);
              c += uShadows * 0.35 * low * (uShadows > 0.0 ? (1.0 - c) : c);
              c += uHighlights * 0.35 * high * (uHighlights > 0.0 ? (1.0 - c) : c);
              c += vec3(uTint * 0.05, -uTint * 0.1, uTint * 0.05);
              if (uHue != 0.0) {
                float cs = cos(uHue);
                float sn = sin(uHue);
                mat3 toYiq = mat3(0.299, 0.596, 0.211, 0.587, -0.274, -0.523, 0.114, -0.322, 0.312);
                mat3 toRgb = mat3(1.0, 1.0, 1.0, 0.956, -0.272, -1.106, 0.621, -0.647, 1.703);
                vec3 yiq = toYiq * c;
                yiq.yz = vec2(yiq.y * cs - yiq.z * sn, yiq.y * sn + yiq.z * cs);
                c = toRgb * yiq;
              }
              gl_FragColor = vec4(clamp(c, 0.0, 1.0), px.a);
            }
        """
    }
}
