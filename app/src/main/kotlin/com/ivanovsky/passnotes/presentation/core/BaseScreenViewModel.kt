package com.ivanovsky.passnotes.presentation.core

import androidx.annotation.CallSuper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.core.event.EventProviderImpl
import kotlin.reflect.KClass

abstract class BaseScreenViewModel(
    initialState: ScreenState = ScreenState.notInitialized(),
    protected val eventProvider: EventProvider = EventProviderImpl()
) : ViewModel() {

    // TODO: refactor, create BaseCellScreenViewModel class and move cellViewModel into it
    private val _cellViewModels = MutableLiveData<List<BaseCellViewModel>>()
    val cellViewModels: LiveData<List<BaseCellViewModel>> = _cellViewModels

    val screenState = MutableLiveData(initialState)

    fun setCellViewModels(viewModels: List<BaseCellViewModel>) {
        _cellViewModels.postValue(viewModels)
    }

    @CallSuper
    open fun setScreenState(state: ScreenState) {
        screenState.value = state
    }

    fun setErrorState(error: OperationError) {
        setScreenState(ScreenState.error(error))
    }

    fun setErrorPanelState(error: OperationError) {
        setScreenState(ScreenState.dataWithError(error))
    }

    protected fun <T : BaseCellViewModel> findCellViewModel(
        cellId: String,
        type: KClass<T>
    ): T? {
        val viewModels = _cellViewModels.value ?: return null

        val viewModel = viewModels.firstOrNull { viewModel -> viewModel.model.id == cellId }
            ?: return null

        @Suppress("UNCHECKED_CAST")
        return viewModel as? T
    }

    @CallSuper
    override fun onCleared() {
        super.onCleared()
        eventProvider.clear()
    }
}