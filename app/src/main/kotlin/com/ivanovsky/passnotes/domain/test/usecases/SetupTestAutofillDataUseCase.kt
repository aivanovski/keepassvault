package com.ivanovsky.passnotes.domain.test.usecases

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.TestAutofillData
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.domain.ResourceProvider

class SetupTestAutofillDataUseCase(
    private val settings: Settings,
    private val resourceProvider: ResourceProvider
) {

    fun setupTestAutofillData(data: TestAutofillData): OperationResult<String> {
        settings.testAutofillData = data

        return OperationResult.success(
            resourceProvider.getString(R.string.test_data_successfully_imported)
        )
    }
}