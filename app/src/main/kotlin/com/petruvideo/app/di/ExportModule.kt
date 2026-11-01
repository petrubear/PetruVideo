package com.petruvideo.app.di

import android.content.Context
import com.petruvideo.core.data.repo.ExportRepository
import com.petruvideo.core.export.ContextExportServiceStarter
import com.petruvideo.core.export.EncoderCapabilities
import com.petruvideo.core.export.ExportCoordinator
import com.petruvideo.core.export.ExportRecovery
import com.petruvideo.core.export.ExportRunner
import com.petruvideo.core.export.ExportSaver
import com.petruvideo.core.export.ExportServiceStarter
import com.petruvideo.core.export.GallerySaver
import com.petruvideo.core.export.MediaCodecEncoderCapabilities
import com.petruvideo.core.export.StorageInfo
import com.petruvideo.core.export.TransformerRunner
import com.petruvideo.core.media.reverse.ReversePreparer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope

/** Bindings for `core:export`: one coordinator per process, so a render outlives the screen. */
@Module
@InstallIn(SingletonComponent::class)
object ExportModule {
    @Provides
    @Singleton
    fun encoderCapabilities(): EncoderCapabilities = MediaCodecEncoderCapabilities()

    @Provides
    fun exportRunner(@ApplicationContext context: Context, reverse: ReversePreparer): ExportRunner =
        TransformerRunner(context, reverse)

    @Provides
    fun exportSaver(@ApplicationContext context: Context): ExportSaver = GallerySaver(context)

    @Provides
    fun serviceStarter(@ApplicationContext context: Context): ExportServiceStarter =
        ContextExportServiceStarter(context)

    @Provides
    fun storageInfo(@ApplicationContext context: Context): StorageInfo = StorageInfo { context.cacheDir.usableSpace }

    @Provides
    @Singleton
    fun exportCoordinator(
        runner: ExportRunner,
        saver: ExportSaver,
        repository: ExportRepository,
        encoders: EncoderCapabilities,
        storage: StorageInfo,
        @ApplicationContext context: Context,
        @ApplicationScope scope: CoroutineScope,
    ) = ExportCoordinator(runner, saver, repository, encoders, storage, File(context.cacheDir, "exports"), scope)

    @Provides
    fun exportRecovery(repository: ExportRepository, @ApplicationContext context: Context) =
        ExportRecovery(repository, File(context.cacheDir, "exports"))
}
