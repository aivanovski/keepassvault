package com.ivanovsky.passnotes.domain.test

import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.domain.test.entity.TestCommand
import com.ivanovsky.passnotes.domain.test.usecases.ResetTestDataUseCase
import com.ivanovsky.passnotes.domain.test.usecases.SetupFakeFileUseCase
import com.ivanovsky.passnotes.domain.test.usecases.SetupTestAutofillDataUseCase

class TestCommandInteractor(
    private val resetTestDataUseCase: ResetTestDataUseCase,
    private val setupFakeFileUseCase: SetupFakeFileUseCase,
    private val setupAutofillDataUseCase: SetupTestAutofillDataUseCase
) {

    fun process(command: TestCommand): OperationResult<String> {
        return when (command) {
            is TestCommand.ResetTestData -> {
                resetTestDataUseCase.resetTestData()
            }

            is TestCommand.SetupAutofillData -> {
                setupAutofillDataUseCase.setupTestAutofillData(command.autofillData)
            }

            is TestCommand.SetupFakeFile -> {
                setupFakeFileUseCase.setupFakeFile(command.fileName)
            }
        }
    }
}