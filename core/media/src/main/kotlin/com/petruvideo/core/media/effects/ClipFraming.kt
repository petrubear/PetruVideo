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
import com.petruvideo.core.model.Pose
import com.petruvideo.core.model.VideoClip

/**
 * The zoom, pan, and fade of one video clip (feature 014) as one shader pass. The clip is read at every frame, so a
 * keyframe drag needs no rebuild. [timeMs] is the clip's own time on the timeline, which is what effects after the
 * speed change see.
 */
@OptIn(UnstableApi::class)
class ClipFramingHandle(clip: VideoClip) {
    @Volatile private var current: VideoClip = clip

    fun update(clip: VideoClip) {
        current = clip
    }

    fun poseAt(timeMs: Long): Pose = current.poseAt(timeMs)

    val glEffect: GlEffect = GlEffect { _: Context, _: Boolean -> FramingShader(this) }
}

@OptIn(UnstableApi::class)
private class FramingShader(private val handle: ClipFramingHandle) : BaseGlShaderProgram(false, 1) {
    private val program: GlProgram = try {
        GlProgram(VERTEX, FRAGMENT)
    } catch (e: GlUtil.GlException) {
        throw VideoFrameProcessingException(e)
    }

    override fun configure(inputWidth: Int, inputHeight: Int): Size = Size(inputWidth, inputHeight)

    override fun drawFrame(inputTexId: Int, presentationTimeUs: Long) {
        try {
            val pose = handle.poseAt(presentationTimeUs / MICROS_PER_MS)
            program.use()
            program.setSamplerTexIdUniform("uTexSampler", inputTexId, 0)
            program.setFloatUniform("uZoom", pose.sizePermille / PERMILLE)
            // Texture coordinates run up from the bottom; the pose runs down from the top.
            program.setFloatsUniform("uCenter", floatArrayOf(pose.xPermille / PERMILLE, 1f - pose.yPermille / PERMILLE))
            program.setFloatUniform("uOpacity", pose.opacityPercent / PERCENT)
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
        const val MICROS_PER_MS = 1_000L
        const val PERMILLE = 1_000f
        const val PERCENT = 100f
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
            uniform float uZoom;
            uniform vec2 uCenter;
            uniform float uOpacity;
            varying vec2 vUv;
            void main() {
              vec4 color = texture2D(uTexSampler, uCenter + (vUv - 0.5) / uZoom);
              gl_FragColor = vec4(color.rgb * uOpacity, color.a);
            }
        """
    }
}
