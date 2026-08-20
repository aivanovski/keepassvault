package com.ivanovsky.passnotes.injection.modules.debug

import com.ivanovsky.passnotes.domain.test.TestDataInteractor
import com.ivanovsky.passnotes.domain.test.usecases.ResetAppDataUseCase
import com.ivanovsky.passnotes.domain.test.usecases.ResetTestDataUseCase
import com.ivanovsky.passnotes.domain.test.usecases.SetupFakeFileUseCase
import com.ivanovsky.passnotes.domain.test.usecases.SetupTestAutofillDataUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

object DebugModule {

    fun build() =
        module {
            singleOf(::ResetAppDataUseCase)
            singleOf(::ResetTestDataUseCase)
            singleOf(::SetupFakeFileUseCase)
            singleOf(::SetupTestAutofillDataUseCase)

            singleOf(::TestDataInteractor)
        }
}