package com.ivanovsky.passnotes.presentation.history

import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.domain.ClipboardInteractor
import com.ivanovsky.passnotes.domain.usecases.history.GetHistoryUseCase
import com.ivanovsky.passnotes.domain.usecases.history.entity.HistoryDiffItem
import java.time.Duration
import java.util.UUID

class HistoryInteractor(
    private val settings: Settings,
    private val getHistoryUseCase: GetHistoryUseCase,
    private val clipboardInteractor: ClipboardInteractor
) {

    suspend fun getHistoryDiff(noteUid: UUID): OperationResult<List<HistoryDiffItem>> =
        getHistoryUseCase.getHistoryDiff(noteUid)

    fun copyToClipboardWithTimeout(text: String, isProtected: Boolean) {
        clipboardInteractor.copyWithTimeout(text, isProtected, getClipboardTimeout())
    }

    fun copyToClipboard(text: String, isProtected: Boolean) {
        clipboardInteractor.copy(text, isProtected)
    }

    fun getClipboardTimeout(): Duration {
        return Duration.ofMillis(settings.autoClearClipboardDelayInMs.toLong())
    }
}