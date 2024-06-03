package com.ivanovsky.passnotes.injection.modules

import com.ivanovsky.passnotes.domain.test.TestDataInteractor
import com.ivanovsky.passnotes.domain.test.usecases.ResetAppDataUseCase
import com.ivanovsky.passnotes.domain.test.usecases.ResetTestDataUseCase
import com.ivanovsky.passnotes.domain.test.usecases.SetupFakeFileUseCase
import com.ivanovsky.passnotes.domain.test.usecases.SetupTestAutofillDataUseCase
import org.koin.dsl.module

object DebugModule {

    fun build() =
        module {
            single { ResetAppDataUseCase(get()) }
            single { ResetTestDataUseCase(get()) }
            single { SetupFakeFileUseCase(get(), get(), get(), get(), get()) }
            single { SetupTestAutofillDataUseCase(get(), get()) }

            single { TestDataInteractor(get(), get(), get(), get()) }
        }
}