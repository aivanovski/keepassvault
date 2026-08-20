package com.ivanovsky.passnotes.injection.modules

import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.crypto.DataCipherProvider
import com.ivanovsky.passnotes.data.crypto.DataCipherProviderImpl
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository
import com.ivanovsky.passnotes.data.repository.RemoteFileRepository
import com.ivanovsky.passnotes.data.repository.TemporaryFileRepository
import com.ivanovsky.passnotes.data.repository.UsedFileRepository
import com.ivanovsky.passnotes.data.repository.db.AppDatabase
import com.ivanovsky.passnotes.data.repository.file.saf.SAFHelper
import com.ivanovsky.passnotes.data.repository.keepass.DatabaseSyncStateProvider
import com.ivanovsky.passnotes.data.repository.keepass.KeepassDatabaseRepository
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.domain.DateFormatProvider
import com.ivanovsky.passnotes.domain.DateFormatter
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.DispatcherProviderImpl
import com.ivanovsky.passnotes.domain.FileHelper
import com.ivanovsky.passnotes.domain.LocaleProvider
import com.ivanovsky.passnotes.domain.NoteDiffer
import com.ivanovsky.passnotes.domain.PermissionHelper
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.interactor.SelectionHolder
import com.ivanovsky.passnotes.injection.AppStartDependencies
import com.ivanovsky.passnotes.presentation.core.ThemeProvider
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

object CoreModule {

    fun build(
        appStartDeps: AppStartDependencies
    ) = module {
        single { appStartDeps.loggerInteractor }
        singleOf(::ThemeProvider)
        single { ResourceProvider(get(), get()) }
        singleOf(::PermissionHelper)
        singleOf(::LocaleProvider)
        singleOf(::DispatcherProviderImpl).bind(DispatcherProvider::class)
        singleOf(::ObserverBus)
        singleOf(::DateFormatProvider)
        singleOf(::DateFormatter)
        singleOf(::NoteDiffer)
        singleOf(::SelectionHolder)
        single<Settings> { appStartDeps.settings }
        single { appStartDeps.crashReporterInteractor }
        singleOf(::DataCipherProviderImpl).bind(DataCipherProvider::class)
        singleOf(::FileHelper)
        singleOf(::SAFHelper)

        // Database
        single { AppDatabase.buildDatabase(get(), get()) }
        // DAO's
        singleOf(::provideRemoteFileDao)
        singleOf(::provideGitRootDao)
        singleOf(::provideTemporaryFileDao)
        singleOf(::provideUsedFileDao)
        // Repositories
        singleOf(::RemoteFileRepository)
        singleOf(::UsedFileRepository)
        singleOf(::TemporaryFileRepository)

        // Files, Keepass
        singleOf(::DatabaseSyncStateProvider)
        singleOf(::KeepassDatabaseRepository).bind(EncryptedDatabaseRepository::class)
    }

    private fun provideGitRootDao(database: AppDatabase) =
        database.gitRootDao

    private fun provideTemporaryFileDao(database: AppDatabase) =
        database.temporaryFileDao

    private fun provideUsedFileDao(database: AppDatabase) =
        database.usedFileDao

    private fun provideRemoteFileDao(database: AppDatabase) =
        database.remoteFileDao
}