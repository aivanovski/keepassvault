package com.ivanovsky.passnotes.injection

import android.content.Context
import androidx.room.Room
import com.github.terrakok.cicerone.Cicerone
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.crypto.DataCipherProvider
import com.ivanovsky.passnotes.data.repository.RemoteFileRepository
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl
import com.ivanovsky.passnotes.data.repository.UsedFileRepository
import com.ivanovsky.passnotes.data.repository.db.AppDatabase
import com.ivanovsky.passnotes.data.repository.db.converters.FSAuthorityTypeConverter
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.data.repository.keepass.KeepassDatabaseRepository
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.domain.*
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.SelectionHolder
import com.ivanovsky.passnotes.domain.interactor.autofill.AutofillInteractor
import com.ivanovsky.passnotes.domain.interactor.debugmenu.DebugMenuInteractor
import com.ivanovsky.passnotes.domain.interactor.filepicker.FilePickerInteractor
import com.ivanovsky.passnotes.domain.interactor.group_editor.GroupEditorInteractor
import com.ivanovsky.passnotes.domain.interactor.groups.GroupsInteractor
import com.ivanovsky.passnotes.domain.interactor.main.MainInteractor
import com.ivanovsky.passnotes.domain.interactor.newdb.NewDatabaseInteractor
import com.ivanovsky.passnotes.domain.interactor.note.NoteInteractor
import com.ivanovsky.passnotes.domain.interactor.note_editor.NoteEditorInteractor
import com.ivanovsky.passnotes.domain.interactor.search.SearchInteractor
import com.ivanovsky.passnotes.domain.interactor.selectdb.SelectDatabaseInteractor
import com.ivanovsky.passnotes.domain.interactor.server_login.GetDebugCredentialsUseCase
import com.ivanovsky.passnotes.domain.interactor.server_login.ServerLoginInteractor
import com.ivanovsky.passnotes.domain.interactor.service.LockServiceInteractor
import com.ivanovsky.passnotes.domain.interactor.settings.app.AppSettingsInteractor
import com.ivanovsky.passnotes.domain.interactor.settings.database.DatabaseSettingsInteractor
import com.ivanovsky.passnotes.domain.interactor.settings.main.MainSettingsInteractor
import com.ivanovsky.passnotes.domain.interactor.storagelist.StorageListInteractor
import com.ivanovsky.passnotes.domain.interactor.unlock.UnlockInteractor
import com.ivanovsky.passnotes.domain.usecases.AddTemplatesUseCase
import com.ivanovsky.passnotes.domain.usecases.UpdateNoteWithAutofillDataUseCase
import com.ivanovsky.passnotes.domain.usecases.LockDatabaseUseCase
import com.ivanovsky.passnotes.domain.usecases.FindNoteForAutofillUseCase
import com.ivanovsky.passnotes.domain.usecases.GetDatabaseStatusUseCase
import com.ivanovsky.passnotes.domain.usecases.GetDatabaseUseCase
import com.ivanovsky.passnotes.domain.usecases.GetGroupUseCase
import com.ivanovsky.passnotes.domain.usecases.GetNoteUseCase
import com.ivanovsky.passnotes.domain.usecases.GetRecentlyOpenedFilesUseCase
import com.ivanovsky.passnotes.domain.usecases.IsDatabaseOpenedUseCase
import com.ivanovsky.passnotes.domain.usecases.MoveGroupUseCase
import com.ivanovsky.passnotes.domain.usecases.MoveNoteUseCase
import com.ivanovsky.passnotes.domain.usecases.SyncUseCases
import com.ivanovsky.passnotes.domain.usecases.UpdateNoteUseCase
import com.ivanovsky.passnotes.presentation.about.AboutViewModel
import com.ivanovsky.passnotes.presentation.autofill.AutofillViewFactory
import com.ivanovsky.passnotes.presentation.core.factory.DatabaseStatusCellModelFactory
import com.ivanovsky.passnotes.presentation.debugmenu.DebugMenuViewModel
import com.ivanovsky.passnotes.presentation.filepicker.FilePickerViewModel
import com.ivanovsky.passnotes.presentation.group_editor.GroupEditorViewModel
import com.ivanovsky.passnotes.presentation.groups.GroupsScreenArgs
import com.ivanovsky.passnotes.presentation.groups.GroupsViewModel
import com.ivanovsky.passnotes.presentation.groups.dialog.SortAndViewDialogViewModel
import com.ivanovsky.passnotes.presentation.groups.factory.GroupsCellModelFactory
import com.ivanovsky.passnotes.presentation.groups.factory.GroupsCellViewModelFactory
import com.ivanovsky.passnotes.presentation.main.MainScreenArgs
import com.ivanovsky.passnotes.presentation.main.MainViewModel
import com.ivanovsky.passnotes.presentation.main.navigation.NavigationMenuViewModel
import com.ivanovsky.passnotes.presentation.newdb.NewDatabaseViewModel
import com.ivanovsky.passnotes.presentation.note.NoteScreenArgs
import com.ivanovsky.passnotes.presentation.note.factory.NoteCellViewModelFactory
import com.ivanovsky.passnotes.presentation.note.NoteViewModel
import com.ivanovsky.passnotes.presentation.note.factory.NoteCellModelFactory
import com.ivanovsky.passnotes.presentation.note_editor.NoteEditorViewModel
import com.ivanovsky.passnotes.presentation.note_editor.factory.NoteEditorCellModelFactory
import com.ivanovsky.passnotes.presentation.note_editor.factory.NoteEditorCellViewModelFactory
import com.ivanovsky.passnotes.presentation.search.SearchScreenArgs
import com.ivanovsky.passnotes.presentation.search.SearchViewModel
import com.ivanovsky.passnotes.presentation.search.factory.SearchCellModelFactory
import com.ivanovsky.passnotes.presentation.search.factory.SearchCellViewModelFactory
import com.ivanovsky.passnotes.presentation.selectdb.SelectDatabaseArgs
import com.ivanovsky.passnotes.presentation.selectdb.SelectDatabaseViewModel
import com.ivanovsky.passnotes.presentation.selectdb.cells.factory.SelectDatabaseCellModelFactory
import com.ivanovsky.passnotes.presentation.unlock.cells.factory.UnlockCellModelFactory
import com.ivanovsky.passnotes.presentation.selectdb.cells.factory.SelectDatabaseCellViewModelFactory
import com.ivanovsky.passnotes.presentation.server_login.ServerLoginArgs
import com.ivanovsky.passnotes.presentation.server_login.ServerLoginViewModel
import com.ivanovsky.passnotes.presentation.settings.SettingsRouter
import com.ivanovsky.passnotes.presentation.settings.app.AppSettingsViewModel
import com.ivanovsky.passnotes.presentation.settings.database.DatabaseSettingsViewModel
import com.ivanovsky.passnotes.presentation.settings.database.change_password.ChangePasswordDialogViewModel
import com.ivanovsky.passnotes.presentation.settings.main.MainSettingsViewModel
import com.ivanovsky.passnotes.presentation.storagelist.StorageListViewModel
import com.ivanovsky.passnotes.presentation.unlock.UnlockScreenArgs
import com.ivanovsky.passnotes.presentation.unlock.UnlockViewModel
import com.ivanovsky.passnotes.presentation.unlock.cells.factory.UnlockCellViewModelFactory
import okhttp3.OkHttp
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import timber.log.Timber

object KoinModule {

    fun buildModule(loggerInteractor: LoggerInteractor) =
        module {
            single { loggerInteractor }
            single { ResourceProvider(get()) }
            single { SettingsImpl(get(), get()) as Settings }
            single { FileHelper(get(), get()) }
            single { PermissionHelper(get()) }
            single { ErrorInteractor(get()) }
            single { LocaleProvider(get()) }
            single { DispatcherProvider() }
            single { ObserverBus() }
            single { ClipboardHelper(get()) }
            single { DateFormatProvider(get()) }
            single { NoteDiffer() }
            single { provideOkHttp() }
            single { DataCipherProvider(get()) }
            single { SelectionHolder() }

            // Database
            single { provideAppDatabase(get(), get()) }
            single { provideRemoteFileRepository(get()) }
            single { provideUsedFileRepository(get(), get()) }

            // Files, Keepass
            single { FileSystemResolver(get(), get(), get(), get(), get(), get()) }
            single { KeepassDatabaseRepository(get(), get(), get(), get()) as EncryptedDatabaseRepository }

            // Use Cases
            single { GetDebugCredentialsUseCase() }
            single { LockDatabaseUseCase() }
            single { GetRecentlyOpenedFilesUseCase(get(), get()) }
            single { SyncUseCases(get(), get()) }
            single { GetDatabaseStatusUseCase(get(), get()) }
            single { AddTemplatesUseCase(get(), get(), get()) }
            single { GetDatabaseUseCase(get(), get()) }
            single { MoveNoteUseCase(get(), get(), get()) }
            single { MoveGroupUseCase(get(), get(), get()) }
            single { GetGroupUseCase(get(), get()) }
            single { IsDatabaseOpenedUseCase(get()) }
            single { GetNoteUseCase(get(), get()) }
            single { FindNoteForAutofillUseCase(get(), get()) }
            single { UpdateNoteWithAutofillDataUseCase(get(), get(), get()) }
            single { UpdateNoteUseCase(get(), get(), get()) }

            // Interactors
            single { FilePickerInteractor(get()) }
            single { UnlockInteractor(get(), get(), get(), get(), get(), get(), get()) }
            single { StorageListInteractor(get(), get(), get()) }
            single { NewDatabaseInteractor(get(), get(), get(), get(), get()) }
            single { GroupEditorInteractor(get(), get(), get(), get(), get(), get()) }
            single { DebugMenuInteractor(get(), get(), get(), get(), get()) }
            single { NoteInteractor(get(), get(), get(), get(), get(), get()) }
            single { GroupsInteractor(get(), get(), get(), get(), get(), get(), get(), get()) }
            single { NoteEditorInteractor(get(), get(), get(), get()) }
            single { ServerLoginInteractor(get(), get(), get()) }
            single { DatabaseLockInteractor(get(), get(), get()) }
            single { SelectDatabaseInteractor(get(), get(), get(), get()) }
            single { SearchInteractor(get(), get(), get(), get(), get(), get()) }
            single { MainSettingsInteractor(get()) }
            single { DatabaseSettingsInteractor(get(), get()) }
            single { AppSettingsInteractor(get(), get(), get()) }
            single { AutofillInteractor(get(), get()) }
            single { MainInteractor(get()) }
            single { LockServiceInteractor(get(), get(), get(), get(), get()) }

            // Autofill
            single { AutofillViewFactory(get(), get()) }

            // Cell factories
            single { DatabaseStatusCellModelFactory(get()) }

            single { GroupsCellModelFactory(get()) }
            single { GroupsCellViewModelFactory(get(), get()) }

            single { NoteEditorCellModelFactory(get()) }
            single { NoteEditorCellViewModelFactory(get()) }

            single { SelectDatabaseCellModelFactory(get()) }
            single { SelectDatabaseCellViewModelFactory() }

            single { UnlockCellModelFactory(get()) }
            single { UnlockCellViewModelFactory() }

            single { SearchCellModelFactory() }
            single { SearchCellViewModelFactory(get(), get()) }

            single { NoteCellModelFactory() }
            single { NoteCellViewModelFactory() }

            // Cicerone
            single { Cicerone.create() }
            single { provideCiceroneRouter(get()) }
            single { provideCiceroneNavigatorHolder(get()) }
            single { SettingsRouter(get()) }

            // ViewModels
            viewModel { StorageListViewModel(get(), get(), get(), get(), get()) }
            viewModel { FilePickerViewModel(get(), get(), get(), get(), get(), get()) }
            viewModel { NewDatabaseViewModel(get(), get(), get(), get(), get()) }
            viewModel { GroupEditorViewModel(get(), get(), get(), get()) }
            viewModel { DebugMenuViewModel(get(), get(), get(), get(), get()) }
            factory { (args: NoteScreenArgs) -> NoteViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), args) }
            factory { (args: GroupsScreenArgs) -> GroupsViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), args) }
            viewModel { NoteEditorViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
            viewModel { (args: ServerLoginArgs) -> ServerLoginViewModel(get(), get(), get(), get(), args) }
            viewModel { (args: SelectDatabaseArgs) -> SelectDatabaseViewModel(get(), get(), get(), get(), get(), get(), get(), args) }
            factory { (args: SearchScreenArgs) -> SearchViewModel(get(), get(), get(), get(), get(), get(), args) }
            viewModel { AboutViewModel(get(), get()) }
            viewModel { MainSettingsViewModel(get(), get()) }
            viewModel { AppSettingsViewModel(get(), get(), get(), get(), get()) }
            viewModel { DatabaseSettingsViewModel(get(), get()) }
            viewModel { ChangePasswordDialogViewModel(get(), get(), get()) }
            viewModel { SortAndViewDialogViewModel(get()) }
            factory { (args: UnlockScreenArgs) -> UnlockViewModel(get(), get(), get(), get(), get(), get(), get(), get(), args) }
            factory { NavigationMenuViewModel(get()) }
            factory { (args: MainScreenArgs) -> MainViewModel(get(), get(), args) }
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

    private fun provideAppDatabase(
        context: Context,
        cipherProvider: DataCipherProvider
    ): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            AppDatabase.FILE_NAME
        )
            .addTypeConverter(FSAuthorityTypeConverter(cipherProvider))
            .build()
    }

    private fun provideCiceroneRouter(cicerone: Cicerone<Router>) =
        cicerone.router

    private fun provideCiceroneNavigatorHolder(cicerone: Cicerone<Router>) =
        cicerone.getNavigatorHolder()
}