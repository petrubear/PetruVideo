package com.petruvideo.app.di

import com.petruvideo.core.media.library.MediaLibrary
import com.petruvideo.core.media.library.MediaLibrarySource
import com.petruvideo.core.media.probe.MediaProbe
import com.petruvideo.core.media.probe.MediaProber
import android.content.Context
import com.petruvideo.core.data.repo.ProjectRepository
import com.petruvideo.core.media.MissingMediaChecker
import com.petruvideo.core.media.pressure.ResourcePressureMonitor
import com.petruvideo.core.media.preview.MediaAvailabilityChecker
import com.petruvideo.core.media.preview.Media3PreviewEngine
import com.petruvideo.core.media.preview.PreviewEngine
import com.petruvideo.core.media.preview.ThumbnailRefresher
import com.petruvideo.core.media.thumbs.ProjectThumbnailUpdater
import com.petruvideo.core.media.thumbs.ThumbnailStripGenerator
import dagger.Binds
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Bindings for `core:media`; the preview engine is added with US3. */
@Module
@InstallIn(SingletonComponent::class)
abstract class MediaModule {
    @Binds
    abstract fun mediaProber(impl: MediaProbe): MediaProber

    @Binds
    abstract fun mediaLibrary(impl: MediaLibrarySource): MediaLibrary

    @Binds
    abstract fun previewEngine(impl: Media3PreviewEngine): PreviewEngine

    @Binds
    abstract fun availabilityChecker(impl: MissingMediaChecker): MediaAvailabilityChecker

    companion object {
        @Provides
        @Singleton
        fun resourcePressureMonitor(@ApplicationContext context: Context): ResourcePressureMonitor =
            ResourcePressureMonitor().also { it.start(context) }

        @Provides
        fun thumbnailRefresher(
            @ApplicationContext context: Context,
            generator: ThumbnailStripGenerator,
            repository: ProjectRepository,
        ): ThumbnailRefresher = ProjectThumbnailUpdater(context, generator, repository::setThumbnail)
    }
}
