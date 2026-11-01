package com.petruvideo.app.di

import android.content.Context
import androidx.room.Room
import com.petruvideo.core.data.db.PetruDatabase
import com.petruvideo.core.data.importing.ImportMediaUseCase
import com.petruvideo.core.data.importing.MediaImporter
import com.petruvideo.core.data.repo.ExportRepository
import com.petruvideo.core.data.repo.ProjectRepository
import com.petruvideo.core.data.repo.PrefsSettingsRepository
import com.petruvideo.core.data.repo.RoomExportRepository
import com.petruvideo.core.data.repo.RoomProjectRepository
import com.petruvideo.core.data.repo.SettingsRepository
import com.petruvideo.core.data.session.ProjectSession
import com.petruvideo.core.media.probe.MediaProber
import com.petruvideo.core.media.voiceover.VoiceOverStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    @ApplicationScope
    fun applicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): PetruDatabase =
        Room.databaseBuilder(context, PetruDatabase::class.java, PetruDatabase.NAME).build()

    @Provides
    @Singleton
    fun projectRepository(db: PetruDatabase, recordings: VoiceOverStore): ProjectRepository =
        RoomProjectRepository(db, onDeleted = { used -> recordings.sweep(used) })

    @Provides
    @Singleton
    fun exportRepository(db: PetruDatabase): ExportRepository = RoomExportRepository(db.exportDao())

    @Provides
    @Singleton
    fun settingsRepository(@ApplicationContext context: Context): SettingsRepository =
        PrefsSettingsRepository(context.getSharedPreferences("petruvideo-settings", Context.MODE_PRIVATE))

    @Provides
    fun mediaImporter(prober: MediaProber): MediaImporter = ImportMediaUseCase(prober)

    /** Process-scoped, so the open project survives fold, unfold, rotation, and resize. */
    @Provides
    @Singleton
    fun projectSession(repository: ProjectRepository, @ApplicationScope scope: CoroutineScope) =
        ProjectSession(repository, scope)
}
