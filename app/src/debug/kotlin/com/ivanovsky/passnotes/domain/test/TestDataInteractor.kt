package com.ivanovsky.passnotes.domain.test

import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.domain.test.entity.TestArguments
import com.ivanovsky.passnotes.domain.test.usecases.ResetAppDataUseCase
import com.ivanovsky.passnotes.domain.test.usecases.ResetTestDataUseCase
import com.ivanovsky.passnotes.domain.test.usecases.SetupFakeFileUseCase
import com.ivanovsky.passnotes.domain.test.usecases.SetupTestAutofillDataUseCase
import com.ivanovsky.passnotes.extensions.mapError

class TestDataInteractor(
    private val resetAppDataUseCase: ResetAppDataUseCase,
    private val resetTestDataUseCase: ResetTestDataUseCase,
    private val setupFakeFileUseCase: SetupFakeFileUseCase,
    private val setupAutofillDataUseCase: SetupTestAutofillDataUseCase
) {

    fun process(arguments: TestArguments): OperationResult<Unit> {
        if (arguments.isResetAppData) {
            // Application will be stopped after data reset
            return resetAppDataUseCase.resetApplicationData()
        }

        if (arguments.isResetTestData) {
            val resetTestDatResult = resetTestDataUseCase.resetTestData()
            if (resetTestDatResult.isFailed) {
                return resetTestDatResult.mapError()
            }
        }

        if (arguments.testFakeFileName != null) {
            val setupFileResult = setupFakeFileUseCase.setupFakeFile(arguments.testFakeFileName)
            if (setupFileResult.isFailed) {
                return setupFileResult.mapError()
            }
        }

        if (arguments.testAutofillData != null) {
            val setupAutofillDataResult = setupAutofillDataUseCase.setupTestAutofillData(
                arguments.testAutofillData
            )
            if (setupAutofillDataResult.isFailed) {
                return setupAutofillDataResult.mapError()
            }
        }

        return OperationResult.success(Unit)
    }
}