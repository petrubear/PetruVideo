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
        GlProgram(EffectShaderSources.VERTEX, EffectShaderSources.fragmentFor(handle.kind))
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
            if (handle.kind in EffectShaderSources.USES_ASPECT) {
                program.setFloatUniform("uAspect", width.toFloat() / height)
            }
            if (handle.kind in EffectShaderSources.USES_TIME) {
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
    }
}
