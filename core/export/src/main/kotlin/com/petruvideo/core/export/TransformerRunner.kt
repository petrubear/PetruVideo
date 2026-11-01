package com.petruvideo.core.export

import android.content.Context
import android.media.MediaCodecInfo
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.AudioEncoderSettings
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import com.petruvideo.core.media.composition.BuildSpec
import com.petruvideo.core.media.composition.CompositionBuilder
import com.petruvideo.core.media.ducking.DuckingAnalyzer
import com.petruvideo.core.media.waveform.WaveformPeaksGenerator
import com.petruvideo.core.model.export.BitrateTable
import com.petruvideo.core.model.export.ExportCaps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Runs the export composition (the same one the preview plays, built by [CompositionBuilder]) through Media3's
 * Transformer: H.264 video at a constant bitrate and AAC audio. Must run where a Looper exists; it hops to the main
 * thread itself because the Transformer's callbacks and progress belong there.
 */
@OptIn(UnstableApi::class)
class TransformerRunner(private val context: Context) : ExportRunner {
    private val analyzer = DuckingAnalyzer(WaveformPeaksGenerator(context))

    override suspend fun run(request: ExportRequest, onProgress: (Int) -> Unit): RunResult =
        withContext(Dispatchers.Main.immediate) {
            val spec = BuildSpec.forExport(
                request.timeline,
                request.settings.resolution.shortSide,
                request.settings.frameRate.fps,
                ExportCaps.from(request.timeline).maxFrameRate.fps,
            )
            val ducking = withContext(Dispatchers.Default) { analyzer.envelopes(request.timeline) }
            val composition = CompositionBuilder(spec, duckEnvelopes = ducking).build(request.timeline)
                ?: return@withContext RunResult.Error("Nothing to export")
            render(composition, request, onProgress)
        }

    private suspend fun render(composition: Composition, request: ExportRequest, onProgress: (Int) -> Unit): RunResult =
        suspendCancellableCoroutine { continuation ->
            val handler = Handler(Looper.getMainLooper())
            val encoders = DefaultEncoderFactory.Builder(context)
                .setRequestedVideoEncoderSettings(
                    VideoEncoderSettings.Builder()
                        .setBitrate(request.videoBitrate)
                        .setBitrateMode(MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR)
                        .setiFrameIntervalSeconds(KEYFRAME_SECONDS)
                        .build(),
                )
                .setRequestedAudioEncoderSettings(
                    AudioEncoderSettings.Builder().setBitrate(BitrateTable.AUDIO_BITRATE).build(),
                )
                .setEnableFallback(false)
                .build()
            var polling = true
            lateinit var transformer: Transformer
            val listener = object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    polling = false
                    if (continuation.isActive) {
                        continuation.resume(RunResult.Done(exportResult.fileSizeBytes, exportResult.durationMs))
                    }
                }

                override fun onError(composition: Composition, exportResult: ExportResult, exception: ExportException) {
                    polling = false
                    android.util.Log.e("PetruExport", "export failed", exception)
                    if (continuation.isActive) continuation.resume(RunResult.Error(exception.errorCodeName))
                }
            }
            transformer = Transformer.Builder(context)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .setAudioMimeType(MimeTypes.AUDIO_AAC)
                .setEncoderFactory(encoders)
                .addListener(listener)
                .build()
            val holder = ProgressHolder()
            val poll = object : Runnable {
                override fun run() {
                    if (!polling) return
                    if (transformer.getProgress(holder) != Transformer.PROGRESS_STATE_NOT_STARTED) {
                        onProgress(holder.progress)
                    }
                    handler.postDelayed(this, POLL_MS)
                }
            }
            continuation.invokeOnCancellation {
                polling = false
                handler.post { transformer.cancel() }
            }
            transformer.start(composition, request.output.absolutePath)
            handler.post(poll)
        }

    private companion object {
        const val POLL_MS = 500L
        const val KEYFRAME_SECONDS = 2f
    }
}
