package com.ivanovsky.passnotes.injection

import com.ivanovsky.passnotes.App
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository
import com.ivanovsky.passnotes.domain.DateFormatProvider
import com.ivanovsky.passnotes.domain.interactor.debugmenu.DebugMenuInteractor
import com.ivanovsky.passnotes.domain.interactor.filepicker.FilePickerInteractor
import com.ivanovsky.passnotes.domain.interactor.group.GroupInteractor
import com.ivanovsky.passnotes.domain.interactor.groups.GroupsInteractor
import com.ivanovsky.passnotes.domain.interactor.newdb.NewDatabaseInteractor
import com.ivanovsky.passnotes.domain.interactor.note.NoteInteractor
import com.ivanovsky.passnotes.domain.interactor.storagelist.StorageListInteractor
import com.ivanovsky.passnotes.domain.interactor.unlock.UnlockInteractor
import com.ivanovsky.passnotes.presentation.debugmenu.DebugMenuViewModel
import com.ivanovsky.passnotes.presentation.filepicker.FilePickerViewModel
import com.ivanovsky.passnotes.presentation.group.GroupViewModel
import com.ivanovsky.passnotes.presentation.groups.GroupsViewModel
import com.ivanovsky.passnotes.presentation.groups.factory.GroupsCellModelFactory
import com.ivanovsky.passnotes.presentation.groups.factory.GroupsCellViewModelFactory
import com.ivanovsky.passnotes.presentation.newdb.NewDatabaseViewModel
import com.ivanovsky.passnotes.presentation.note.NoteViewModel
import com.ivanovsky.passnotes.presentation.storagelist.StorageListViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

object KoinModule {

    val appModule = module {
        val deps = App.sharedModule

        single { deps.settings }
        single { deps.database }
        single { deps.fileHelper }
        single { deps.permissionHelper }
        single { deps.resourceProvider }
        single { deps.errorInteractor }
        single { deps.localeProvider }
        single { deps.dispatcherProvider }
        single { deps.observerBus }
        single { deps.clipboardHelper }
        single { DateFormatProvider(get()) }

        single { deps.fileSyncHelper }

        single { deps.dropboxFileRepository }
        single { deps.fileSystemResolver }
        single { deps.usedFileRepository }
        single { deps.encryptedDatabaseRepository as EncryptedDatabaseRepository }

        single { FilePickerInteractor(get()) }
        single { UnlockInteractor(get(), get(), get(), get()) }
        single { StorageListInteractor(get(), get()) }
        single { NewDatabaseInteractor(get(), get(), get(), get()) }
        single { GroupInteractor(get(), get(), get()) }
        single { DebugMenuInteractor(get(), get(), get()) }
        single { NoteInteractor(get(), get()) }
        single { GroupsInteractor(get(), get()) }

        single { GroupsCellModelFactory(get()) }
        single { GroupsCellViewModelFactory() }

        viewModel { StorageListViewModel(get(), get(), get(), get(), get()) }
        viewModel { FilePickerViewModel(get(), get(), get(), get(), get()) }
        viewModel { NewDatabaseViewModel(get(), get(), get(), get()) }
        viewModel { GroupViewModel(get(), get(), get()) }
        viewModel { DebugMenuViewModel(get(), get(), get(), get()) }
        viewModel { NoteViewModel(get(), get(), get(), get(), get()) }
        viewModel { GroupsViewModel(get(), get(), get(), get(), get(), get()) }
    }
}