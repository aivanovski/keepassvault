package com.ivanovsky.passnotes.injection.modules

import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.crypto.DataCipherProvider
import com.ivanovsky.passnotes.data.crypto.DataCipherProviderImpl
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository
import com.ivanovsky.passnotes.data.repository.RemoteFileRepository
import com.ivanovsky.passnotes.data.repository.UsedFileRepository
import com.ivanovsky.passnotes.data.repository.db.AppDatabase
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.data.repository.keepass.KeepassDatabaseRepository
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl
import com.ivanovsky.passnotes.domain.ClipboardHelper
import com.ivanovsky.passnotes.domain.DateFormatProvider
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.FileHelper
import com.ivanovsky.passnotes.domain.LocaleProvider
import com.ivanovsky.passnotes.domain.LoggerInteractor
import com.ivanovsky.passnotes.domain.NoteDiffer
import com.ivanovsky.passnotes.domain.PermissionHelper
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.SelectionHolder
import okhttp3.OkHttp
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import timber.log.Timber

object CoreModule {

    fun build(loggerInteractor: LoggerInteractor) =
        module {
            single { loggerInteractor }
            single { ResourceProvider(get()) }
            single { PermissionHelper(get()) }
            single { ErrorInteractor(get()) }
            single { LocaleProvider(get()) }
            single { DispatcherProvider() }
            single { ObserverBus() }
            single { ClipboardHelper(get()) }
            single { DateFormatProvider(get()) }
            single { NoteDiffer() }
            single { SelectionHolder() }
            single<Settings> { SettingsImpl(get()) }
            single<DataCipherProvider> { DataCipherProviderImpl(get()) }
            single { FileHelper(get(), get()) }

            // Network
            single { provideOkHttp() }

            // Database
            single { AppDatabase.buildDatabase(get(), get()) }
            single { provideRemoteFileRepository(get()) }
            single { provideUsedFileRepository(get(), get()) }
            single { provideGitRootDao(get()) }

            // Files, Keepass
            single { FileSystemResolver(get(), get(), get(), get(), get(), get(), get()) }
            single<EncryptedDatabaseRepository> {
                KeepassDatabaseRepository(get(), get(), get(), get())
            }
        }

    private fun provideOkHttp(): OkHttpClient {
        val builder = OkHttpClient.Builder()

        val interceptor = HttpLoggingInterceptor {
            Timber.tag(OkHttp::class.java.simpleName).d(it)
        }.apply {
            setLevel(HttpLoggingInterceptor.Level.BASIC)
        }

        builder.addInterceptor(interceptor)

        return builder.build()
    }

    private fun provideRemoteFileRepository(database: AppDatabase) =
        RemoteFileRepository(database.remoteFileDao)

    private fun provideUsedFileRepository(database: AppDatabase, observerBus: ObserverBus) =
        UsedFileRepository(database.usedFileDao, observerBus)

    private fun provideGitRootDao(database: AppDatabase) =
        database.gitRootDao
}