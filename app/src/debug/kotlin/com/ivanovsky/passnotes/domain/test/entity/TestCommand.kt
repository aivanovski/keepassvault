package com.ivanovsky.passnotes.domain.test.entity

import com.ivanovsky.passnotes.data.entity.TestAutofillData

sealed class TestCommand {

    data object ResetTestData : TestCommand()

    data class SetupFakeFile(
        val fileName: String
    ) : TestCommand()

    data class SetupAutofillData(
        val autofillData: TestAutofillData
    ) : TestCommand()
}