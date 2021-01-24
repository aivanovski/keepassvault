package com.ivanovsky.passnotes.injection

import com.ivanovsky.passnotes.App
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository
import com.ivanovsky.passnotes.domain.DateFormatProvider
import com.ivanovsky.passnotes.domain.interactor.filepicker.FilePickerInteractor
import com.ivanovsky.passnotes.domain.interactor.storagelist.StorageListInteractor
import com.ivanovsky.passnotes.domain.interactor.unlock.UnlockInteractor
import com.ivanovsky.passnotes.presentation.storagelist.StorageListViewModel
import com.ivanovsky.passnotes.presentation.unlock.UnlockViewModel
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
        single { DateFormatProvider(get()) }

        single { deps.fileSyncHelper }

        single { deps.dropboxFileRepository }
        single { deps.fileSystemResolver }
        single { deps.usedFileRepository }
        single { deps.encryptedDatabaseRepository as EncryptedDatabaseRepository }

        single { FilePickerInteractor(get()) }
        single { UnlockInteractor(get(), get(), get(), get()) }
        single { StorageListInteractor(get()) }
        
        viewModel { StorageListViewModel(get(), get(), get(), get(), get()) }
    }
}