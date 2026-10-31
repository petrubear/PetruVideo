package com.petruvideo.app.di

import com.petruvideo.core.media.library.MediaLibrary
import com.petruvideo.core.media.library.MediaLibrarySource
import com.petruvideo.core.media.probe.MediaProbe
import com.petruvideo.core.media.probe.MediaProber
import dagger.Binds
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
}
