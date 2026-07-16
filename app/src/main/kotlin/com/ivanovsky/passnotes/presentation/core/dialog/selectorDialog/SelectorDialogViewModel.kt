package com.ivanovsky.passnotes.presentation.core.dialog.selectorDialog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ivanovsky.passnotes.presentation.core.dialog.selectorDialog.model.SelectorDialogState
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import kotlinx.coroutines.flow.MutableStateFlow

class SelectorDialogViewModel(
    args: SelectorDialogArgs
) : ViewModel() {

    val state = MutableStateFlow(args.toInitialState())
    val selectItemEvent = SingleLiveEvent<Int>()
    val dismissEvent = SingleLiveEvent<Unit>()

    fun onItemClicked(index: Int) {
        state.value = state.value.copy(selectedItemIndex = index)
        selectItemEvent.call(index)
    }

    fun onCancelClicked() {
        dismissEvent.call(Unit)
    }

    private fun SelectorDialogArgs.toInitialState(): SelectorDialogState {
        return SelectorDialogState(
            title = title,
            description = description,
            items = items,
            selectedItemIndex = selectedItemIndex?.takeIf { index ->
                index in items.indices
            }
        )
    }

    class Factory(
        private val args: SelectorDialogArgs
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SelectorDialogViewModel(args) as T
        }
    }
}