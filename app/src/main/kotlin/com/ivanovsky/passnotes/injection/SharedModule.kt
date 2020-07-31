package com.ivanovsky.passnotes.injection

import android.content.Context
import androidx.room.Room
import com.ivanovsky.passnotes.data.repository.DropboxFileRepository
import com.ivanovsky.passnotes.data.repository.SettingsRepository
import com.ivanovsky.passnotes.data.repository.db.AppDatabase
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.domain.*
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor

class SharedModule(private val context: Context) {

    val settings = SettingsRepository(context)
    val database = provideAppDatabase()
    val fileHelper = FileHelper(context, settings)
    val errorInteractor = ErrorInteractor(context)
    val permissionHelper = PermissionHelper(context)
    val resourceHelper = ResourceProvider(context)
    val localeProvider = LocaleProvider(context)
    val dispatcherProvider = DispatcherProvider()

    val dropboxFileRepository = DropboxFileRepository(database.dropboxFileDao)
    val fileSystemResolver = FileSystemResolver(settings, dropboxFileRepository, fileHelper)

    private fun provideAppDatabase(): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            AppDatabase.FILE_NAME
        ).build()
    }
}
