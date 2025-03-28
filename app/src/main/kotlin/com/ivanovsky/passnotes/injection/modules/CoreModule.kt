package com.ivanovsky.passnotes.injection.modules

import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.crypto.DataCipherProvider
import com.ivanovsky.passnotes.data.crypto.DataCipherProviderImpl
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository
import com.ivanovsky.passnotes.data.repository.RemoteFileRepository
import com.ivanovsky.passnotes.data.repository.UsedFileRepository
import com.ivanovsky.passnotes.data.repository.db.AppDatabase
import com.ivanovsky.passnotes.data.repository.file.saf.SAFHelper
import com.ivanovsky.passnotes.data.repository.keepass.DatabaseSyncStateProvider
import com.ivanovsky.passnotes.data.repository.keepass.KeepassDatabaseRepository
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl
import com.ivanovsky.passnotes.domain.DateFormatProvider
import com.ivanovsky.passnotes.domain.DateFormatter
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.FileHelper
import com.ivanovsky.passnotes.domain.LocaleProvider
import com.ivanovsky.passnotes.domain.LoggerInteractor
import com.ivanovsky.passnotes.domain.NoteDiffer
import com.ivanovsky.passnotes.domain.PermissionHelper
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.interactor.SelectionHolder
import com.ivanovsky.passnotes.presentation.core.ThemeProvider
import org.koin.dsl.module

object CoreModule {

    fun build(loggerInteractor: LoggerInteractor) =
        module {
            single { loggerInteractor }
            single { ThemeProvider(get()) }
            single { ResourceProvider(get(), get()) }
            single { PermissionHelper(get()) }
            single { LocaleProvider(get()) }
            single { DispatcherProvider() }
            single { ObserverBus() }
            single { DateFormatProvider(get(), get()) }
            single { DateFormatter(get()) }
            single { NoteDiffer() }
            single { SelectionHolder() }
            single<Settings> { SettingsImpl(get()) }
            single<DataCipherProvider> { DataCipherProviderImpl(get()) }
            single { FileHelper(get(), get()) }
            single { SAFHelper(get()) }

            // Database
            single { AppDatabase.buildDatabase(get(), get()) }
            single { provideRemoteFileRepository(get()) }
            single { provideUsedFileRepository(get(), get()) }
            single { provideGitRootDao(get()) }

            // Files, Keepass
            single { DatabaseSyncStateProvider(get(), get(), get()) }
            single<EncryptedDatabaseRepository> {
                KeepassDatabaseRepository(get(), get(), get(), get())
            }
        }

    private fun provideRemoteFileRepository(database: AppDatabase) =
        RemoteFileRepository(database.remoteFileDao)

    private fun provideUsedFileRepository(database: AppDatabase, observerBus: ObserverBus) =
        UsedFileRepository(database.usedFileDao, observerBus)

    private fun provideGitRootDao(database: AppDatabase) =
        database.gitRootDao
}