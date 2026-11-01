package com.petruvideo.core.data.db

import com.petruvideo.core.model.AnimKind
import com.petruvideo.core.model.AudioClip
import com.petruvideo.core.model.AudioFx
import com.petruvideo.core.model.AudioOrigin
import com.petruvideo.core.model.Availability
import com.petruvideo.core.model.Bass
import com.petruvideo.core.model.ClipFilter
import com.petruvideo.core.model.Echo
import com.petruvideo.core.model.EqPreset
import com.petruvideo.core.model.Equalizer
import com.petruvideo.core.model.Fade
import com.petruvideo.core.model.FilterLook
import com.petruvideo.core.model.Fingerprint
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.NoiseReduction
import com.petruvideo.core.model.NoiseStrength
import com.petruvideo.core.model.OverlayClip
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
)

fun VideoClip.toEntity(projectId: String, orderIndex: Int) =
    VideoClipEntity(
        id, projectId, mediaItemId, orderIndex, sourceStartMs, sourceEndMs, attachedAudio, speedMilli, volumePercent,
        muted, reverb.toColumns(), fx.toColumns(), filter.toColumns(),
    )

fun AudioClipEntity.toModel() = AudioClip(
    id, mediaItemId, sourceStartMs, sourceEndMs, timelineStartMs, muted, AudioOrigin.valueOf(origin),
    speedMilli, volumePercent, reverb.toModel(), fx.toModel(),
)

fun AudioClip.toEntity(projectId: String) = AudioClipEntity(
    id, projectId, mediaItemId, timelineStartMs, sourceStartMs, sourceEndMs, muted, origin.name,
    speedMilli, volumePercent, reverb.toColumns(), fx.toColumns(),
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
    )
}

fun AudioFx.toColumns() = AudioFxColumns(
    fade.inMs, fade.outMs, noise.enabled, noise.strength.name, eq.lowDb, eq.midDb, eq.highDb,
    voice.echo.enabled, voice.echo.delayMs, voice.echo.repeats, voice.robot.enabled, voice.robot.intensity,
    voice.pitch.enabled, voice.pitch.semitones, voice.bass.enabled, voice.bass.amount,
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
    ),
    animIn = TextAnimation(AnimKind.entries.firstOrNull { it.name == inKind } ?: AnimKind.None, inMs),
    animOut = TextAnimation(AnimKind.entries.firstOrNull { it.name == outKind } ?: AnimKind.None, outMs),
)

fun TextClip.toEntity(projectId: String) = TextClipEntity(
    id, projectId, startMs, durationMs, text, xPermille, yPermille, sizePermille,
    style.font.name, style.colorArgb, style.align.name,
    style.background.enabled, style.background.colorArgb, style.background.opacityPercent,
    style.outline.enabled, style.outline.colorArgb, style.outline.widthPermille,
    animIn.kind.name, animIn.lengthMs, animOut.kind.name, animOut.lengthMs,
)

fun FilterColumns.toModel() = ClipFilter(
    FilterLook.entries.firstOrNull { it.name == look } ?: FilterLook.None,
    intensity, brightness, contrast, saturation, warmth, vignette,
)

fun ClipFilter.toColumns() = FilterColumns(look.name, intensity, brightness, contrast, saturation, warmth, vignette)

fun OverlayClipEntity.toModel() =
    OverlayClip(id, mediaItemId, sourceStartMs, sourceEndMs, startMs, x, y, width, opacity)

fun OverlayClip.toEntity(projectId: String) =
    OverlayClipEntity(
        id, projectId, mediaItemId, startMs, sourceStartMs, sourceEndMs,
        xPermille, yPermille, widthPermille, opacityPercent,
    )
