package com.ivanovsky.passnotes.presentation.debugmenu.dialog

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ivanovsky.passnotes.injection.GlobalInjector
import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.event.EventProviderImpl
import com.ivanovsky.passnotes.presentation.debugmenu.dialog.cells.SelectorDialogCellFactory
import com.ivanovsky.passnotes.presentation.debugmenu.dialog.cells.viewModel.CheckboxCellViewModel
import com.ivanovsky.passnotes.presentation.debugmenu.dialog.cells.viewModel.EditableTextCellViewModel
import com.ivanovsky.passnotes.util.StringUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.parameter.parametersOf

class SelectorDialogViewModel(
    private val cellFactory: SelectorDialogCellFactory,
    private val args: SelectorDialogArgs
) : ViewModel() {

    private val eventProvider = EventProviderImpl()

    val cellViewModels = MutableLiveData<List<BaseCellViewModel>>()
    val searchFieldViewModel = cellFactory.createSearchCell(eventProvider)
    val isLoading = MutableLiveData(false)
    var currentQuery = StringUtils.EMPTY

    private val allViewModels = cellFactory.createOptionCells(
        options = args.options,
        selectedIndices = args.selectedIndices,
        eventProvider = eventProvider
    )

    init {
        subscribeToCellEvents()

        cellViewModels.value = allViewModels
    }

    override fun onCleared() {
        unsubscribeFromCellEvents()
    }

    private fun subscribeToCellEvents() {
        eventProvider.subscribe(this) { event ->
            event.getString(EditableTextCellViewModel.TEXT_CHANGED_EVENT)
                ?.let { query ->
                    search(query)
                }

        }
    }

    private fun unsubscribeFromCellEvents() {
        eventProvider.clear()
    }

    private fun search(query: String) {
        if (query == this.currentQuery) return

        isLoading.value = true

        viewModelScope.launch {
            val filteredCells = withContext(Dispatchers.IO) { filterViewModelsByQuery(query) }

            currentQuery = query
            cellViewModels.value = filteredCells
            isLoading.value = false
        }
    }

    private fun filterViewModelsByQuery(query: String): List<CheckboxCellViewModel> {
        if (query.isEmpty()) {
            return allViewModels
        }

        val cells = mutableListOf<CheckboxCellViewModel>()

        for ((index, option) in args.options.withIndex()) {
            if (option.title.contains(query, ignoreCase = true)
                || option.description.contains(query, ignoreCase = true)
            ) {
                cells.add(allViewModels[index])
            }
        }

        return cells
    }

    class Factory(
        private val args: SelectorDialogArgs
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GlobalInjector.get<SelectorDialogViewModel>(
                parametersOf(args)
            ) as T
        }
    }
}