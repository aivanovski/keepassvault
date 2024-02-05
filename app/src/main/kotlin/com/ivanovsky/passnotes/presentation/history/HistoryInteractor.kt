package com.ivanovsky.passnotes.presentation.history

import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.domain.usecases.history.GetHistoryUseCase
import com.ivanovsky.passnotes.domain.usecases.history.entity.HistoryDiffItem
import java.util.UUID

class HistoryInteractor(
    private val getHistoryUseCase: GetHistoryUseCase
) {

    suspend fun getHistoryDiff(noteUid: UUID): OperationResult<List<HistoryDiffItem>> =
        getHistoryUseCase.getHistoryDiff(noteUid)
}