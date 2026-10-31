package com.petruvideo.core.data.db

import com.petruvideo.core.model.AudioClip
import com.petruvideo.core.model.AudioOrigin
import com.petruvideo.core.model.Availability
import com.petruvideo.core.model.Fingerprint
import com.petruvideo.core.model.MediaItem
import com.petruvideo.core.model.MediaKind
import com.petruvideo.core.model.Project
import com.petruvideo.core.model.ProjectFormat
import com.petruvideo.core.model.ProjectStatus
import com.petruvideo.core.model.VideoClip

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

fun VideoClipEntity.toModel() =
    VideoClip(id, mediaItemId, sourceStartMs, sourceEndMs, attachedAudio, speedMilli, volumePercent)

fun VideoClip.toEntity(projectId: String, orderIndex: Int) =
    VideoClipEntity(
        id, projectId, mediaItemId, orderIndex, sourceStartMs, sourceEndMs, attachedAudio, speedMilli, volumePercent,
    )

fun AudioClipEntity.toModel() = AudioClip(
    id, mediaItemId, sourceStartMs, sourceEndMs, timelineStartMs, muted, AudioOrigin.valueOf(origin),
    speedMilli, volumePercent,
)

fun AudioClip.toEntity(projectId: String) = AudioClipEntity(
    id, projectId, mediaItemId, timelineStartMs, sourceStartMs, sourceEndMs, muted, origin.name,
    speedMilli, volumePercent,
)
