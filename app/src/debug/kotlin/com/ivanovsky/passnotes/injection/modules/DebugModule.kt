package com.ivanovsky.passnotes.injection.modules

import com.ivanovsky.passnotes.domain.test.TestCommandInteractor
import com.ivanovsky.passnotes.domain.test.usecases.ResetTestDataUseCase
import com.ivanovsky.passnotes.domain.test.usecases.SetupFakeFileUseCase
import com.ivanovsky.passnotes.domain.test.usecases.SetupTestAutofillDataUseCase
import org.koin.dsl.module

object DebugModule {

    fun build() =
        module {
            single { ResetTestDataUseCase(get(), get()) }
            single { SetupFakeFileUseCase(get(), get(), get(), get(), get()) }
            single { SetupTestAutofillDataUseCase(get(), get()) }

            single { TestCommandInteractor(get(), get(), get()) }
        }
}