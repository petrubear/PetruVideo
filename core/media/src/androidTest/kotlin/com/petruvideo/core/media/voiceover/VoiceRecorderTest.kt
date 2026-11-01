package com.petruvideo.core.media.voiceover

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.petruvideo.core.media.preview.Media3PreviewEngine
import com.petruvideo.core.media.pressure.PreviewQuality
import com.petruvideo.core.model.AudioClip
import com.petruvideo.core.model.AudioOrigin
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.VOICE_OVER_BITRATE
import com.petruvideo.core.model.VoiceOverLimit
import io.kotest.matchers.shouldBe
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Test

/**
 * The voice-over file format (feature 013): AAC in an ADTS stream, as the recorder writes it. A take is made here from
 * a sine wave with the same settings, because the microphone cannot be opened on the emulator (it crashes it).
 */
@OptIn(UnstableApi::class)
class VoiceRecorderTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val store = VoiceOverStore(context)
    private val made = mutableListOf<File>()

    @Before
    fun setUp() {
        store.closeTake()
    }

    @After
    fun tearDown() {
        made.forEach { it.delete() }
        store.closeTake()
    }

    private fun isEmulator() = Build.FINGERPRINT.contains("generic") || Build.HARDWARE.contains("ranchu") ||
        Build.HARDWARE.contains("goldfish")

    /** A take of [ms] milliseconds: a sine wave encoded to AAC at 44.1 kHz, mono, 96 kbps, framed as ADTS. */
    private fun syntheticTake(ms: Long): File {
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, SAMPLE_RATE, 1).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, VOICE_OVER_BITRATE)
        }
        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()
        val out = ByteArrayOutputStream()
        val info = MediaCodec.BufferInfo()
        val totalSamples = (SAMPLE_RATE * ms / 1000).toInt()
        var fed = 0
        var inputDone = false
        var outputDone = false
        while (!outputDone) {
            if (!inputDone) {
                val index = codec.dequeueInputBuffer(TIMEOUT_US)
                if (index >= 0) {
                    val buffer = codec.getInputBuffer(index)!!
                    val samples = minOf(buffer.capacity() / 2, totalSamples - fed, FRAME_SAMPLES * 2)
                    buffer.clear()
                    for (i in 0 until samples) {
                        buffer.putShort((sin(2 * PI * 440 * (fed + i) / SAMPLE_RATE) * 12_000).toInt().toShort())
                    }
                    fed += samples
                    inputDone = fed >= totalSamples
                    val flags = if (inputDone) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                    codec.queueInputBuffer(index, 0, samples * 2, fed * 1_000_000L / SAMPLE_RATE, flags)
                }
            }
            val outIndex = codec.dequeueOutputBuffer(info, TIMEOUT_US)
            if (outIndex >= 0) {
                val buffer = codec.getOutputBuffer(outIndex)!!
                if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0 && info.size > 0) {
                    val frame = ByteArray(info.size)
                    buffer.position(info.offset)
                    buffer.get(frame)
                    out.write(adtsHeader(frame.size))
                    out.write(frame)
                }
                outputDone = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                codec.releaseOutputBuffer(outIndex, false)
            }
        }
        codec.stop()
        codec.release()
        return store.newFile().also {
            it.writeBytes(out.toByteArray())
            made += it
        }
    }

    private fun adtsHeader(payload: Int): ByteArray {
        val length = payload + ADTS_HEADER
        return byteArrayOf(
            0xFF.toByte(), 0xF1.toByte(),
            ((1 shl 6) or (FREQ_INDEX_44100 shl 2)).toByte(),
            ((1 shl 6) or (length shr 11)).toByte(),
            ((length shr 3) and 0xFF).toByte(),
            (((length and 7) shl 5) or 0x1F).toByte(),
            0xFC.toByte(),
        )
    }

    private fun playToEnd(file: File, ms: Long) {
        val item = store.mediaItemFor(file, ms)
        val timeline = Timeline(
            "p", media = mapOf(item.id to item),
            audioLane = listOf(AudioClip("a", item.id, 0, ms, 0, false, AudioOrigin.Added)),
        )
        lateinit var engine: Media3PreviewEngine
        instrumentation.runOnMainSync { engine = Media3PreviewEngine(context) }
        try {
            instrumentation.runOnMainSync { engine.load(timeline, PreviewQuality.Full) }
            instrumentation.runOnMainSync { engine.play() }
            val end = System.currentTimeMillis() + PLAY_TIMEOUT_MS
            while (System.currentTimeMillis() < end && !engine.state.value.ended) Thread.sleep(50)
            check(engine.state.value.ended) { "state ${engine.state.value}" }
            engine.state.value.error shouldBe null
        } finally {
            instrumentation.runOnMainSync { engine.release() }
        }
    }

    @Test
    fun theLengthEstimatedFromTheFileSizeMatchesTheRecordedLength() {
        val file = syntheticTake(2_000)
        val estimate = VoiceOverLimit.durationOfBytes(file.length())
        check(abs(estimate - 2_000) < 250) { "file says $estimate ms for a 2000 ms take (${file.length()} bytes)" }
    }

    @Test
    fun aTakePlaysAsAnAudioClipToItsEnd() {
        playToEnd(syntheticTake(1_500), 1_500)
    }

    @Test
    fun aTakeCutShortInTheMiddleOfAFrameStillPlaysItsRecordedPart() {
        val file = syntheticTake(2_000)
        val cut = (file.length() * 0.6).toLong() + 37
        java.io.RandomAccessFile(file, "rw").use { it.setLength(cut) }
        playToEnd(file, VoiceOverLimit.durationOfBytes(cut))
    }

    @Test
    fun theJournalOfAnOpenTakeIsReadBackAndCleared() {
        val file = store.newFile().also { made += it; it.writeBytes(ByteArray(10)) }
        store.openTake("p1", 2_000, file)
        val pending = store.pendingTake()
        pending?.projectId shouldBe "p1"
        pending?.startMs shouldBe 2_000L
        pending?.file?.name shouldBe file.name
        store.closeTake()
        store.pendingTake() shouldBe null
    }

    @Test
    fun theSweepKeepsUsedAndOpenTakeFilesAndFreshOnesAndRemovesTheRest() {
        val used = store.newFile().also { made += it; it.writeBytes(ByteArray(10)) }
        val orphan = store.newFile().also { made += it; it.writeBytes(ByteArray(10)) }
        val open = store.newFile().also { made += it; it.writeBytes(ByteArray(10)) }
        val fresh = store.newFile().also { made += it; it.writeBytes(ByteArray(10)) }
        listOf(used, orphan, open).forEach { it.setLastModified(System.currentTimeMillis() - 5 * 60_000) }
        store.openTake("p", 0, open)
        store.sweep(setOf(android.net.Uri.fromFile(used).toString()))
        used.exists() shouldBe true
        open.exists() shouldBe true
        fresh.exists() shouldBe true
        orphan.exists() shouldBe false
        store.closeTake()
    }

    @Test
    fun theRealMicrophoneRecordsATake() {
        assumeFalse("the microphone crashes the emulator", isEmulator())
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, "android.permission.RECORD_AUDIO")
        val recorder = MediaVoiceRecorder(context)
        val file = store.newFile().also { made += it }
        var started = false
        instrumentation.runOnMainSync { started = recorder.start(file) }
        check(started) { "the microphone could not be opened" }
        Thread.sleep(1_500)
        var time = 0L
        instrumentation.runOnMainSync { time = recorder.stop() }
        check(time in 1_400..2_200) { "measured $time ms" }
        check(file.length() > 0) { "empty file" }
    }

    private companion object {
        const val SAMPLE_RATE = 44_100
        const val FRAME_SAMPLES = 1_024
        const val TIMEOUT_US = 10_000L
        const val ADTS_HEADER = 7
        const val FREQ_INDEX_44100 = 4
        const val PLAY_TIMEOUT_MS = 20_000L
    }
}
