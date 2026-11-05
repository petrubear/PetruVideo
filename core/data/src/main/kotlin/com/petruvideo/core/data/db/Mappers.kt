package com.petruvideo.core.data.db

import com.petruvideo.core.model.AnimKind
import com.petruvideo.core.model.AudioClip
import com.petruvideo.core.model.BlurStrength
import com.petruvideo.core.model.CanvasBackground
import com.petruvideo.core.model.AudioFx
import com.petruvideo.core.model.AudioOrigin
import com.petruvideo.core.model.Availability
import com.petruvideo.core.model.Bass
import com.petruvideo.core.model.ClipEffect
import com.petruvideo.core.model.ClipTransform
import com.petruvideo.core.model.SpeedCurve
import com.petruvideo.core.model.CropBox
import com.petruvideo.core.model.ClipFilter
import com.petruvideo.core.model.EffectKind
import com.petruvideo.core.model.Transition
import com.petruvideo.core.model.TransitionKind
import com.petruvideo.core.model.DuckSmoothness
import com.petruvideo.core.model.Ducking
import com.petruvideo.core.model.Echo
import com.petruvideo.core.model.Compressor
import com.petruvideo.core.model.CompressorPreset
import com.petruvideo.core.model.EqPreset
import com.petruvideo.core.model.Equalizer
import com.petruvideo.core.model.Fade
import com.petruvideo.core.model.FilterLook
import com.petruvideo.core.model.Fingerprint
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.NoiseReduction
import com.petruvideo.core.model.NoiseStrength
import com.petruvideo.core.model.Keyframe
import com.petruvideo.core.model.ChromaKey
import com.petruvideo.core.model.MaskKind
import com.petruvideo.core.model.OverlayClip
import com.petruvideo.core.model.OverlayMask
import com.petruvideo.core.model.Pose
import com.petruvideo.core.model.Pitch
import com.petruvideo.core.model.Project
import com.petruvideo.core.model.ProjectFormat
import com.petruvideo.core.model.ProjectStatus
import com.petruvideo.core.model.Reverb
import com.petruvideo.core.model.ReverbPreset
import com.petruvideo.core.model.Robot
import com.petruvideo.core.model.TextAlign
import com.petruvideo.core.model.TextAnimation
import com.petruvideo.core.model.TextBackground
import com.petruvideo.core.model.TextClip
import com.petruvideo.core.model.TextFont
import com.petruvideo.core.model.TextOutline
import com.petruvideo.core.model.TextShadow
import com.petruvideo.core.model.TextStyle
import com.petruvideo.core.model.VideoClip
import com.petruvideo.core.model.VoiceFx

fun ProjectEntity.toModel() = Project(
    id = id,
    name = name,
    createdAt = createdAt,
    modifiedAt = modifiedAt,
    status = ProjectStatus.valueOf(status),
    format = if (formatWidth != null && formatHeight != null && formatFps != null) {
        ProjectFormat(formatWidth, formatHeight, formatFps)
    } else {
        null
    },
    thumbnailRef = thumbnailRef,
)

fun Project.toEntity(playheadMs: Long) = ProjectEntity(
    id = id,
    name = name,
    createdAt = createdAt,
    modifiedAt = modifiedAt,
    status = status.name,
    formatWidth = format?.width,
    formatHeight = format?.height,
    formatFps = format?.fps,
    thumbnailRef = thumbnailRef,
    playheadMs = playheadMs,
)

fun MediaItemEntity.toModel() = MediaItem(
    id = id,
    kind = MediaKind.valueOf(kind),
    uri = uri,
    displayName = displayName,
    durationMs = durationMs,
    width = width,
    height = height,
    frameRate = frameRate,
    hasAudio = hasAudio,
    fingerprint = Fingerprint(fpSize, fpLastModified, durationMs, fpMimeType),
    availability = Availability.Available,
)

fun MediaItem.toEntity(projectId: String) = MediaItemEntity(
    id = id,
    projectId = projectId,
    kind = kind.name,
    uri = uri,
    displayName = displayName,
    durationMs = durationMs,
    width = width,
    height = height,
    frameRate = frameRate,
    hasAudio = hasAudio,
    fpSize = fingerprint.sizeBytes,
    fpLastModified = fingerprint.lastModified,
    fpMimeType = fingerprint.mimeType,
)

fun VideoClipEntity.toModel() = VideoClip(
    id, mediaItemId, sourceStartMs, sourceEndMs, attachedAudio, speedMilli, volumePercent, muted = muted,
    reverb = reverb.toModel(), fx = fx.toModel(), filter = filter.toModel(),
    transitionOut = transition.toModel(),
    effects = listOfNotNull(effect.toModel(), effect2.toModel(), effect3.toModel()),
    transform = transform.toModel(),
    reversed = reversed,
    speedCurve = SpeedCurve.decode(speedCurve)?.takeIf { it.sourceLengthMs == sourceEndMs - sourceStartMs },
)

fun VideoClip.toEntity(projectId: String, orderIndex: Int) =
    VideoClipEntity(
        id, projectId, mediaItemId, orderIndex, sourceStartMs, sourceEndMs, attachedAudio, speedMilli, volumePercent,
        muted, reverb.toColumns(), fx.toColumns(), filter.toColumns(),
        transitionOut.toColumns(), effects.getOrNull(0).toColumns(), effects.getOrNull(1).toColumns(),
        effects.getOrNull(2).toColumns(), transform.toColumns(), reversed,
        speedCurve?.let(SpeedCurve::encode),
    )

fun TransformColumns.toModel() =
    ClipTransform(turns.coerceIn(0, 3), flipH, flipV, CropBox(cropLeft, cropTop, cropRight, cropBottom).normalized())

fun ClipTransform.toColumns() =
    TransformColumns(quarterTurns, flipH, flipV, crop.left, crop.top, crop.right, crop.bottom)

fun AudioClipEntity.toModel() = AudioClip(
    id, mediaItemId, sourceStartMs, sourceEndMs, timelineStartMs, muted, AudioOrigin.valueOf(origin),
    speedMilli, volumePercent, reverb.toModel(), fx.toModel(),
    Ducking(
        duckEnabled, duckAmount,
        DuckSmoothness.entries.firstOrNull { it.name == duckSmoothness } ?: DuckSmoothness.Medium,
    ),
)

fun AudioClip.toEntity(projectId: String) = AudioClipEntity(
    id, projectId, mediaItemId, timelineStartMs, sourceStartMs, sourceEndMs, muted, origin.name,
    speedMilli, volumePercent, reverb.toColumns(), fx.toColumns(),
    ducking.enabled, ducking.amountPercent, ducking.smoothness.name,
)

fun ReverbColumns.toModel(): Reverb {
    val known = ReverbPreset.entries.firstOrNull { it.name == preset } ?: ReverbPreset.Custom
    return Reverb(enabled, known, room, decayMs, damping, preDelayMs, mix)
}

fun Reverb.toColumns() = ReverbColumns(enabled, preset.name, roomSize, decayMs, damping, preDelayMs, mix)

fun AudioFxColumns.toModel(): AudioFx {
    val strength = NoiseStrength.entries.firstOrNull { it.name == nrStrength } ?: NoiseStrength.Medium
    return AudioFx(
        fade = Fade(fadeInMs, fadeOutMs),
        noise = NoiseReduction(nrEnabled, strength),
        eq = Equalizer(EqPreset.Custom, eqLow, eqMid, eqHigh).normalized(),
        voice = VoiceFx(
            Echo(echoEnabled, echoDelayMs, echoRepeats),
            Robot(robotEnabled, robotIntensity),
            Pitch(pitchEnabled, pitchSemitones),
            Bass(bassEnabled, bassAmount),
        ),
        compressor = Compressor(
            cmpEnabled,
            CompressorPreset.entries.firstOrNull { it.name == cmpPreset } ?: CompressorPreset.Custom,
            cmpThreshold, cmpRatioTenths, cmpAttackMs, cmpReleaseMs, cmpLevel,
        ).normalized(),
    )
}

fun AudioFx.toColumns() = AudioFxColumns(
    fade.inMs, fade.outMs, noise.enabled, noise.strength.name, eq.lowDb, eq.midDb, eq.highDb,
    voice.echo.enabled, voice.echo.delayMs, voice.echo.repeats, voice.robot.enabled, voice.robot.intensity,
    voice.pitch.enabled, voice.pitch.semitones, voice.bass.enabled, voice.bass.amount,
    compressor.enabled, compressor.preset.name, compressor.thresholdDb, compressor.ratioTenths, compressor.attackMs,
    compressor.releaseMs, compressor.levelDb,
)

fun TextClipEntity.toModel() = TextClip(
    id = id, text = text, startMs = startMs, durationMs = durationMs,
    xPermille = x, yPermille = y, sizePermille = size,
    style = TextStyle(
        font = TextFont.entries.firstOrNull { it.name == font } ?: TextFont.Sans,
        colorArgb = color,
        align = TextAlign.entries.firstOrNull { it.name == align } ?: TextAlign.Center,
        background = TextBackground(bgEnabled, bgColor, bgOpacity),
        outline = TextOutline(olEnabled, olColor, olWidth),
        shadow = TextShadow(shEnabled, shColor, shOpacity, shSoftness, shDistance, shDirection),
        rotationDegrees = rotation,
        letterSpacingPercent = letterSpacing,
        lineSpacingPercent = lineSpacing,
    ).normalized(),
    animIn = TextAnimation(AnimKind.entries.firstOrNull { it.name == inKind } ?: AnimKind.None, inMs),
    animOut = TextAnimation(AnimKind.entries.firstOrNull { it.name == outKind } ?: AnimKind.None, outMs),
    caption = caption,
)

fun TextClip.toEntity(projectId: String) = TextClipEntity(
    id, projectId, startMs, durationMs, text, xPermille, yPermille, sizePermille,
    style.font.name, style.colorArgb, style.align.name,
    style.background.enabled, style.background.colorArgb, style.background.opacityPercent,
    style.outline.enabled, style.outline.colorArgb, style.outline.widthPermille,
    animIn.kind.name, animIn.lengthMs, animOut.kind.name, animOut.lengthMs, caption,
    style.rotationDegrees, style.shadow.enabled, style.shadow.colorArgb, style.shadow.opacityPercent,
    style.shadow.softnessPercent, style.shadow.distancePercent, style.shadow.directionDegrees,
    style.letterSpacingPercent, style.lineSpacingPercent,
)

fun FilterColumns.toModel() = ClipFilter(
    FilterLook.entries.firstOrNull { it.name == look } ?: FilterLook.None,
    intensity, brightness, contrast, saturation, warmth, vignette,
    exposure, gamma, highlights, shadows, tint, hue, sharpen,
)

fun ClipFilter.toColumns() = FilterColumns(
    look.name, intensity, brightness, contrast, saturation, warmth, vignette,
    exposure, gamma, highlights, shadows, tint, hue, sharpen,
)

fun KeyframeEntity.toModel() = Keyframe(timeMs, Pose(x, y, size, opacity), ease)

fun Keyframe.toEntity(projectId: String, ownerId: String) =
    KeyframeEntity(
        projectId = projectId, ownerId = ownerId, timeMs = timeMs, x = pose.xPermille, y = pose.yPermille,
        size = pose.sizePermille, opacity = pose.opacityPercent, ease = ease,
    )

fun OverlayClipEntity.toModel() =
    OverlayClip(
        id, mediaItemId, sourceStartMs, sourceEndMs, startMs, x, y, width, opacity,
        chroma = ChromaKey(chromaEnabled, chromaColor, chromaStrength, chromaSmoothness, chromaSpill),
        mask = OverlayMask(
            MaskKind.entries.firstOrNull { it.name == maskKind } ?: MaskKind.None, maskSize, maskFeather, maskInvert,
        ),
        attachedAudio = attachedAudio, muted = muted, volumePercent = volumePercent,
        reverb = reverb.toModel(), fx = fx.toModel(),
    )

fun OverlayClip.toEntity(projectId: String) =
    OverlayClipEntity(
        id, projectId, mediaItemId, startMs, sourceStartMs, sourceEndMs,
        xPermille, yPermille, widthPermille, opacityPercent,
        chroma.enabled, chroma.color, chroma.strength, chroma.smoothness, chroma.spill,
        mask.kind.name, mask.size, mask.feather, mask.invert,
        attachedAudio, muted, volumePercent, reverb.toColumns(), fx.toColumns(),
    )

fun TransitionColumns.toModel(): Transition? =
    TransitionKind.entries.firstOrNull { it.name == kind }?.let { Transition(it, lengthMs) }

fun Transition?.toColumns() = if (this == null) TransitionColumns() else TransitionColumns(kind.name, lengthMs)

fun EffectColumns.toModel(): ClipEffect? =
    EffectKind.entries.firstOrNull { it.name == kind }?.let { ClipEffect(it, strength) }

fun ClipEffect?.toColumns() = if (this == null) EffectColumns() else EffectColumns(kind.name, strength)

/** A reversed clip of any project: its media and the source range it plays. */
class ReversedRange(val media: com.petruvideo.core.model.MediaItem, val startMs: Long, val endMs: Long)

/** Every reversed clip of every project, so unused backwards copies can be removed. */
suspend fun PetruDatabase.reversedRanges(): List<ReversedRange> {
    val media = timelineDao().allMediaItems().associate { it.id to it.toModel() }
    return timelineDao().reversedClips().mapNotNull { clip ->
        media[clip.mediaItemId]?.let { ReversedRange(it, clip.sourceStartMs, clip.sourceEndMs) }
    }
}

/** The project's background as stored in its row. */
fun ProjectEntity.canvas(): CanvasBackground = when (canvasKind) {
    "Colour" -> CanvasBackground.Colour(canvasColor)
    "Blur" -> CanvasBackground.Blur(BlurStrength.entries.firstOrNull { it.name == canvasBlur } ?: BlurStrength.Medium)
    else -> CanvasBackground.Black
}

fun ProjectEntity.withCanvas(canvas: CanvasBackground): ProjectEntity = when (canvas) {
    CanvasBackground.Black -> copy(canvasKind = "Black")
    is CanvasBackground.Colour -> copy(canvasKind = "Colour", canvasColor = canvas.argb)
    is CanvasBackground.Blur -> copy(canvasKind = "Blur", canvasBlur = canvas.strength.name)
}
