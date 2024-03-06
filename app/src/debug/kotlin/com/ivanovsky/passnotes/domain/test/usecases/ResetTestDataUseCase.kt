package com.ivanovsky.passnotes.domain.test.usecases

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.domain.ResourceProvider

class ResetTestDataUseCase(
    private val settings: Settings,
    private val resourceProvider: ResourceProvider
) {

    fun resetTestData(): OperationResult<String> {
        settings.testAutofillData = null
        settings.testToggles = null

        return OperationResult.success(resourceProvider.getString(R.string.test_data_removed))
    }
}