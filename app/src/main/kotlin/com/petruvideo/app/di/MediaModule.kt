package com.petruvideo.app.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Bindings for `core:media` (probe, library source, preview engine) are added as those classes land. */
@Module
@InstallIn(SingletonComponent::class)
object MediaModule
