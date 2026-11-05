package com.petruvideo.core.export

import android.content.ContentUris
import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.petruvideo.core.data.repo.ExportRepository
import com.petruvideo.core.data.repo.StoredJob
import com.petruvideo.core.media.probe.MediaProbe
import com.petruvideo.core.model.AudioClip
import com.petruvideo.core.model.AudioFx
import com.petruvideo.core.model.Echo
import com.petruvideo.core.model.EqPreset
import com.petruvideo.core.model.EqPresets
import com.petruvideo.core.model.Fade
import com.petruvideo.core.model.NoiseReduction
import com.petruvideo.core.model.NoiseStrength
import com.petruvideo.core.model.Pitch
import com.petruvideo.core.model.Robot
import com.petruvideo.core.model.Bass
import com.petruvideo.core.model.VoiceFx
import com.petruvideo.core.model.AudioOrigin
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.ProjectFormat
import com.petruvideo.core.model.Reverb
import com.petruvideo.core.model.ReverbPreset
import com.petruvideo.core.model.ReverbPresets
import com.petruvideo.core.model.TextBackground
import com.petruvideo.core.model.TextClip
import com.petruvideo.core.model.TextStyle
import com.petruvideo.core.model.ClipFilter
import com.petruvideo.core.model.FilterLook
import com.petruvideo.core.model.OverlayClip
import com.petruvideo.core.model.ClipEffect
import com.petruvideo.core.model.EffectKind
import com.petruvideo.core.model.Transition
import com.petruvideo.core.model.TransitionKind
import com.petruvideo.core.model.Timeline
import com.petruvideo.core.model.VideoClip
import com.petruvideo.core.model.export.DeviceLimits
import com.petruvideo.core.model.export.ExportSettings
import com.petruvideo.core.model.export.ExportState
import com.petruvideo.core.model.export.FrameRate
import com.petruvideo.core.model.export.Quality
import com.petruvideo.core.model.export.Resolution
import io.kotest.matchers.shouldBe
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

/** Exports real projects on the device and checks the files; also the gallery saving and cancel cleanup. */
class ExportPipelineTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val cache = File(context.cacheDir, "export-test-${System.nanoTime()}")
    private val savedNames = mutableListOf<String>()

    private class Repo : ExportRepository {
        override suspend fun settings(projectId: String) = null
        override suspend fun saveSettings(projectId: String, settings: ExportSettings) = Unit
        override suspend fun job(projectId: String): StoredJob? = null
        override suspend fun saveJob(job: StoredJob) = Unit
        override suspend fun markInterrupted() = 0
        val records = mutableListOf<com.petruvideo.core.model.export.ExportRecord>()
        override suspend fun addRecord(record: com.petruvideo.core.model.export.ExportRecord) {
            records += record
        }
    }

    private val encoders = object : EncoderCapabilities {
        override val limits = DeviceLimits(2160, 60)
        override val bitrateRange = 500_000..40_000_000
    }

    @Before
    fun setUp() {
        cache.mkdirs()
    }

    @After
    fun tearDown() {
        scope.cancel()
        cache.deleteRecursively()
        savedNames.forEach { name ->
            context.contentResolver.delete(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "${MediaStore.Video.Media.DISPLAY_NAME} = ?", arrayOf(name),
            )
        }
    }

    private fun item(name: String): MediaItem {
        val out = File(context.cacheDir, name)
        instrumentation.context.assets.open(name).use { i -> out.outputStream().use { i.copyTo(it) } }
        return runBlocking { MediaProbe(context).probe(Uri.fromFile(out), name).getOrThrow() }
    }

    private fun project(vararg clips: VideoClip, media: List<MediaItem>, audio: List<AudioClip> = emptyList()) = Timeline(
        projectId = "p",
        format = ProjectFormat(320, 180, 30),
        media = media.associateBy { it.id },
        videoLane = clips.toList(),
        audioLane = audio,
    )

    private val repo = Repo()

    private val CLIP_LEVEL = 0.999

    private fun coordinator(saver: ExportSaver = GallerySaver(context)) =
        ExportCoordinator(TransformerRunner(context), saver, repo, encoders, { Long.MAX_VALUE }, cache, scope)

    private fun waitForEnd(c: ExportCoordinator, timeoutMs: Long = 120_000): ExportState {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            val state = c.job.value?.state
            if (state != null && !state.active) return state
            Thread.sleep(100)
        }
        error("export did not finish: ${c.job.value?.state}")
    }

    private fun retriever(uri: String) = MediaMetadataRetriever().apply { setDataSource(context, Uri.parse(uri)) }

    private fun MediaMetadataRetriever.int(key: Int) = extractMetadata(key)?.toInt() ?: 0

    private var lastTimes: List<Long> = emptyList()

    private fun videoSamples(uri: String): Int {
        val extractor = MediaExtractor().apply { setDataSource(context, Uri.parse(uri), null) }
        val track = (0 until extractor.trackCount).first {
            extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)!!.startsWith("video/")
        }
        extractor.selectTrack(track)
        var count = 0
        val times = mutableListOf<Long>()
        while (extractor.sampleTime >= 0) {
            count++
            if (times.size < 10) times += extractor.sampleTime / 1000
            extractor.advance()
        }
        lastTimes = times
        extractor.release()
        return count
    }

    private fun export(timeline: Timeline, settings: ExportSettings, name: String): Pair<ExportState, String?> {
        val saver = object : ExportSaver {
            val inner = GallerySaver(context)
            override suspend fun save(file: File, displayName: String): SavedVideo? =
                inner.save(file, displayName).also { savedNames += displayName }
        }
        val c = coordinator(saver)
        c.start("p", name, timeline, settings) shouldBe null
        val end = waitForEnd(c)
        return end to (end as? ExportState.Finished)?.uri
    }

    @Test
    fun aSpedUpClipAndAPortraitClipExportWithTheRightLengthSizeAndSound() {
        val a = item("video_with_audio.mp4")
        val p = item("video_portrait.mp4")
        val timeline = project(
            VideoClip("c1", a.id, 0, 2_000, true, speedMilli = 2000),
            VideoClip("c2", p.id, 0, 2_000, p.hasAudio),
            media = listOf(a, p),
        )
        val (state, uri) = export(timeline, ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller), "pipeline-a")
        state.shouldBeFinished()
        val r = retriever(uri!!)
        val duration = r.int(MediaMetadataRetriever.METADATA_KEY_DURATION)
        (duration in 2_930..3_100) shouldBe true
        val w = r.int(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
        val h = r.int(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
        minOf(w, h) shouldBe 720
        (maxOf(w, h) * 9 / 16 - 720 in -2..2) shouldBe true
        r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) shouldBe "yes"
        r.release()
    }

    @Test
    fun aMutedProjectExportsWithTheRightLength() {
        val a = item("video_with_audio.mp4")
        val timeline = project(VideoClip("c1", a.id, 0, 2_000, true, muted = true), media = listOf(a))
        val (state, uri) = export(timeline, ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller), "pipeline-mute")
        state.shouldBeFinished()
        val r = retriever(uri!!)
        (r.int(MediaMetadataRetriever.METADATA_KEY_DURATION) in 1_930..2_100) shouldBe true
        r.release()
    }

    @Test
    fun theFrameRateAndLengthMatchWhenTheRateEqualsTheFootage() {
        val a = item("video_with_audio.mp4")
        val timeline = project(VideoClip("c1", a.id, 0, 2_000, true), media = listOf(a))
        val (state, uri) = export(timeline, ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller), "pipeline-30")
        state.shouldBeFinished()
        val r = retriever(uri!!)
        (r.int(MediaMetadataRetriever.METADATA_KEY_DURATION) in 1_930..2_100) shouldBe true
        r.release()
        val samples = videoSamples(uri)
        check(samples in 56..64) { "video samples = $samples, first times (ms) = $lastTimes" }
    }

    @Test
    fun reverbMusicAndAnAudioLaneExportWithSound() {
        val a = item("video_silent.mp4")
        val music = item("audio.mp3")
        val hall = ReverbPresets.settings(ReverbPreset.Hall, true)
        val audio = AudioClip(
            "m1", music.id, 0, 2_000, timelineStartMs = 0, origin = AudioOrigin.Added, reverb = hall, volumePercent = 150,
        )
        val timeline = project(VideoClip("c1", a.id, 0, 2_000, false), media = listOf(a, music), audio = listOf(audio))
        val (state, uri) = export(timeline, ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller), "pipeline-rv")
        state.shouldBeFinished()
        val r = retriever(uri!!)
        r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) shouldBe "yes"
        r.release()
    }

    @Test
    fun theGalleryRowIsPublishedNotPendingAndLivesInThePetruVideoAlbum() {
        val a = item("video_with_audio.mp4")
        val timeline = project(VideoClip("c1", a.id, 0, 1_000, true), media = listOf(a))
        val (state, uri) = export(timeline, ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller), "pipeline-gallery")
        state.shouldBeFinished()
        val id = ContentUris.parseId(Uri.parse(uri))
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Video.Media.IS_PENDING, MediaStore.Video.Media.RELATIVE_PATH),
            "${MediaStore.Video.Media._ID} = ?", arrayOf(id.toString()), null,
        )!!.use {
            it.moveToFirst() shouldBe true
            it.getInt(0) shouldBe 0
            it.getString(1).startsWith("Movies/PetruVideo") shouldBe true
        }
        File(cache, "${(state as ExportState.Finished).uri.hashCode()}").exists() shouldBe false
        cache.listFiles().orEmpty().none { it.name.endsWith(".mp4") } shouldBe true
    }

    @Test
    fun aFinishedExportAddsOneHistoryRecordWithTheSavedVideo() {
        val a = item("video_with_audio.mp4")
        val timeline = project(VideoClip("c1", a.id, 0, 1_000, true), media = listOf(a))
        val (state, uri) = export(timeline, ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller), "pipeline-history")
        state.shouldBeFinished()
        val record = repo.records.single()
        record.uri shouldBe uri
        record.projectName shouldBe "pipeline-history"
        (record.bytes > 0 && record.durationMs in 900..1_200) shouldBe true
        record.settings.quality shouldBe Quality.Smaller
    }

    @Test
    fun aMissingFileFailsSavingWithoutLeavingAGalleryRow() {
        val saver = GallerySaver(context)
        val ghost = File(cache, "ghost.mp4")
        val name = "ghost-${System.nanoTime()}.mp4"
        val result = runBlocking { saver.save(ghost, name) }
        result shouldBe null
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Video.Media._ID),
            "${MediaStore.Video.Media.DISPLAY_NAME} = ?", arrayOf(name), null,
        )!!.use { it.count shouldBe 0 }
    }

    @Test
    fun cancelStopsWithinTwoSecondsAndLeavesNothingBehind() {
        val a = item("video_with_audio.mp4")
        val clips = (0 until 6).map { VideoClip("c$it", a.id, 0, 2_000, true) }
        val timeline = project(*clips.toTypedArray(), media = listOf(a))
        val name = "cancel-test"
        val c = coordinator(object : ExportSaver {
            override suspend fun save(file: File, displayName: String): SavedVideo? {
                savedNames += displayName
                return GallerySaver(context).save(file, displayName)
            }
        })
        c.start("p", name, timeline, ExportSettings(Resolution.R1080, FrameRate.F30, Quality.Higher)) shouldBe null
        val begin = System.currentTimeMillis()
        while (c.job.value?.state !is ExportState.Rendering && System.currentTimeMillis() - begin < 30_000) Thread.sleep(50)
        val cancelAt = System.currentTimeMillis()
        c.cancel()
        val end = waitForEnd(c, 10_000)
        end shouldBe ExportState.Cancelled
        ((System.currentTimeMillis() - cancelAt) < 2_000) shouldBe true
        cache.listFiles().orEmpty().none { it.name.endsWith(".mp4") } shouldBe true
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Video.Media._ID),
            "${MediaStore.Video.Media.DISPLAY_NAME} LIKE ?", arrayOf("$name%"), null,
        )!!.use { it.count shouldBe 0 }
    }

    @Test
    fun theSizeEstimateIsCloseToTheRealFileSize() {
        // A software encoder (the emulator's) does not hold the bitrate on simple footage; SC-003 is judged on hardware.
        assumeTrue("needs a hardware H.264 encoder", hasHardwareAvcEncoder())
        val a = item("video_with_audio.mp4")
        val timeline = project(
            VideoClip("c1", a.id, 0, 2_000, true),
            VideoClip("c2", a.id, 0, 2_000, true),
            media = listOf(a),
        )
        val ratios = listOf(
            ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller),
            ExportSettings(Resolution.R720, FrameRate.F30, Quality.Higher),
            ExportSettings(Resolution.R1080, FrameRate.F30, Quality.Recommended),
        ).map { settings ->
            val c = coordinator()
            val estimate = c.plan(timeline, settings).estimatedBytes
            c.start("p", "pipeline-size", timeline, settings) shouldBe null
            val end = waitForEnd(c) as ExportState.Finished
            savedNames += "pipeline-size-${c.job.value!!.startedAt}.mp4"
            end.bytes.toDouble() / estimate
        }
        // Simple footage lets a hardware encoder stay under its target, so the estimate is judged as an upper bound.
        check(ratios.all { it in 0.3..1.15 }) { "real size / estimate = $ratios" }
    }

    @Test
    fun audioToolsExportWithTheFadesAndSoundOfThePreview() {
        val a = item("video_silent.mp4")
        val music = item("audio.mp3")
        val fx = AudioFx(
            fade = Fade(600, 600),
            noise = NoiseReduction(true, NoiseStrength.Medium),
            eq = EqPresets.settings(EqPreset.Warm),
            voice = VoiceFx(Echo(true, 150, 2), Robot(), Pitch(true, 3), Bass(true, 40)),
        )
        val audio = AudioClip("m1", music.id, 0, 2_000, timelineStartMs = 0, origin = AudioOrigin.Added, fx = fx)
        val timeline = project(VideoClip("c1", a.id, 0, 2_000, false), media = listOf(a, music), audio = listOf(audio))
        val (state, uri) = export(timeline, ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller), "pipeline-fx")
        state.shouldBeFinished()
        val r = retriever(uri!!)
        (r.int(MediaMetadataRetriever.METADATA_KEY_DURATION) in 1_930..2_150) shouldBe true
        r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) shouldBe "yes"
        r.release()
        val rms = audioRms(uri, listOf(0L to 60L, 900L to 1_100L, 1_940L to 2_000L))
        check(rms[0] < rms[1] * 0.35f) { "start level ${rms[0]} should be far below the middle ${rms[1]}" }
        check(rms[2] < rms[1] * 0.35f) { "end level ${rms[2]} should be far below the middle ${rms[1]}" }
    }

    /** SC-006 of feature 027: two audio clips at the same moment are both heard, mixed together. */
    @Test
    fun overlappingAudioClipsAreMixed() {
        val a = item("video_silent.mp4")
        val music = item("audio.mp3")
        fun level(copies: Int, name: String): Float {
            val audio = (0 until copies).map {
                AudioClip("m$it", music.id, 0, 2_000, timelineStartMs = 0, origin = AudioOrigin.Added, volumePercent = 40)
            }
            val timeline = project(VideoClip("c1", a.id, 0, 2_000, false), media = listOf(a, music), audio = audio)
            val (state, uri) = export(timeline, ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller), name)
            state.shouldBeFinished()
            return audioRms(uri!!, listOf(500L to 1_500L))[0]
        }
        val one = level(1, "pipeline-layers-one")
        val two = level(2, "pipeline-layers-two")
        val three = level(3, "pipeline-layers-three")
        check(two > one * 1.6f && two < one * 2.4f) { "two layers $two against one $one" }
        check(three > two * 1.2f) { "three layers $three against two $two" }
    }

    /** Feature 029 (T029): three loud overlays and the base mixed together must not hit the ceiling. */
    @Test
    fun threeLoudOverlaysMixedTogetherDoNotClip() {
        val base = item("video_with_audio.mp4")
        val clip = item("video_with_audio.mp4")
        val overlays = (0 until 3).map {
            OverlayClip("o$it", clip.id, 0, 1_500, 200L, xPermille = 250 + it * 250, attachedAudio = true)
        }
        val timeline = project(VideoClip("c1", base.id, 0, 2_000, true), media = listOf(base, clip))
            .copy(overlayLane = overlays)
        val (state, uri) = export(timeline, ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller), "pipeline-overlay-mix")
        state.shouldBeFinished()
        audioRms(uri!!, listOf(300L to 1_500L))
        println("overlay mix: peak=$lastPeak clipped=$lastClipped of $lastSamples")
        check(lastClipped * 1000 <= lastSamples) { "clipped $lastClipped of $lastSamples samples, peak $lastPeak" }
    }

    /** Feature 029: the sound of an overlay is in the export, at its place, and follows its volume. */
    @Test
    fun aSoundingOverlayIsHeardInTheExportAtItsPlaceAndFollowsItsVolume() {
        val base = item("video_silent.mp4")
        val clip = item("video_with_audio.mp4")
        fun level(volume: Int, name: String): List<Float> {
            val overlay = OverlayClip("o", clip.id, 0, 1_500, 500, attachedAudio = true, volumePercent = volume)
            val timeline = project(VideoClip("c1", base.id, 0, 2_500, false), media = listOf(base, clip))
                .copy(overlayLane = listOf(overlay))
            val (state, uri) = export(timeline, ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller), name)
            state.shouldBeFinished()
            return audioRms(uri!!, listOf(700L to 1_700L, 0L to 300L))
        }
        val full = level(100, "pipeline-overlay-sound-full")
        val half = level(50, "pipeline-overlay-sound-half")
        check(full[0] > 0.005f) { "no overlay sound: ${full[0]}" }
        check(full[1] < full[0] * 0.1f) { "sound before the overlay starts: ${full[1]} against ${full[0]}" }
        check(half[0] > full[0] * 0.35f && half[0] < full[0] * 0.65f) { "half volume ${half[0]} against ${full[0]}" }
    }

    /** Decodes the audio track and returns the RMS (0..1) of each [windows] range in milliseconds. */
    private var lastPeak = 0.0
    private var lastClipped = 0L
    private var lastSamples = 0L

    private fun audioRms(uri: String, windows: List<Pair<Long, Long>>): List<Float> {
        lastPeak = 0.0
        lastClipped = 0
        lastSamples = 0
        val extractor = MediaExtractor().apply { setDataSource(context, Uri.parse(uri), null) }
        val track = (0 until extractor.trackCount).first {
            extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)!!.startsWith("audio/")
        }
        val format = extractor.getTrackFormat(track)
        extractor.selectTrack(track)
        val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
        codec.configure(format, null, null, 0)
        codec.start()
        val sums = DoubleArray(windows.size)
        val counts = LongArray(windows.size)
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false
        var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        var rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        while (!outputDone) {
            if (!inputDone) {
                val index = codec.dequeueInputBuffer(10_000)
                if (index >= 0) {
                    val size = extractor.readSampleData(codec.getInputBuffer(index)!!, 0)
                    if (size < 0) {
                        codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        codec.queueInputBuffer(index, 0, size, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }
            val out = codec.dequeueOutputBuffer(info, 10_000)
            if (out == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                channels = codec.outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                rate = codec.outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else if (out >= 0) {
                val shorts = codec.getOutputBuffer(out)!!.order(java.nio.ByteOrder.nativeOrder()).asShortBuffer()
                var frame = 0L
                while (shorts.remaining() >= channels) {
                    val timeMs = info.presentationTimeUs / 1000 + frame * 1000 / rate
                    var sample = 0.0
                    repeat(channels) { sample += shorts.get() / 32768.0 }
                    sample /= channels
                    windows.forEachIndexed { i, (from, to) ->
                        if (timeMs in from until to) {
                            lastSamples++
                            lastPeak = maxOf(lastPeak, kotlin.math.abs(sample))
                            if (kotlin.math.abs(sample) >= CLIP_LEVEL) lastClipped++
                            sums[i] += sample * sample
                            counts[i]++
                        }
                    }
                    frame++
                }
                codec.releaseOutputBuffer(out, false)
                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
            }
        }
        codec.stop()
        codec.release()
        extractor.release()
        return sums.indices.map { if (counts[it] == 0L) 0f else kotlin.math.sqrt(sums[it] / counts[it]).toFloat() }
    }

    /** Decodes the audio track and returns the zero crossings per second of each [windows] range in milliseconds. */
    private fun zeroCrossRates(uri: String, windows: List<Pair<Long, Long>>): List<Float> {
        val extractor = MediaExtractor().apply { setDataSource(context, Uri.parse(uri), null) }
        val track = (0 until extractor.trackCount).first {
            extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)!!.startsWith("audio/")
        }
        val format = extractor.getTrackFormat(track)
        extractor.selectTrack(track)
        val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
        codec.configure(format, null, null, 0)
        codec.start()
        val sums = DoubleArray(windows.size)
        val last = DoubleArray(windows.size)
        val counts = LongArray(windows.size)
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false
        var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        var rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        while (!outputDone) {
            if (!inputDone) {
                val index = codec.dequeueInputBuffer(10_000)
                if (index >= 0) {
                    val size = extractor.readSampleData(codec.getInputBuffer(index)!!, 0)
                    if (size < 0) {
                        codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        codec.queueInputBuffer(index, 0, size, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }
            val out = codec.dequeueOutputBuffer(info, 10_000)
            if (out == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                channels = codec.outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                rate = codec.outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else if (out >= 0) {
                val shorts = codec.getOutputBuffer(out)!!.order(java.nio.ByteOrder.nativeOrder()).asShortBuffer()
                var frame = 0L
                while (shorts.remaining() >= channels) {
                    val timeMs = info.presentationTimeUs / 1000 + frame * 1000 / rate
                    var sample = 0.0
                    repeat(channels) { sample += shorts.get() / 32768.0 }
                    sample /= channels
                    windows.forEachIndexed { i, (from, to) ->
                        if (timeMs in from until to) {
                            if (counts[i] > 0 && (sample < 0) != (last[i] < 0) && kotlin.math.abs(sample) > 0.002) sums[i] += 1.0
                            last[i] = sample
                            counts[i]++
                        }
                    }
                    frame++
                }
                codec.releaseOutputBuffer(out, false)
                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
            }
        }
        codec.stop()
        codec.release()
        extractor.release()
        return sums.indices.map { if (counts[it] == 0L) 0f else (sums[it] * rate / counts[it]).toFloat() }
    }

    private fun textClip(id: String, start: Long, duration: Long, x: Int = 500, text: String = "TITLE") = TextClip(
        id, text, start, duration, xPermille = x, yPermille = 500, sizePermille = 200,
        style = TextStyle(
            colorArgb = 0xFFFFFFFF.toInt(),
            background = TextBackground(true, 0xFF000000.toInt(), 100),
        ),
    )

    private fun frameAt(uri: String, ms: Long): android.graphics.Bitmap {
        val r = retriever(uri)
        val frame = r.getFrameAtTime(ms * 1000, MediaMetadataRetriever.OPTION_CLOSEST)!!
        r.release()
        return frame
    }

    /** Pixels whose color differs clearly between two frames of the same size. */
    private fun changedPixels(a: android.graphics.Bitmap, b: android.graphics.Bitmap, left: Float = 0f, right: Float = 1f): Int {
        var count = 0
        for (y in 0 until a.height step 2) {
            for (x in (a.width * left).toInt() until (a.width * right).toInt() step 2) {
                val p = a.getPixel(x, y)
                val q = b.getPixel(x, y)
                val d = kotlin.math.abs(android.graphics.Color.red(p) - android.graphics.Color.red(q)) +
                    kotlin.math.abs(android.graphics.Color.green(p) - android.graphics.Color.green(q)) +
                    kotlin.math.abs(android.graphics.Color.blue(p) - android.graphics.Color.blue(q))
                if (d > 150) count++
            }
        }
        return count
    }

    /** Pixels that are nearly white in a frame; the title's white letters on its black box. */
    private fun whitePixels(frame: android.graphics.Bitmap, left: Float = 0f, right: Float = 1f): Int {
        var count = 0
        for (y in 0 until frame.height step 2) {
            for (x in (frame.width * left).toInt() until (frame.width * right).toInt() step 2) {
                val c = frame.getPixel(x, y)
                if (android.graphics.Color.red(c) > 235 && android.graphics.Color.green(c) > 235 &&
                    android.graphics.Color.blue(c) > 235
                ) count++
            }
        }
        return count
    }

    @Test
    fun textShowsOnlyInsideItsWindowAndAtItsPlace() {
        val a = item("video_silent.mp4")
        val plain = project(VideoClip("c1", a.id, 0, 2_000, false), media = listOf(a))
        val titled = plain.copy(textLane = listOf(textClip("t1", 600, 800, x = 250)))
        val settings = ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller)
        val (s1, withoutText) = export(plain, settings, "pipeline-notext")
        val (s2, withText) = export(titled, settings, "pipeline-text")
        s1.shouldBeFinished()
        s2.shouldBeFinished()
        // The same footage with and without the title: frames match outside the window and differ inside, on the left.
        val outside = changedPixels(frameAt(withoutText!!, 300), frameAt(withText!!, 300)) +
            changedPixels(frameAt(withoutText, 1_700), frameAt(withText, 1_700))
        val inside = changedPixels(frameAt(withoutText, 1_000), frameAt(withText, 1_000))
        check(inside > 1_500 && outside < inside / 5) { "changed pixels inside $inside, outside $outside" }
        val left = changedPixels(frameAt(withoutText, 1_000), frameAt(withText, 1_000), 0f, 0.5f)
        val right = changedPixels(frameAt(withoutText, 1_000), frameAt(withText, 1_000), 0.5f, 1f)
        check(left > right * 3) { "text should sit on the left half: left $left right $right" }
    }

    @Test
    fun textAfterTheVideoExtendsTheProjectOverBlack() {
        val a = item("video_silent.mp4")
        val timeline = project(VideoClip("c1", a.id, 0, 1_000, false), media = listOf(a)).copy(
            textLane = listOf(textClip("t1", 1_200, 1_300)),
        )
        val (state, uri) = export(timeline, ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller), "pipeline-text-tail")
        state.shouldBeFinished()
        val r = retriever(uri!!)
        (r.int(MediaMetadataRetriever.METADATA_KEY_DURATION) in 2_400..2_650) shouldBe true
        r.release()
        (whitePixels(frameAt(uri, 2_000)) > 800) shouldBe true
    }

    @Test
    fun aProjectWithOnlyTextExports() {
        val timeline = Timeline(
            projectId = "p", format = ProjectFormat(320, 180, 30),
            textLane = listOf(textClip("t1", 0, 2_000)),
        )
        val (state, uri) = export(timeline, ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller), "pipeline-text-only")
        state.shouldBeFinished()
        val r = retriever(uri!!)
        (r.int(MediaMetadataRetriever.METADATA_KEY_DURATION) in 1_900..2_200) shouldBe true
        r.release()
        (whitePixels(frameAt(uri, 1_000)) > 800) shouldBe true
    }

    private fun meanLuma(frame: android.graphics.Bitmap, l: Float = 0f, t: Float = 0f, r: Float = 1f, b: Float = 1f): Float {
        var sum = 0.0
        var n = 0
        for (y in (frame.height * t).toInt() until (frame.height * b).toInt() step 2) {
            for (x in (frame.width * l).toInt() until (frame.width * r).toInt() step 2) {
                val c = frame.getPixel(x, y)
                sum += 0.2126 * android.graphics.Color.red(c) + 0.7152 * android.graphics.Color.green(c) +
                    0.0722 * android.graphics.Color.blue(c)
                n++
            }
        }
        return (sum / n).toFloat()
    }

    private fun filtered(filter: ClipFilter, name: String): String {
        val a = item("video_with_audio.mp4")
        val timeline = project(VideoClip("c1", a.id, 0, 2_000, true, filter = filter), media = listOf(a))
        val (state, uri) = export(timeline, ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller), name)
        state.shouldBeFinished()
        return uri!!
    }

    @Test
    fun filtersChangeTheExportedPictureAsTheMatrixPredicts() {
        val plain = frameAt(filtered(ClipFilter(), "pipeline-flt-plain"), 1_000)
        val bright = frameAt(filtered(ClipFilter(brightness = 100), "pipeline-flt-bright"), 1_000)
        val mono = frameAt(filtered(ClipFilter(look = FilterLook.BlackAndWhite), "pipeline-flt-bw"), 1_000)
        val vignette = frameAt(filtered(ClipFilter(vignette = 100), "pipeline-flt-vig"), 1_000)
        check(meanLuma(bright) > meanLuma(plain) + 25) { "bright ${meanLuma(bright)} vs plain ${meanLuma(plain)}" }
        var spread = 0
        for (y in 0 until mono.height step 8) for (x in 0 until mono.width step 8) {
            val c = mono.getPixel(x, y)
            spread += maxOf(
                kotlin.math.abs(android.graphics.Color.red(c) - android.graphics.Color.green(c)),
                kotlin.math.abs(android.graphics.Color.green(c) - android.graphics.Color.blue(c)),
            )
        }
        val samples = (mono.height / 8) * (mono.width / 8)
        check(spread / samples < 12) { "black and white keeps color: mean channel spread ${spread / samples}" }
        val cornersPlain = meanLuma(plain, 0f, 0f, 0.15f, 0.15f) + meanLuma(plain, 0.85f, 0.85f, 1f, 1f)
        val cornersVignette = meanLuma(vignette, 0f, 0f, 0.15f, 0.15f) + meanLuma(vignette, 0.85f, 0.85f, 1f, 1f)
        check(cornersVignette < cornersPlain * 0.6f) { "corners $cornersVignette vs $cornersPlain" }
        val centerPlain = meanLuma(plain, 0.4f, 0.4f, 0.6f, 0.6f)
        val centerVignette = meanLuma(vignette, 0.4f, 0.4f, 0.6f, 0.6f)
        check(kotlin.math.abs(centerVignette - centerPlain) < 12f) { "center $centerVignette vs $centerPlain" }
    }

    @Test
    fun aNeutralFilterExportsLikeNoFilter() {
        val a = filtered(ClipFilter(look = FilterLook.Vivid, intensity = 0), "pipeline-flt-neutral")
        val b = filtered(ClipFilter(), "pipeline-flt-none")
        check(kotlin.math.abs(meanLuma(frameAt(a, 1_000)) - meanLuma(frameAt(b, 1_000))) < 1.5f)
    }

    @Test
    fun aPhotoWithAFilterExports() {
        val photo = item("photo.jpg")
        val timeline = project(
            VideoClip("p1", photo.id, 0, 1_500, false, filter = ClipFilter(brightness = 100)), media = listOf(photo),
        )
        val plain = project(VideoClip("p1", photo.id, 0, 1_500, false), media = listOf(photo))
        val settings = ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller)
        val (s1, u1) = export(timeline, settings, "pipeline-photo-flt")
        val (s2, u2) = export(plain, settings, "pipeline-photo-plain")
        check(s2 is ExportState.Finished) { "plain photo export: $s2" }
        check(s1 is ExportState.Finished) { "filtered photo export: $s1" }
        check(meanLuma(frameAt(u1!!, 700)) > meanLuma(frameAt(u2!!, 700)) + 25)
    }

    /** Bounding box (as shares of the frame) of the pixels that differ between two frames, or null when none do. */
    private fun changedBox(a: android.graphics.Bitmap, b: android.graphics.Bitmap): FloatArray? {
        var minX = Int.MAX_VALUE; var maxX = -1; var minY = Int.MAX_VALUE; var maxY = -1
        for (y in 0 until a.height step 2) for (x in 0 until a.width step 2) {
            val p = a.getPixel(x, y)
            val q = b.getPixel(x, y)
            val d = kotlin.math.abs(android.graphics.Color.red(p) - android.graphics.Color.red(q)) +
                kotlin.math.abs(android.graphics.Color.green(p) - android.graphics.Color.green(q)) +
                kotlin.math.abs(android.graphics.Color.blue(p) - android.graphics.Color.blue(q))
            if (d > 90) { minX = minOf(minX, x); maxX = maxOf(maxX, x); minY = minOf(minY, y); maxY = maxOf(maxY, y) }
        }
        if (maxX < 0) return null
        return floatArrayOf(
            minX.toFloat() / a.width, minY.toFloat() / a.height, maxX.toFloat() / a.width, maxY.toFloat() / a.height,
        )
    }

    private val MIN_OVERLAY_PIXELS = 3_000

    /** A solid brown picture (no footage color is close), so an overlay can be told apart from any footage. */
    private fun magentaPhoto(): MediaItem {
        val file = File(context.cacheDir, "magenta.jpg")
        val bitmap = android.graphics.Bitmap.createBitmap(400, 300, android.graphics.Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(0xFF804000.toInt())
        file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, it) }
        return runBlocking { MediaProbe(context).probe(Uri.fromFile(file), "magenta.jpg").getOrThrow() }
    }

    /** Box (shares of the frame) around the brown pixels, ignoring stray ones, or null when there are too few. */
    private fun magentaBox(frame: android.graphics.Bitmap): FloatArray? {
        val xs = ArrayList<Int>()
        val ys = ArrayList<Int>()
        for (y in 0 until frame.height step 2) for (x in 0 until frame.width step 2) {
            val c = frame.getPixel(x, y)
            if (android.graphics.Color.red(c) in 100..160 && android.graphics.Color.green(c) in 40..90 &&
                android.graphics.Color.blue(c) < 40
            ) {
                xs += x
                ys += y
            }
        }
        if (xs.size < MIN_OVERLAY_PIXELS) return null
        xs.sort()
        ys.sort()
        fun at(list: List<Int>, share: Double) = list[((list.size - 1) * share).toInt()]
        return floatArrayOf(
            at(xs, 0.02).toFloat() / frame.width, at(ys, 0.02).toFloat() / frame.height,
            at(xs, 0.98).toFloat() / frame.width, at(ys, 0.98).toFloat() / frame.height,
        )
    }

    @Test
    fun overlaysAreDrawnOverTheMainVideoInTheirWindowAtTheirPlaceAndSize() {
        val base = item("video_silent.mp4")
        val photo = magentaPhoto()
        val timeline = project(VideoClip("c1", base.id, 0, 3_000, false), media = listOf(base)).let {
            it.copy(
                media = it.media + (photo.id to photo),
                overlayLane = listOf(
                    OverlayClip("o1", photo.id, 0, 2_000, 500, xPermille = 250, yPermille = 300, widthPermille = 400),
                ),
            )
        }
        val (state, uri) = export(timeline, ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller), "pipeline-ov")
        check(state is ExportState.Finished) { "overlay export: $state" }
        val before = magentaBox(frameAt(uri!!, 250))
        val during = magentaBox(frameAt(uri, 1_500))
        val after = magentaBox(frameAt(uri, 2_800))
        check(during != null) { "overlay not visible in its window" }
        val centerX = (during[0] + during[2]) / 2
        val centerY = (during[1] + during[3]) / 2
        val width = during[2] - during[0]
        check(kotlin.math.abs(centerX - 0.25f) < 0.04f && kotlin.math.abs(centerY - 0.3f) < 0.05f) {
            "overlay center $centerX,$centerY box ${during.toList()}"
        }
        check(width in 0.36f..0.44f) { "overlay width $width of the frame" }
        check(before == null) { "overlay shows before its start: ${before?.toList()}" }
        check(after == null) { "overlay shows after its end: ${after?.toList()}" }
    }

    @Test
    fun twoOverlaysKeepTheirOwnPlaceAndOpacityAndAVideoOverlayPlays() {
        val base = item("video_silent.mp4")
        val photo = magentaPhoto()
        val clip = item("video_portrait.mp4")
        val timeline = project(VideoClip("c1", base.id, 0, 3_000, false), media = listOf(base)).let {
            it.copy(
                media = it.media + (photo.id to photo) + (clip.id to clip),
                overlayLane = listOf(
                    OverlayClip("o1", photo.id, 0, 2_500, 0, xPermille = 200, yPermille = 250, widthPermille = 250),
                    OverlayClip("o2", clip.id, 0, 2_000, 500, xPermille = 800, yPermille = 700, widthPermille = 300),
                ),
            )
        }
        val (state, uri) = export(timeline, ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller), "pipeline-ov2")
        check(state is ExportState.Finished) { "two overlays export: $state" }
        val frame = frameAt(uri!!, 1_500)
        val magenta = magentaBox(frame)
        check(magenta != null && (magenta[0] + magenta[2]) / 2 < 0.35f) { "first overlay at ${magenta?.toList()}" }
        // The second overlay is a video: the frame around (80%, 70%) differs from the footage without it.
        val plain = project(VideoClip("c1", base.id, 0, 3_000, false), media = listOf(base))
        val (ps, pu) = export(plain, ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller), "pipeline-ov2-plain")
        check(ps is ExportState.Finished)
        check(changedPixels(frameAt(pu!!, 1_500), frame, 0.7f, 0.9f) > 500) { "video overlay not drawn near 80%, 70%" }
    }

    private fun solidPhoto(name: String, color: Int): MediaItem {
        val file = File(context.cacheDir, "$name.jpg")
        val bitmap = android.graphics.Bitmap.createBitmap(400, 300, android.graphics.Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color)
        file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, it) }
        return runBlocking { MediaProbe(context).probe(Uri.fromFile(file), "$name.jpg").getOrThrow() }
    }

    private fun meanRgb(frame: android.graphics.Bitmap, l: Float, r: Float): IntArray {
        val sum = LongArray(3)
        var n = 0
        for (y in 0 until frame.height step 2) for (x in (frame.width * l).toInt() until (frame.width * r).toInt() step 2) {
            val c = frame.getPixel(x, y)
            sum[0] += android.graphics.Color.red(c); sum[1] += android.graphics.Color.green(c)
            sum[2] += android.graphics.Color.blue(c)
            n++
        }
        return intArrayOf((sum[0] / n).toInt(), (sum[1] / n).toInt(), (sum[2] / n).toInt())
    }

    private fun twoPhotos(kind: TransitionKind?): Triple<Timeline, MediaItem, MediaItem> {
        val brown = solidPhoto("brown", 0xFF804000.toInt())
        val blue = solidPhoto("blue", 0xFF0040C0.toInt())
        val transition = kind?.let { Transition(it, 1_000) }
        val timeline = project(
            VideoClip("p1", brown.id, 0, 3_000, false, transitionOut = transition),
            VideoClip("p2", blue.id, 0, 3_000, false),
            media = listOf(brown, blue),
        )
        return Triple(timeline, brown, blue)
    }

    @Test
    fun transitionsBlendTheTwoClipsAndShortenTheProject() = checkTransitions(TransitionKind.entries, blueMin = 130)

    /** The ten transitions of feature 035, with a little more room for the colors a phone's encoder shifts. */
    @Test
    fun theNewTransitionsExportAndEndOnTheSecondClip() =
        checkTransitions(TransitionKind.entries.drop(5), blueMin = 120)

    private fun checkTransitions(kinds: List<TransitionKind>, blueMin: Int) {
        val settings = ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller)
        for (kind in kinds) {
            val (timeline, _, _) = twoPhotos(kind)
            val (state, uri) = export(timeline, settings, "pipeline-tr-${kind.name}")
            check(state is ExportState.Finished) { "$kind export: $state" }
            val duration = retriever(uri!!).extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toLong()
            check(duration in 4_800..5_200) { "$kind: two 3 s clips with a 1 s transition last $duration ms" }
            val early = meanRgb(frameAt(uri, 1_000), 0f, 1f)
            val late = meanRgb(frameAt(uri, 4_000), 0f, 1f)
            check(early[0] > 70 && early[2] < 30) { "$kind: first clip not brown before the transition ${early.toList()}" }
            check(late[2] > blueMin && late[0] < 30) { "$kind: second clip not blue after the transition ${late.toList()}" }
            val mid = frameAt(uri, 2_500)
            val all = meanRgb(mid, 0f, 1f)
            val left = meanRgb(mid, 0f, 0.4f)
            val right = meanRgb(mid, 0.6f, 1f)
            val msg = "$kind mid: all ${all.toList()} left ${left.toList()} right ${right.toList()}"
            when (kind) {
                TransitionKind.Dissolve, TransitionKind.Zoom ->
                    check(all[0] in 20..75 && all[2] in 40..130) { "not a blend: $msg" }
                TransitionKind.FadeBlack -> check(all.max() < 25) { "not black in the middle: $msg" }
                TransitionKind.Slide, TransitionKind.Cover, TransitionKind.WhipPan ->
                    check(left[0] > 80 && left[2] < 40 && right[2] > 100 && right[0] < 40) { "not a side by side: $msg" }
                TransitionKind.PushRight ->
                    check(left[2] > 100 && left[0] < 40 && right[0] > 80 && right[2] < 40) { "not pushed right: $msg" }
                TransitionKind.PushUp, TransitionKind.PushDown ->
                    check(left[0] in 30..100 && right[0] in 30..100 && left[2] in 50..150) { "not a vertical push: $msg" }
                TransitionKind.TiltSlide ->
                    check(left[0] > left[2] && right[2] > right[0]) { "not a tilted side by side: $msg" }
                TransitionKind.Flip, TransitionKind.DipZoom -> check(all.max() < 40) { "not dark in the middle: $msg" }
                TransitionKind.Spin, TransitionKind.ZoomOut, TransitionKind.Drop ->
                    check(!(all[0] > 110 && all[2] < 30) && !(all[2] > 170 && all[0] < 30)) { "not moving: $msg" }
            }
        }
    }

    /** A picture of vertical bands, brown then blue, each [bandPx] wide (of 400), so effects have detail to change. */
    private fun bandsPhoto(name: String, bandPx: Int): MediaItem {
        val file = File(context.cacheDir, "$name.jpg")
        val bitmap = android.graphics.Bitmap.createBitmap(400, 300, android.graphics.Bitmap.Config.ARGB_8888)
        for (x in 0 until 400) {
            val color = if ((x / bandPx) % 2 == 0) 0xFF804000.toInt() else 0xFF0040C0.toInt()
            for (y in 0 until 300) bitmap.setPixel(x, y, color)
        }
        file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, it) }
        return runBlocking { MediaProbe(context).probe(Uri.fromFile(file), "$name.jpg").getOrThrow() }
    }

    private fun effectFrames(photo: MediaItem, effect: ClipEffect?, name: String, vararg times: Long): List<android.graphics.Bitmap> {
        val timeline = project(VideoClip("e1", photo.id, 0, 2_000, false, effects = listOfNotNull(effect)), media = listOf(photo))
        val (state, uri) = export(timeline, ExportSettings(Resolution.R720, FrameRate.F30, Quality.Higher), name)
        check(state is ExportState.Finished) { "$name export: $state" }
        return times.map { frameAt(uri!!, it) }
    }

    /** Along the middle row between the bars: [brown/blue changes, share of samples that are neither]. */
    private fun rowProfile(frame: android.graphics.Bitmap): Pair<Int, Float> {
        var last = 0
        var changes = 0
        var mixed = 0
        var n = 0
        val y = frame.height / 2
        for (x in (frame.width * 0.15f).toInt() until (frame.width * 0.85f).toInt()) {
            val c = frame.getPixel(x, y)
            val r = android.graphics.Color.red(c)
            val b = android.graphics.Color.blue(c)
            val kind = when {
                r > b + 40 -> 1
                b > r + 40 -> 2
                else -> 0
            }
            n++
            if (kind == 0) mixed++ else {
                if (last != 0 && kind != last) changes++
                last = kind
            }
        }
        return changes to mixed.toFloat() / n
    }

    @Test
    fun clipEffectsChangeTheExportedPicture() {
        val stripes = bandsPhoto("stripes", 10)
        val split = bandsPhoto("split", 200)
        val plain = rowProfile(effectFrames(stripes, null, "pipeline-fx-plain", 1_000)[0])
        check(plain.first >= 20 && plain.second < 0.1f) { "plain stripes: changes ${plain.first} mixed ${plain.second}" }

        val blur = rowProfile(effectFrames(stripes, ClipEffect(EffectKind.Blur, 100), "pipeline-fx-blur", 1_000)[0])
        check(blur.second > 0.4f) { "blur left too few mixed samples: ${blur.second} (plain ${plain.second})" }

        val pixel = rowProfile(effectFrames(stripes, ClipEffect(EffectKind.Pixelate, 100), "pipeline-fx-pixel", 1_000)[0])
        check(pixel.first < plain.first / 2) { "pixelate kept ${pixel.first} changes of ${plain.first}" }

        val shake = effectFrames(stripes, ClipEffect(EffectKind.Shake, 100), "pipeline-fx-shake", 1_000, 1_083)
        val still = effectFrames(stripes, null, "pipeline-fx-still", 1_000, 1_083)
        val moved = changedPixels(shake[0], shake[1])
        val base = changedPixels(still[0], still[1])
        check(moved > base + 2_000) { "shake changed $moved pixels, a still picture $base" }

        val mirrored = effectFrames(split, ClipEffect(EffectKind.Mirror, 100), "pipeline-fx-mirror", 1_000)[0]
        val right = meanRgb(mirrored, 0.6f, 0.8f)
        check(right[0] > right[2] + 40) { "right side is not mirrored brown: ${right.toList()}" }
        val unmirrored = meanRgb(effectFrames(split, null, "pipeline-fx-split", 1_000)[0], 0.6f, 0.8f)
        check(unmirrored[2] > unmirrored[0] + 40) { "plain right side is not blue: ${unmirrored.toList()}" }
    }

    /** Feature 025: each new effect changes the exported picture compared with the same clip without one. */
    @Test
    fun theNewEffectsChangeTheExportedPicture() {
        val stripes = bandsPhoto("stripes25", 10)
        val plain = effectFrames(stripes, null, "pipeline-fx25-plain", 1_000, 1_500)
        val glow = effectFrames(stripes, ClipEffect(EffectKind.Glow, 100), "pipeline-fx25-Glow", 1_000)[0]
        val before = meanRgb(plain[0], 0.15f, 0.85f).sum()
        val after = meanRgb(glow, 0.15f, 0.85f).sum()
        check(after > before + 15) { "glow did not brighten: $before to $after" }
        val kinds = listOf(EffectKind.Glitch, EffectKind.RgbSplit, EffectKind.Vhs, EffectKind.Grain)
        for (kind in kinds) {
            val frames = effectFrames(stripes, ClipEffect(kind, 100), "pipeline-fx25-${kind.name}", 1_000, 1_500)
            val changed = maxOf(changedPixels(plain[0], frames[0]), changedPixels(plain[1], frames[1]))
            check(changed > 2_000) { "$kind changed only $changed pixels" }
        }
    }

    /** Feature 026: a second effect on the same clip is drawn too, after the first. */
    @Test
    fun stackedEffectsAreAllDrawn() {
        val stripes = bandsPhoto("stripes26", 10)
        fun frame(effects: List<ClipEffect>, name: String): android.graphics.Bitmap {
            val timeline = project(VideoClip("s1", stripes.id, 0, 2_000, false, effects = effects), media = listOf(stripes))
            val (state, uri) = export(timeline, ExportSettings(Resolution.R720, FrameRate.F30, Quality.Higher), name)
            check(state is ExportState.Finished) { "$name export: $state" }
            return frameAt(uri!!, 1_000)
        }
        val one = frame(listOf(ClipEffect(EffectKind.Glow, 100)), "pipeline-stack-one")
        val two = frame(listOf(ClipEffect(EffectKind.Glow, 100), ClipEffect(EffectKind.Grain, 100)), "pipeline-stack-two")
        check(changedPixels(one, two) > 1_000) { "the second effect changed only ${changedPixels(one, two)} pixels" }
    }

    @Test
    fun aFilledTemplateExportsWithItsLengthLookAndTransitions() {
        val a = item("video_with_audio.mp4")
        val b = item("video_silent.mp4")
        val photo = solidPhoto("tpl-photo", 0xFF804000.toInt())
        val template = com.petruvideo.core.model.template.TemplateCatalog.all.first { it.id == "memories" }
        var n = 0
        val fill = com.petruvideo.core.model.template.TemplateFiller.fill(
            template, listOf(a, photo, b), "p", newId = { "t${n++}" },
        )!!
        val timeline = fill.timeline.copy(format = ProjectFormat(320, 180, 30))
        val clipsMs = timeline.videoLane.sumOf { it.lengthMs } - 2 * 800
        timeline.videoLengthMs shouldBe clipsMs
        val (state, uri) = export(timeline, ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller), "pipeline-tpl")
        check(state is ExportState.Finished) { "template export: $state" }
        val duration = retriever(uri!!).extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toLong()
        check(kotlin.math.abs(duration - clipsMs) < 300) { "template length $duration, expected $clipsMs" }
    }

    @Test
    fun anAnimatedOverlayIsDrawnWhereItsKeyframesPutItAtEachMoment() {
        val base = item("video_silent.mp4")
        val photo = magentaPhoto()
        val frames = listOf(
            com.petruvideo.core.model.Keyframe(0, com.petruvideo.core.model.Pose(250, 300, 300, 100)),
            com.petruvideo.core.model.Keyframe(2_000, com.petruvideo.core.model.Pose(750, 700, 500, 100)),
        )
        val overlay = OverlayClip("o1", photo.id, 0, 3_000, 0).withKeyframes(frames)
        val timeline = project(VideoClip("c1", base.id, 0, 3_000, false), media = listOf(base)).let {
            it.copy(media = it.media + (photo.id to photo), overlayLane = listOf(overlay))
        }
        val (state, uri) = export(timeline, ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller), "pipeline-kf")
        check(state is ExportState.Finished) { "keyframe export: $state" }
        for (time in listOf(300L, 1_000L, 1_700L, 2_600L)) {
            val box = magentaBox(frameAt(uri!!, time))
            check(box != null) { "overlay not visible at $time ms" }
            val pose = overlay.poseAt(time)
            val cx = (box[0] + box[2]) / 2
            val cy = (box[1] + box[3]) / 2
            val width = box[2] - box[0]
            check(kotlin.math.abs(cx - pose.xPermille / 1000f) < 0.05f && kotlin.math.abs(cy - pose.yPermille / 1000f) < 0.07f) {
                "at $time ms the center is $cx,$cy, the pose says ${pose.xPermille / 1000f},${pose.yPermille / 1000f}"
            }
            check(kotlin.math.abs(width - pose.sizePermille / 1000f) < 0.07f) {
                "at $time ms the width is $width, the pose says ${pose.sizePermille / 1000f}"
            }
        }
    }

    @Test
    fun animatedTextMovesAcrossTheFrameAndFadesWithItsOpacityKeyframes() {
        val a = item("video_silent.mp4")
        val frames = listOf(
            com.petruvideo.core.model.Keyframe(0, com.petruvideo.core.model.Pose(250, 500, 200, 100)),
            com.petruvideo.core.model.Keyframe(1_500, com.petruvideo.core.model.Pose(750, 500, 200, 100)),
            com.petruvideo.core.model.Keyframe(2_000, com.petruvideo.core.model.Pose(750, 500, 200, 0)),
        )
        val title = textClip("t1", 0, 2_500, x = 250).withKeyframes(frames)
        val timeline = project(VideoClip("c1", a.id, 0, 3_000, false), media = listOf(a)).copy(textLane = listOf(title))
        val settings = ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller)
        val (state, uri) = export(timeline, settings, "pipeline-kf-text")
        val (plainState, plain) = export(timeline.copy(textLane = emptyList()), settings, "pipeline-kf-text-plain")
        check(state is ExportState.Finished && plainState is ExportState.Finished) { "text exports: $state, $plainState" }
        fun diff(ms: Long, l: Float = 0f, r: Float = 1f) = changedPixels(frameAt(plain!!, ms), frameAt(uri!!, ms), l, r)
        val earlyLeft = diff(200, 0f, 0.5f)
        val earlyRight = diff(200, 0.5f, 1f)
        check(earlyLeft > earlyRight * 3) { "text should start on the left: $earlyLeft left, $earlyRight right" }
        val lateLeft = diff(1_450, 0f, 0.5f)
        val lateRight = diff(1_450, 0.5f, 1f)
        check(lateRight > lateLeft * 3) { "text should be on the right by 1.5 s: $lateLeft left, $lateRight right" }
        val faded = diff(2_200)
        check(faded < diff(200) / 4) { "text should have faded out: $faded changed pixels against ${diff(200)}" }
    }

    @Test
    fun aVoiceOverIsHeardInTheExportAtTheTimeItWasRecorded() {
        val a = item("video_silent.mp4")
        val file = File(context.cacheDir, "take-${System.nanoTime()}.aac").also { it.writeBytes(SyntheticTake.bytes(1_500)) }
        try {
            val take = com.petruvideo.core.media.voiceover.VoiceOverStore(File(context.cacheDir, "vo-test"))
                .mediaItemFor(file, 1_500)
            val timeline = com.petruvideo.core.model.engine.AddVoiceOver(take, 1_000, { "vo" })
                .apply(project(VideoClip("c1", a.id, 0, 3_000, false), media = listOf(a)))
                .let { (it as com.petruvideo.core.model.engine.CommandResult.Applied).timeline }
            val (state, uri) = export(timeline, ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller), "pipeline-vo")
            state.shouldBeFinished()
            val r = retriever(uri!!)
            r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) shouldBe "yes"
            r.release()
            val rms = audioRms(uri, listOf(100L to 800L, 1_300L to 2_200L, 2_700L to 2_950L))
            check(rms[1] > 0.02f) { "voice-over not heard: ${rms[1]}" }
            check(rms[0] < rms[1] * 0.2f) { "sound before the voice-over: ${rms[0]} against ${rms[1]}" }
            check(rms[2] < rms[1] * 0.2f) { "sound after the voice-over: ${rms[2]} against ${rms[1]}" }
        } finally {
            file.delete()
        }
    }

    /** SC-003 of feature 015: music is lowered under a quieter sound by the chosen amount, and only there. */
    @Test
    fun duckedMusicIsLowerUnderASoundThanAloneAndNormalWithoutDucking() {
        val a = item("video_silent.mp4")
        val files = listOf("duck-music", "duck-voice").map { name ->
            File(context.cacheDir, "$name-${System.nanoTime()}.aac").also {
                it.writeBytes(SyntheticTake.bytes(if (name == "duck-music") 3_000 else 1_000))
            }
        }
        try {
            val store = com.petruvideo.core.media.voiceover.VoiceOverStore(File(context.cacheDir, "duck-test"))
            val musicItem = store.mediaItemFor(files[0], 3_000)
            val voiceItem = store.mediaItemFor(files[1], 1_000)
            fun timeline(ducked: Boolean) = project(VideoClip("c1", a.id, 0, 3_000, false), media = listOf(a)).let {
                it.copy(
                    media = it.media + (musicItem.id to musicItem) + (voiceItem.id to voiceItem),
                    audioLane = listOf(
                        AudioClip(
                            "music", musicItem.id, 0, 3_000, 0, origin = AudioOrigin.Added,
                            ducking = com.petruvideo.core.model.Ducking(
                                ducked, 100, com.petruvideo.core.model.DuckSmoothness.Quick,
                            ),
                        ),
                        AudioClip("voice", voiceItem.id, 0, 1_000, 1_000, origin = AudioOrigin.Added, volumePercent = 15),
                    ),
                )
            }
            val settings = ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller)
            val windows = listOf(200L to 700L, 1_300L to 1_800L, 2_400L to 2_900L)
            val (s1, ducked) = export(timeline(true), settings, "pipeline-duck")
            s1.shouldBeFinished()
            val d = audioRms(ducked!!, windows)
            val (s2, plain) = export(timeline(false), settings, "pipeline-noduck")
            s2.shouldBeFinished()
            val p = audioRms(plain!!, windows)
            check(d[1] < d[0] * 0.35f) { "ducked: under the sound ${d[1]} against alone ${d[0]}" }
            check(d[2] > d[0] * 0.8f) { "ducked: music did not come back, ${d[2]} against ${d[0]}" }
            check(p[1] > p[0] * 0.9f) { "not ducked: under the sound ${p[1]} against alone ${p[0]}" }
        } finally {
            files.forEach { it.delete() }
        }
    }

    private fun halvesAt(frame: android.graphics.Bitmap, x: Float, y: Float): String {
        val c = frame.getPixel((frame.width * x).toInt(), (frame.height * y).toInt())
        val r = android.graphics.Color.red(c)
        val b = android.graphics.Color.blue(c)
        return when {
            r > b + 40 -> "brown"
            b > r + 40 -> "blue"
            else -> "other"
        }
    }

    private fun transformed(transform: com.petruvideo.core.model.ClipTransform, name: String): android.graphics.Bitmap {
        val photo = halvesPhoto()
        val clip = VideoClip("t1", photo.id, 0, 2_000, false, transform = transform)
        val (state, uri) = export(project(clip, media = listOf(photo)), ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller), name)
        check(state is ExportState.Finished) { "$name export: $state" }
        return frameAt(uri!!, 800)
    }

    /** SC-003 of feature 016: orientation and crop in the export are the ones the transform describes. */
    @Test
    fun aClipIsTurnedMirroredAndCroppedAsItsTransformSays() {
        val t = com.petruvideo.core.model.ClipTransform()
        val plain = transformed(t, "pipeline-xf-plain")
        check(halvesAt(plain, 0.4f, 0.5f) == "brown" && halvesAt(plain, 0.6f, 0.5f) == "blue") { "plain picture is wrong" }

        // A quarter turn clockwise: the left (brown) half goes to the top.
        val turned = transformed(t.rotated(), "pipeline-xf-turn")
        check(halvesAt(turned, 0.5f, 0.25f) == "brown" && halvesAt(turned, 0.5f, 0.75f) == "blue") {
            "turned: top ${halvesAt(turned, 0.5f, 0.25f)}, bottom ${halvesAt(turned, 0.5f, 0.75f)}"
        }

        // A horizontal mirror swaps the halves.
        val mirrored = transformed(t.flippedHorizontally(), "pipeline-xf-flip")
        check(halvesAt(mirrored, 0.4f, 0.5f) == "blue" && halvesAt(mirrored, 0.6f, 0.5f) == "brown") { "mirror is wrong" }

        // Cropping to the right half shows only blue, fitted across the frame.
        val cropped = transformed(
            t.copy(crop = com.petruvideo.core.model.CropBox(500, 0, 1000, 1000)), "pipeline-xf-crop",
        )
        check(halvesAt(cropped, 0.5f, 0.5f) == "blue" && halvesAt(cropped, 0.4f, 0.3f) == "blue") { "crop shows brown" }

        // Turn first, then crop the top half of the turned picture: the brown half stays.
        val both = transformed(
            t.rotated().copy(crop = com.petruvideo.core.model.CropBox(0, 0, 1000, 500)), "pipeline-xf-both",
        )
        check(halvesAt(both, 0.5f, 0.5f) == "brown" && halvesAt(both, 0.5f, 0.3f) == "brown") { "turn and crop is wrong" }
    }

    /** FR-011 of feature 016: keyframes zoom and pan the turned picture, after the turn. */
    @Test
    fun keyframesActOnThePictureAfterItIsTurned() {
        val photo = halvesPhoto()
        val clip = VideoClip(
            "k1", photo.id, 0, 2_000, false,
            transform = com.petruvideo.core.model.ClipTransform().rotated(),
        ).withKeyframes(
            listOf(com.petruvideo.core.model.Keyframe(0, com.petruvideo.core.model.Pose(500, 250, 2_000, 100))),
        )
        val (state, uri) = export(project(clip, media = listOf(photo)), ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller), "pipeline-xf-kf")
        check(state is ExportState.Finished) { "turn and keyframe export: $state" }
        val frame = frameAt(uri!!, 800)
        // Turned, the brown half is on top; zoomed on the top half, only brown is left in the middle of the frame.
        check(halvesAt(frame, 0.5f, 0.5f) == "brown" && halvesAt(frame, 0.5f, 0.8f) == "brown") { "keyframes did not act on the turned picture" }
    }

    /** SC-003 of feature 017: the exported freeze holds the frame the playhead showed, and the video goes on after it. */
    @Test
    fun aFreezeHoldsTheFrameAtThePlayheadAndTheVideoContinuesAfterIt() = runBlocking<Unit> {
        val a = item("video_silent.mp4")
        val settings = ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller)
        val length = minOf(3_000L, a.durationMs ?: 3_000L)
        val original = project(VideoClip("c1", a.id, 0, length, false), media = listOf(a))
        val (s1, plainUri) = export(original, settings, "pipeline-fz-plain")
        s1.shouldBeFinished()
        val still = com.petruvideo.core.media.freeze.FreezeStills(context, MediaProbe(context)).make(a, 1_000)!!
        val frozen = (com.petruvideo.core.model.engine.FreezeFrame("c1", 1_000, 2_000, still).apply(original) as
            com.petruvideo.core.model.engine.CommandResult.Applied).timeline
        val (s2, frozenUri) = export(frozen, settings, "pipeline-fz-frozen")
        s2.shouldBeFinished()
        val atPlayhead = frameAt(plainUri!!, 1_000)
        val early = frameAt(frozenUri!!, 1_400)
        val late = frameAt(frozenUri, 2_800)
        val total = (atPlayhead.width / 2) * (atPlayhead.height / 2)
        check(changedPixels(atPlayhead, early) < total / 10) { "freeze differs from the playhead frame" }
        check(changedPixels(early, late) < total / 20) { "the picture moved during the freeze" }
        val after = frameAt(frozenUri, 3_300)
        val sourceAfter = frameAt(plainUri, 1_300)
        check(changedPixels(sourceAfter, after) < total / 5) { "the video did not continue after the freeze" }
        r(frozenUri).let { val ms = it.int(MediaMetadataRetriever.METADATA_KEY_DURATION); check(ms in (length + 1_900)..(length + 2_300)) { "length $ms" } }
    }

    private fun r(uri: String) = retriever(uri)

    /** SC-003 and SC-004 of feature 017: a reversed clip exports backwards, picture and sound. */
    @Test
    fun aReversedClipIsExportedBackwardsWithItsSound() {
        val a = item("video_with_audio.mp4")
        val length = minOf(3_000L, a.durationMs ?: 3_000L)
        val settings = ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller)
        val forward = project(VideoClip("c1", a.id, 0, length, true), media = listOf(a))
        val backward = (com.petruvideo.core.model.engine.SetReversed("c1", true).apply(forward) as
            com.petruvideo.core.model.engine.CommandResult.Applied).timeline
        val (s1, forwardUri) = export(forward, settings, "pipeline-rev-forward")
        s1.shouldBeFinished()
        val (s2, reversedUri) = export(backward, settings, "pipeline-rev-back")
        s2.shouldBeFinished()
        val r = retriever(reversedUri!!)
        r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) shouldBe "yes"
        check(kotlin.math.abs(r.int(MediaMetadataRetriever.METADATA_KEY_DURATION) - length) < 400) { "length" }
        r.release()
        val early = frameAt(forwardUri!!, 150)
        val late = frameAt(forwardUri, length - 250)
        check(changedPixels(early, late) > 0) { "the source never changes, the test cannot tell the order" }
        val reversedStart = frameAt(reversedUri, 150)
        check(changedPixels(reversedStart, late) < changedPixels(reversedStart, early)) { "reversed start is not the source end" }
        val reversedEnd = frameAt(reversedUri, length - 250)
        check(changedPixels(reversedEnd, early) < changedPixels(reversedEnd, late)) { "reversed end is not the source start" }
    }

    /** SC-002 of feature 018: the exported length follows the curve, for every preset. */
    @Test
    fun everySpeedPresetExportsWithTheLengthTheEditorShows() {
        val a = item("video_with_audio.mp4")
        val length = minOf(3_000L, a.durationMs ?: 3_000L)
        val settings = ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller)
        for (preset in listOf(com.petruvideo.core.model.SpeedPreset.Bullet, com.petruvideo.core.model.SpeedPreset.Montage)) {
            val base = project(VideoClip("c1", a.id, 0, length, true), media = listOf(a))
            val ramped = (com.petruvideo.core.model.engine.ApplySpeedPreset("c1", preset).apply(base) as
                com.petruvideo.core.model.engine.CommandResult.Applied).timeline
            val expected = ramped.videoLane.single().lengthMs
            val (state, uri) = export(ramped, settings, "pipeline-ramp-${preset.name}")
            state.shouldBeFinished()
            val r = retriever(uri!!)
            val actual = r.int(MediaMetadataRetriever.METADATA_KEY_DURATION)
            r.release()
            check(
                kotlin.math.abs(actual - expected) < 150,
            ) { "${preset.name}: exported $actual ms, the editor shows $expected ms" }
        }
    }

    /** SC-004 of feature 018: slowing the clip down does not lower the pitch of its sound. */
    @Test
    fun theSoundOfARampedClipKeepsItsPitch() {
        val a = item("video_with_audio.mp4")
        val length = minOf(3_000L, a.durationMs ?: 3_000L)
        val settings = ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller)
        val base = project(VideoClip("c1", a.id, 0, length, true), media = listOf(a))
        val ramped = (com.petruvideo.core.model.engine.ApplySpeedPreset("c1", com.petruvideo.core.model.SpeedPreset.Bullet).apply(base) as
            com.petruvideo.core.model.engine.CommandResult.Applied).timeline
        val (s1, plainUri) = export(base, settings, "pipeline-pitch-plain")
        s1.shouldBeFinished()
        val (s2, rampUri) = export(ramped, settings, "pipeline-pitch-ramp")
        s2.shouldBeFinished()
        val plainRate = zeroCrossRates(plainUri!!, listOf(0L to length))[0]
        val total = ramped.videoLane.single().lengthMs
        val windows = (0 until 4).map { (total * it / 4) to (total * (it + 1) / 4) }
        val rates = zeroCrossRates(rampUri!!, windows).filter { it > 0f }
        check(plainRate > 0f && rates.isNotEmpty()) { "no sound to measure" }
        val mean = rates.average().toFloat()
        check(mean > plainRate * 0.6f && mean < plainRate * 1.6f) { "pitch moved: plain $plainRate, ramped $rates" }
    }

    private fun withCanvas(canvas: com.petruvideo.core.model.CanvasBackground, name: String): android.graphics.Bitmap {
        val photo = solidPhoto("canvasphoto", 0xFF804000.toInt())
        val clip = VideoClip("k1", photo.id, 0, 2_000, false)
        val timeline = project(clip, media = listOf(photo)).copy(canvas = canvas)
        val (state, uri) = export(timeline, ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller), name)
        check(state is ExportState.Finished) { "$name export: $state" }
        return frameAt(uri!!, 800)
    }

    /** SC-002 of feature 021: the bars of a 4:3 picture in a 16:9 project show what the project's background says. */
    @Test
    fun theBarsShowTheProjectsBackground() {
        val black = withCanvas(com.petruvideo.core.model.CanvasBackground.Black, "pipeline-canvas-black")
        val side = meanRgb(black, 0.0f, 0.08f)
        check(side.all { it < 25 }) { "black bars are ${side.toList()}" }
        check(meanRgb(black, 0.4f, 0.6f)[0] > 90) { "the picture is missing" }

        val red = withCanvas(com.petruvideo.core.model.CanvasBackground.Colour(0xFFE53935.toInt()), "pipeline-canvas-red")
        val redSide = meanRgb(red, 0.0f, 0.08f)
        check(redSide[0] > 180 && redSide[1] < 90 && redSide[2] < 90) { "colour bars are ${redSide.toList()}" }
        check(meanRgb(red, 0.4f, 0.6f)[0] in 90..170) { "the picture changed under a colour" }

        val blur = withCanvas(
            com.petruvideo.core.model.CanvasBackground.Blur(com.petruvideo.core.model.BlurStrength.Medium),
            "pipeline-canvas-blur",
        )
        val blurSide = meanRgb(blur, 0.0f, 0.08f)
        // The picture is solid brown (128, 64, 0), so its blurred copy is brown a little darkened, not black.
        check(blurSide[0] in 60..125 && blurSide[0] > blurSide[2] + 30) { "blur bars are ${blurSide.toList()}" }
    }

    private fun greenScreenPhoto(): MediaItem {
        val file = File(context.cacheDir, "greenscreen.png")
        val bitmap = android.graphics.Bitmap.createBitmap(400, 300, android.graphics.Bitmap.Config.ARGB_8888)
        for (x in 0 until 400) for (y in 0 until 300) bitmap.setPixel(x, y, if (x < 200) 0xFF00B140.toInt() else 0xFF804000.toInt())
        file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        return runBlocking { MediaProbe(context).probe(Uri.fromFile(file), "greenscreen.png").getOrThrow() }
    }

    private fun keyedOverlayFrame(key: com.petruvideo.core.model.ChromaKey, name: String): android.graphics.Bitmap {
        val base = solidPhoto("keybase", 0xFF0040C0.toInt())
        val overlay = greenScreenPhoto()
        val timeline = project(VideoClip("b1", base.id, 0, 2_000, false), media = listOf(base)).let {
            it.copy(
                media = it.media + (overlay.id to overlay),
                overlayLane = listOf(OverlayClip("o1", overlay.id, 0, 2_000, 0, 500, 500, 600, chroma = key)),
            )
        }
        val (state, uri) = export(timeline, ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller), name)
        check(state is ExportState.Finished) { "$name export: $state" }
        return frameAt(uri!!, 800)
    }

    /** SC-002 of feature 022: the keyed backdrop shows the layer beneath; the rest of the overlay is unchanged. */
    @Test
    fun aKeyedOverlayShowsTheLayerBeneathWhereItWasTheKeyColour() {
        val keyed = keyedOverlayFrame(com.petruvideo.core.model.ChromaKey(enabled = true), "pipeline-chroma-on")
        val gone = meanRgb(keyed, 0.25f, 0.45f)
        check(gone[2] > 130 && gone[1] < 100) { "the green was not removed: ${gone.toList()}" }
        val kept = meanRgb(keyed, 0.55f, 0.75f)
        val plain = keyedOverlayFrame(com.petruvideo.core.model.ChromaKey(), "pipeline-chroma-off")
        val plainKept = meanRgb(plain, 0.55f, 0.75f)
        check(kept.indices.all { kotlin.math.abs(kept[it] - plainKept[it]) < 12 }) { "the kept side changed: ${kept.toList()} (no key ${plainKept.toList()})" }
        val green = meanRgb(plain, 0.25f, 0.45f)
        check(green[1] > 120 && green[2] < 110) { "a clip without the key changed: ${green.toList()}" }
    }

    private fun adjustedFrame(filter: com.petruvideo.core.model.ClipFilter, name: String): IntArray {
        val grey = solidPhoto("adjustgrey", 0xFF808080.toInt())
        val clip = VideoClip("g1", grey.id, 0, 2_000, false, filter = filter)
        val (state, uri) = export(
            project(clip, media = listOf(grey)), ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller), name,
        )
        check(state is ExportState.Finished) { "$name export: $state" }
        return meanRgb(frameAt(uri!!, 800), 0.2f, 0.8f)
    }

    /** SC-002 of feature 023: exposure raises a mid grey by about a stop; the other new sliders at 0 change nothing. */
    @Test
    fun exposureBrightensAndNeutralAdjustmentsChangeNothing() {
        val plain = adjustedFrame(com.petruvideo.core.model.ClipFilter(), "pipeline-adjust-plain")
        val neutralLook = adjustedFrame(com.petruvideo.core.model.ClipFilter(brightness = 1), "pipeline-adjust-neutral")
        check(kotlin.math.abs(neutralLook[0] - plain[0]) <= 4) { "neutral adjustments changed ${plain.toList()} to ${neutralLook.toList()}" }
        val bright = adjustedFrame(com.petruvideo.core.model.ClipFilter(exposure = 50), "pipeline-adjust-exposure")
        check(bright[0] > plain[0] + 30) { "exposure did not brighten: ${plain.toList()} to ${bright.toList()}" }
        val tinted = adjustedFrame(com.petruvideo.core.model.ClipFilter(tint = 100), "pipeline-adjust-tint")
        check(tinted[1] < plain[1] - 8 && tinted[0] > plain[0]) { "tint did not move green to magenta: ${tinted.toList()}" }
    }

    private fun maskedOverlayFrame(mask: com.petruvideo.core.model.OverlayMask, name: String): android.graphics.Bitmap {
        val base = solidPhoto("maskbase", 0xFF0040C0.toInt())
        val overlay = solidPhoto("maskoverlay", 0xFF804000.toInt())
        val timeline = project(VideoClip("b1", base.id, 0, 2_000, false), media = listOf(base)).let {
            it.copy(
                media = it.media + (overlay.id to overlay),
                overlayLane = listOf(OverlayClip("o1", overlay.id, 0, 2_000, 0, 500, 500, 600, mask = mask)),
            )
        }
        val (state, uri) = export(timeline, ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller), name)
        check(state is ExportState.Finished) { "$name export: $state" }
        return frameAt(uri!!, 800)
    }

    private fun boxRgb(frame: android.graphics.Bitmap, l: Float, t: Float, r: Float, b: Float): IntArray {
        val sum = LongArray(3)
        var n = 0
        for (y in (frame.height * t).toInt() until (frame.height * b).toInt()) {
            for (x in (frame.width * l).toInt() until (frame.width * r).toInt()) {
                val c = frame.getPixel(x, y)
                sum[0] += android.graphics.Color.red(c); sum[1] += android.graphics.Color.green(c)
                sum[2] += android.graphics.Color.blue(c)
                n++
            }
        }
        return intArrayOf((sum[0] / n).toInt(), (sum[1] / n).toInt(), (sum[2] / n).toInt())
    }

    /** SC-002 of feature 024: the corner of a Circle-masked overlay shows the layer beneath, the centre the overlay. */
    @Test
    fun aCircleMaskCutsTheCornersOfAnOverlay() {
        val kind = com.petruvideo.core.model.MaskKind.Circle
        val masked = maskedOverlayFrame(com.petruvideo.core.model.OverlayMask(kind, 100, 0), "pipeline-mask-circle")
        // The overlay spans 0.2..0.8 of the width and 0.1..0.9 of the height; its corners are outside the ellipse.
        val corner = boxRgb(masked, 0.21f, 0.12f, 0.27f, 0.2f)
        val centre = boxRgb(masked, 0.45f, 0.45f, 0.55f, 0.55f)
        check(corner[2] > 120 && corner[0] < 70) { "the corner still shows the overlay: ${corner.toList()}" }
        check(centre[0] > 70 && centre[2] < 80) { "the centre lost the overlay: ${centre.toList()}" }

        val plain = maskedOverlayFrame(com.petruvideo.core.model.OverlayMask(), "pipeline-mask-none")
        val plainCorner = boxRgb(plain, 0.21f, 0.12f, 0.27f, 0.2f)
        check(plainCorner[0] > 70 && plainCorner[2] < 80) { "a clip without a mask changed: ${plainCorner.toList()}" }

        val inverted = maskedOverlayFrame(
            com.petruvideo.core.model.OverlayMask(kind, 100, 0, true), "pipeline-mask-inverted",
        )
        check(boxRgb(inverted, 0.45f, 0.45f, 0.55f, 0.55f)[2] > 120) { "invert kept the centre" }
        check(boxRgb(inverted, 0.21f, 0.12f, 0.27f, 0.2f)[0] > 70) { "invert lost the corner" }
    }

    private fun hasHardwareAvcEncoder() = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.any {
        it.isEncoder && it.isHardwareAccelerated && it.supportedTypes.any { t -> t.equals("video/avc", true) }
    }

    private fun halvesPhoto(): MediaItem {
        val file = File(context.cacheDir, "halves.jpg")
        val bitmap = android.graphics.Bitmap.createBitmap(400, 300, android.graphics.Bitmap.Config.ARGB_8888)
        for (x in 0 until 400) for (y in 0 until 300) bitmap.setPixel(x, y, if (x < 200) 0xFF804000.toInt() else 0xFF0040C0.toInt())
        file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, it) }
        return runBlocking { MediaProbe(context).probe(Uri.fromFile(file), "halves.jpg").getOrThrow() }
    }

    /** Share of the width where the brown half ends, along the middle row. */
    private fun brownEdge(frame: android.graphics.Bitmap): Float {
        val y = frame.height / 2
        var edge = 0
        for (x in 0 until frame.width) {
            val c = frame.getPixel(x, y)
            if (android.graphics.Color.red(c) > android.graphics.Color.blue(c)) edge = x else if (edge > 0) break
        }
        return edge.toFloat() / frame.width
    }

    /** SC-003 of feature 014: zoom and pan of a video clip in the export follow the pose function at each moment. */
    @Test
    fun aVideoClipIsZoomedAndPannedWhereItsKeyframesPutIt() {
        val photo = halvesPhoto()
        val clip = VideoClip("z1", photo.id, 0, 3_000, false).withKeyframes(
            listOf(
                com.petruvideo.core.model.Keyframe(0, com.petruvideo.core.model.FRAMING_NEUTRAL),
                com.petruvideo.core.model.Keyframe(2_000, com.petruvideo.core.model.Pose(400, 500, 2_000, 100)),
            ),
        )
        val (state, uri) = export(project(clip, media = listOf(photo)), ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller), "pipeline-vkf")
        check(state is ExportState.Finished) { "video keyframe export: $state" }
        for (time in listOf(200L, 1_000L, 1_800L, 2_600L)) {
            val pose = clip.poseAt(time)
            val zoom = pose.sizePermille / 1000f
            val expected = ((0.5f - (pose.xPermille / 1000f - 0.5f / zoom)) * zoom).coerceIn(0f, 1f)
            val edge = brownEdge(frameAt(uri!!, time))
            check(kotlin.math.abs(edge - expected) < 0.05f) { "at $time ms the edge is $edge, the pose says $expected" }
        }
    }

    /** FR-012 of feature 014: an animated clip still crosses over into the next clip through its transition. */
    @Test
    fun anAnimatedClipTransitionsIntoTheNextClip() {
        val (plain, _, _) = twoPhotos(TransitionKind.Dissolve)
        val first = plain.videoLane[0].withKeyframes(
            listOf(
                com.petruvideo.core.model.Keyframe(0, com.petruvideo.core.model.FRAMING_NEUTRAL),
                com.petruvideo.core.model.Keyframe(2_000, com.petruvideo.core.model.Pose(500, 500, 2_000, 100)),
            ),
        )
        val timeline = plain.copy(videoLane = listOf(first, plain.videoLane[1]))
        val (state, uri) = export(timeline, ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller), "pipeline-vkf-tr")
        check(state is ExportState.Finished) { "animated transition export: $state" }
        val brown = meanRgb(frameAt(uri!!, 500), 0.2f, 0.8f)
        val blue = meanRgb(frameAt(uri, 4_200), 0.2f, 0.8f)
        val mid = meanRgb(frameAt(uri, 2_500), 0.2f, 0.8f)
        check(brown[0] > brown[2] + 40 && blue[2] > blue[0] + 40) { "ends ${brown.toList()} ${blue.toList()}" }
        check(mid[0] in (blue[0] + 5)..(brown[0] - 5)) { "mid ${mid.toList()} is not between the clips" }
    }

    @Test
    fun aVideoClipFadesToBlackWhereItsKeyframesFadeIt() {
        val photo = solidPhoto("fademe", 0xFFC0C0C0.toInt())
        val clip = VideoClip("f1", photo.id, 0, 3_000, false).withKeyframes(
            listOf(
                com.petruvideo.core.model.Keyframe(0, com.petruvideo.core.model.FRAMING_NEUTRAL),
                com.petruvideo.core.model.Keyframe(2_000, com.petruvideo.core.model.FRAMING_NEUTRAL.copy(opacityPercent = 0)),
            ),
        )
        val (state, uri) = export(project(clip, media = listOf(photo)), ExportSettings(Resolution.R720, FrameRate.F30, Quality.Smaller), "pipeline-vfade")
        check(state is ExportState.Finished) { "video fade export: $state" }
        val start = meanLuma(frameAt(uri!!, 100))
        val middle = meanLuma(frameAt(uri, 1_000))
        val end = meanLuma(frameAt(uri, 2_600))
        check(start > 100f && middle in (start * 0.3f)..(start * 0.7f) && end < 25f) { "luma $start $middle $end" }
    }

    private fun ExportState.shouldBeFinished() {
        (this is ExportState.Finished) shouldBe true
    }
}
