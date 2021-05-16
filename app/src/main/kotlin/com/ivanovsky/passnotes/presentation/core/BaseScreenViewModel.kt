package com.ivanovsky.passnotes.presentation.core

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.core.event.EventProviderImpl

abstract class BaseScreenViewModel(
    protected val eventProvider: EventProvider = EventProviderImpl()
) : ViewModel() {

    private val _cellViewModels = MutableLiveData<List<BaseCellViewModel>>()
    val cellViewModels: LiveData<List<BaseCellViewModel>> = _cellViewModels

    fun setCellElements(viewModels: List<BaseCellViewModel>) {
        _cellViewModels.postValue(viewModels)
    }

    override fun onCleared() {
        super.onCleared()
        eventProvider.clear()
    }
}