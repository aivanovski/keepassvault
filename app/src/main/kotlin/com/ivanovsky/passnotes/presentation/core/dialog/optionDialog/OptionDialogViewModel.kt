package com.ivanovsky.passnotes.presentation.core.dialog.optionDialog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ivanovsky.passnotes.injection.GlobalInjector
import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.BaseScreenViewModel
import com.ivanovsky.passnotes.presentation.core.dialog.optionDialog.factory.OptionDialogCellModelFactory
import com.ivanovsky.passnotes.presentation.core.dialog.optionDialog.factory.OptionDialogCellViewModelFactory
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import com.ivanovsky.passnotes.presentation.core.viewmodel.OneLineTextCellViewModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.TwoLineTextCellViewModel
import com.ivanovsky.passnotes.util.toIntSafely
import org.koin.core.parameter.parametersOf

class OptionDialogViewModel(
    private val modelFactory: OptionDialogCellModelFactory,
    private val viewModelFactory: OptionDialogCellViewModelFactory,
    private val args: OptionDialogArgs
) : BaseScreenViewModel() {

    val selectItemEvent = SingleLiveEvent<Int>()

    init {
        setCellElements(createCellViewModels())
        subscribeToEvents()
    }

    override fun onCleared() {
        super.onCleared()
        unsubscribeFromEvents()
    }

    private fun subscribeToEvents() {
        eventProvider.subscribe(this) { event ->
            event.getString(OneLineTextCellViewModel.CLICK_EVENT)
                ?.let { cellId ->
                    onCellClicked(cellId)
                }

            event.getString(TwoLineTextCellViewModel.CLICK_EVENT)
                ?.let { cellId ->
                    onCellClicked(cellId)
                }
        }
    }

    private fun onCellClicked(cellId: String) {
        val index = cellId.toIntSafely() ?: return

        selectItemEvent.call(index)
    }

    private fun unsubscribeFromEvents() {
        eventProvider.unSubscribe(this)
    }

    private fun createCellViewModels(): List<BaseCellViewModel> {
        val models = modelFactory.createCellModels(args.options)
        return viewModelFactory.createCellViewModels(models, eventProvider)
    }

    class Factory(
        private val args: OptionDialogArgs
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GlobalInjector.get<OptionDialogViewModel>(
                parametersOf(args)
            ) as T
        }
    }
}