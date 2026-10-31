package com.petruvideo.feature.importmedia

import com.petruvideo.core.model.MediaKind
import io.kotest.matchers.shouldBe
import org.junit.Test

class PermissionCoordinatorTest {
    private val images = "android.permission.READ_MEDIA_IMAGES"
    private val video = "android.permission.READ_MEDIA_VIDEO"
    private val audio = "android.permission.READ_MEDIA_AUDIO"
    private val selected = "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"
    private val legacy = "android.permission.READ_EXTERNAL_STORAGE"

    @Test
    fun `android 14 asks for the kind permission plus partial access`() {
        PermissionCoordinator.permissionsFor(MediaKind.Video, sdk = 34) shouldBe listOf(video, selected)
        PermissionCoordinator.permissionsFor(MediaKind.Photo, sdk = 34) shouldBe listOf(images, selected)
        PermissionCoordinator.permissionsFor(MediaKind.Audio, sdk = 34) shouldBe listOf(audio)
    }

    @Test
    fun `android 13 asks for the kind permission only`() {
        PermissionCoordinator.permissionsFor(MediaKind.Video, sdk = 33) shouldBe listOf(video)
        PermissionCoordinator.permissionsFor(MediaKind.Audio, sdk = 33) shouldBe listOf(audio)
    }

    @Test
    fun `older versions use the storage permission`() {
        PermissionCoordinator.permissionsFor(MediaKind.Photo, sdk = 29) shouldBe listOf(legacy)
        PermissionCoordinator.permissionsFor(MediaKind.Audio, sdk = 26) shouldBe listOf(legacy)
    }

    @Test
    fun `granted access when the main permission is held`() {
        PermissionCoordinator.accessFrom(MediaKind.Video, 34, setOf(video, selected), false) shouldBe
            MediaAccess.Granted
        PermissionCoordinator.accessFrom(MediaKind.Photo, 30, setOf(legacy), false) shouldBe MediaAccess.Granted
    }

    @Test
    fun `partial access when only selected items are shared`() {
        PermissionCoordinator.accessFrom(MediaKind.Photo, 34, setOf(selected), true) shouldBe MediaAccess.Partial
    }

    @Test
    fun `not asked before the first request and denied after it`() {
        PermissionCoordinator.accessFrom(MediaKind.Video, 34, emptySet(), requestedBefore = false) shouldBe
            MediaAccess.NotAsked
        PermissionCoordinator.accessFrom(MediaKind.Video, 34, emptySet(), requestedBefore = true) shouldBe
            MediaAccess.Denied
    }

    @Test
    fun `audio ignores partial access`() {
        PermissionCoordinator.accessFrom(MediaKind.Audio, 34, setOf(selected), true) shouldBe MediaAccess.Denied
    }
}
