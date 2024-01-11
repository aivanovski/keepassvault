package com.ivanovsky.passnotes.injection.modules

import com.github.terrakok.cicerone.Cicerone
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.domain.ClipboardInteractor
import com.ivanovsky.passnotes.domain.DatabaseLockInteractor
import com.ivanovsky.passnotes.domain.interactor.autofill.AutofillInteractor
import com.ivanovsky.passnotes.domain.interactor.debugmenu.DebugMenuInteractor
import com.ivanovsky.passnotes.domain.interactor.filepicker.FilePickerInteractor
import com.ivanovsky.passnotes.domain.interactor.groupEditor.GroupEditorInteractor
import com.ivanovsky.passnotes.domain.interactor.groups.GroupsInteractor
import com.ivanovsky.passnotes.domain.interactor.main.MainInteractor
import com.ivanovsky.passnotes.domain.interactor.newdb.NewDatabaseInteractor
import com.ivanovsky.passnotes.domain.interactor.note.NoteInteractor
import com.ivanovsky.passnotes.domain.interactor.noteEditor.NoteEditorInteractor
import com.ivanovsky.passnotes.domain.interactor.passwordGenerator.PasswordGeneratorInteractor
import com.ivanovsky.passnotes.domain.interactor.serverLogin.ServerLoginInteractor
import com.ivanovsky.passnotes.domain.interactor.service.LockServiceInteractor
import com.ivanovsky.passnotes.domain.interactor.settings.app.AppSettingsInteractor
import com.ivanovsky.passnotes.domain.interactor.settings.database.DatabaseSettingsInteractor
import com.ivanovsky.passnotes.domain.interactor.settings.main.MainSettingsInteractor
import com.ivanovsky.passnotes.domain.interactor.storagelist.StorageListInteractor
import com.ivanovsky.passnotes.domain.interactor.syncState.SyncStateCache
import com.ivanovsky.passnotes.domain.interactor.syncState.SyncStateInteractor
import com.ivanovsky.passnotes.domain.interactor.unlock.UnlockInteractor
import com.ivanovsky.passnotes.presentation.about.AboutViewModel
import com.ivanovsky.passnotes.presentation.autofill.AutofillViewFactory
import com.ivanovsky.passnotes.presentation.core.dialog.resolveConflict.ResolveConflictDialogArgs
import com.ivanovsky.passnotes.presentation.core.dialog.resolveConflict.ResolveConflictDialogInteractor
import com.ivanovsky.passnotes.presentation.core.dialog.resolveConflict.ResolveConflictDialogViewModel
import com.ivanovsky.passnotes.presentation.core.dialog.sortAndView.SortAndViewDialogArgs
import com.ivanovsky.passnotes.presentation.core.dialog.sortAndView.SortAndViewDialogViewModel
import com.ivanovsky.passnotes.presentation.debugmenu.DebugMenuViewModel
import com.ivanovsky.passnotes.presentation.enterDbCredentials.EnterDbCredentialsInteractor
import com.ivanovsky.passnotes.presentation.enterDbCredentials.EnterDbCredentialsScreenArgs
import com.ivanovsky.passnotes.presentation.enterDbCredentials.EnterDbCredentialsViewModel
import com.ivanovsky.passnotes.presentation.filepicker.FilePickerArgs
import com.ivanovsky.passnotes.presentation.filepicker.FilePickerViewModel
import com.ivanovsky.passnotes.presentation.filepicker.factory.FilePickerCellModelFactory
import com.ivanovsky.passnotes.presentation.filepicker.factory.FilePickerCellViewModelFactory
import com.ivanovsky.passnotes.presentation.groupEditor.GroupEditorArgs
import com.ivanovsky.passnotes.presentation.groupEditor.GroupEditorViewModel
import com.ivanovsky.passnotes.presentation.groups.GroupsScreenArgs
import com.ivanovsky.passnotes.presentation.groups.GroupsViewModel
import com.ivanovsky.passnotes.presentation.groups.factory.GroupsCellModelFactory
import com.ivanovsky.passnotes.presentation.groups.factory.GroupsCellViewModelFactory
import com.ivanovsky.passnotes.presentation.main.MainScreenArgs
import com.ivanovsky.passnotes.presentation.main.MainViewModel
import com.ivanovsky.passnotes.presentation.main.navigation.NavigationMenuViewModel
import com.ivanovsky.passnotes.presentation.main.navigation.cells.factory.NavigationMenuCellModelFactory
import com.ivanovsky.passnotes.presentation.main.navigation.cells.factory.NavigationMenuCellViewModelFactory
import com.ivanovsky.passnotes.presentation.newdb.NewDatabaseViewModel
import com.ivanovsky.passnotes.presentation.note.NoteScreenArgs
import com.ivanovsky.passnotes.presentation.note.NoteViewModel
import com.ivanovsky.passnotes.presentation.note.factory.NoteCellModelFactory
import com.ivanovsky.passnotes.presentation.note.factory.NoteCellViewModelFactory
import com.ivanovsky.passnotes.presentation.noteEditor.NoteEditorArgs
import com.ivanovsky.passnotes.presentation.noteEditor.NoteEditorViewModel
import com.ivanovsky.passnotes.presentation.noteEditor.factory.NoteEditorCellModelFactory
import com.ivanovsky.passnotes.presentation.noteEditor.factory.NoteEditorCellViewModelFactory
import com.ivanovsky.passnotes.presentation.passwordGenerator.PasswordGeneratorViewModel
import com.ivanovsky.passnotes.presentation.serverLogin.ServerLoginArgs
import com.ivanovsky.passnotes.presentation.serverLogin.ServerLoginViewModel
import com.ivanovsky.passnotes.presentation.settings.SettingsRouter
import com.ivanovsky.passnotes.presentation.settings.app.AppSettingsViewModel
import com.ivanovsky.passnotes.presentation.settings.database.DatabaseSettingsViewModel
import com.ivanovsky.passnotes.presentation.settings.database.changePassword.ChangePasswordDialogViewModel
import com.ivanovsky.passnotes.presentation.settings.main.MainSettingsViewModel
import com.ivanovsky.passnotes.presentation.storagelist.StorageListArgs
import com.ivanovsky.passnotes.presentation.storagelist.StorageListViewModel
import com.ivanovsky.passnotes.presentation.storagelist.factory.StorageListCellModelFactory
import com.ivanovsky.passnotes.presentation.storagelist.factory.StorageListCellViewModelFactory
import com.ivanovsky.passnotes.presentation.syncState.factory.SyncStateCellModelFactory
import com.ivanovsky.passnotes.presentation.unlock.UnlockScreenArgs
import com.ivanovsky.passnotes.presentation.unlock.UnlockViewModel
import com.ivanovsky.passnotes.presentation.unlock.cells.factory.UnlockCellModelFactory
import com.ivanovsky.passnotes.presentation.unlock.cells.factory.UnlockCellViewModelFactory
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

object UiModule {

    fun build() =
        module {
            // Interactors
            single { ClipboardInteractor(get()) }
            single { DatabaseLockInteractor(get(), get(), get(), get()) }
            single { FilePickerInteractor(get(), get()) }
            single {
                UnlockInteractor(
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get()
                )
            }
            single { StorageListInteractor(get(), get(), get()) }
            single { NewDatabaseInteractor(get(), get(), get(), get()) }
            single { GroupEditorInteractor(get(), get(), get(), get(), get()) }
            single { DebugMenuInteractor(get(), get(), get(), get(), get()) }
            single {
                NoteInteractor(
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get()
                )
            }
            single {
                GroupsInteractor(
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get()
                )
            }
            single { NoteEditorInteractor(get(), get(), get(), get(), get(), get()) }
            single { ServerLoginInteractor(get(), get(), get()) }
            single { MainSettingsInteractor(get()) }
            single { DatabaseSettingsInteractor(get(), get()) }
            single { AppSettingsInteractor(get(), get(), get(), get()) }
            single { AutofillInteractor(get(), get()) }
            single { MainInteractor(get()) }
            single { LockServiceInteractor(get(), get(), get(), get(), get()) }
            single { PasswordGeneratorInteractor(get()) }
            single { ResolveConflictDialogInteractor(get()) }
            single { SyncStateCache(get()) }
            single { SyncStateInteractor(get(), get(), get()) }
            single { EnterDbCredentialsInteractor(get(), get(), get()) }

            // Autofill
            single { AutofillViewFactory(get(), get()) }

            // Cell factories
            single { GroupsCellModelFactory(get()) }
            single { GroupsCellViewModelFactory(get(), get()) }

            single { NoteEditorCellModelFactory(get()) }
            single { NoteEditorCellViewModelFactory(get()) }

            single { UnlockCellModelFactory(get()) }
            single { UnlockCellViewModelFactory() }

            single { NoteCellModelFactory(get()) }
            single { NoteCellViewModelFactory(get()) }

            single { FilePickerCellModelFactory() }
            single { FilePickerCellViewModelFactory() }

            single { StorageListCellModelFactory(get()) }
            single { StorageListCellViewModelFactory() }

            single { NavigationMenuCellModelFactory(get()) }
            single { NavigationMenuCellViewModelFactory() }

            single { SyncStateCellModelFactory(get()) }

            // Cicerone
            single { Cicerone.create() }
            single { provideCiceroneRouter(get()) }
            single { provideCiceroneNavigatorHolder(get()) }
            single { SettingsRouter(get()) }

            // ViewModels
            factory { (args: StorageListArgs) ->
                StorageListViewModel(
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    args
                )
            }
            factory { (args: FilePickerArgs) ->
                FilePickerViewModel(
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    args
                )
            }
            factory { (args: GroupEditorArgs) ->
                GroupEditorViewModel(
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    args
                )
            }
            factory { (args: NoteScreenArgs) ->
                NoteViewModel(
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    args
                )
            }
            factory { (args: GroupsScreenArgs) ->
                GroupsViewModel(
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    args
                )
            }
            viewModel { (args: NoteEditorArgs) ->
                NoteEditorViewModel(
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    args
                )
            }
            viewModel { (args: ServerLoginArgs) ->
                ServerLoginViewModel(
                    get(),
                    get(),
                    get(),
                    get(),
                    args
                )
            }
            factory { (args: UnlockScreenArgs) ->
                UnlockViewModel(
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    args
                )
            }
            viewModel { AboutViewModel(get(), get()) }
            viewModel { MainSettingsViewModel(get(), get()) }
            viewModel { AppSettingsViewModel(get(), get(), get(), get(), get(), get()) }
            viewModel { DatabaseSettingsViewModel(get(), get(), get(), get()) }
            viewModel { ChangePasswordDialogViewModel(get(), get(), get()) }
            viewModel {
                PasswordGeneratorViewModel(
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get(),
                    get()
                )
            }
            viewModel { DebugMenuViewModel(get(), get(), get(), get(), get()) }
            viewModel { NewDatabaseViewModel(get(), get(), get(), get(), get()) }
            factory { (args: SortAndViewDialogArgs) -> SortAndViewDialogViewModel(get(), args) }
            factory { (args: ResolveConflictDialogArgs) ->
                ResolveConflictDialogViewModel(
                    get(),
                    get(),
                    get(),
                    get(),
                    args
                )
            }
            factory { NavigationMenuViewModel(get(), get(), get(), get()) }
            factory { (args: MainScreenArgs) -> MainViewModel(get(), get(), args) }
            factory { (args: EnterDbCredentialsScreenArgs) ->
                EnterDbCredentialsViewModel(
                    get(),
                    get(),
                    get(),
                    args
                )
            }
        }

    private fun provideCiceroneRouter(cicerone: Cicerone<Router>) =
        cicerone.router

    private fun provideCiceroneNavigatorHolder(cicerone: Cicerone<Router>) =
        cicerone.getNavigatorHolder()
}