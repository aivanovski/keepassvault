package com.ivanovsky.passnotes.injection

import android.content.Context
import androidx.room.Room
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.repository.DropboxFileRepository
import com.ivanovsky.passnotes.data.repository.SettingsRepository
import com.ivanovsky.passnotes.data.repository.UsedFileRepository
import com.ivanovsky.passnotes.data.repository.db.AppDatabase
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.data.repository.keepass.KeepassDatabaseRepository
import com.ivanovsky.passnotes.domain.*
import com.ivanovsky.passnotes.domain.globalsnackbar.GlobalSnackbarBus
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor

class SharedModule(private val context: Context) {

    val settings = SettingsRepository(context)
    val database = provideAppDatabase()
    val fileHelper = FileHelper(context, settings)
    val errorInteractor = ErrorInteractor(context)
    val permissionHelper = PermissionHelper(context)
    val resourceProvider = ResourceProvider(context)
    val localeProvider = LocaleProvider(context)
    val dispatcherProvider = DispatcherProvider()
    val observerBus = ObserverBus()

    val dropboxFileRepository = DropboxFileRepository(database.dropboxFileDao)
    val fileSystemResolver = FileSystemResolver(
        settings,
        dropboxFileRepository,
        fileHelper,
        permissionHelper
    )
    val fileSyncHelper = FileSyncHelper(fileSystemResolver)
    val usedFileRepository = UsedFileRepository(
        database,
        observerBus
    )
    val encryptedDatabaseRepository = KeepassDatabaseRepository(
        context,
        fileSystemResolver
    )

    private fun provideAppDatabase(): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            AppDatabase.FILE_NAME
        ).build()
    }
}
