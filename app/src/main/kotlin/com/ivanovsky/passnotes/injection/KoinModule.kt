package com.ivanovsky.passnotes.injection

import com.ivanovsky.passnotes.App
import com.ivanovsky.passnotes.domain.DateFormatProvider
import com.ivanovsky.passnotes.domain.interactor.filepicker.FilePickerInteractor
import org.koin.dsl.module
import kotlin.math.sin

object KoinModule {

    val appModule = module {
        val deps = App.sharedModule

        single { deps.settings }
        single { deps.database }
        single { deps.fileHelper }
        single { deps.permissionHelper }
        single { deps.resourceHelper }
        single { deps.errorInteractor }
        single { deps.localeProvider }
        single { deps.dispatcherProvider }
        single { DateFormatProvider(get()) }

        single { deps.dropboxFileRepository }
        single { deps.fileSystemResolver }

        single { FilePickerInteractor(get()) }
    }
}