package com.ivanovsky.passnotes.domain.test.usecases

import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.settings.Settings

class ResetTestDataUseCase(
    private val settings: Settings
) {

    fun resetTestData(): OperationResult<Unit> {
        settings.testAutofillData = null
        settings.testToggles = null

        return OperationResult.success(Unit)
    }
}