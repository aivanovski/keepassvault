package com.ivanovsky.passnotes.presentation.core_mvvm.factory

import com.ivanovsky.passnotes.presentation.core_mvvm.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core_mvvm.event.EventProvider
import com.ivanovsky.passnotes.presentation.core_mvvm.model.BaseCellModel
import kotlin.reflect.jvm.jvmName

interface ItemViewModelFactory {

    fun createCellViewModels(
        models: List<BaseCellModel>,
        eventProvider: EventProvider
    ): List<BaseCellViewModel> {
        return models.map { createCellViewModel(it, eventProvider) }
    }

    fun createCellViewModel(
        model: BaseCellModel,
        eventProvider: EventProvider
    ): BaseCellViewModel

    fun throwViewModelNotFoundException(model: BaseCellModel): Nothing {
        throw IllegalArgumentException("unable to find ViewModel for model: ${model::class.jvmName}")
    }
}