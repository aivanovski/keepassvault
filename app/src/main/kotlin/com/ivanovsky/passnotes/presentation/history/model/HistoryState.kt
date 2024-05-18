package com.ivanovsky.passnotes.presentation.history.model

import androidx.compose.runtime.Immutable
import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel

@Immutable
sealed class HistoryState {

    @Immutable
    data object Loading : HistoryState()

    @Immutable
    data object Empty : HistoryState()

    @Immutable
    data class Error(
        val message: String
    ) : HistoryState()

    @Immutable
    data class Data(
        val viewModels: List<BaseCellViewModel>
    ) : HistoryState()
}