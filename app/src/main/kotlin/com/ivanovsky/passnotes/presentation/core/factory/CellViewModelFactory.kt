package com.ivanovsky.passnotes.presentation.core.factory

import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel

interface CellViewModelFactory {

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

    fun throwUnsupportedModelException(model: BaseCellModel): Nothing {
        throw IllegalArgumentException(
            "Unable to find ViewModel for model: ${model::class.qualifiedName}"
        )
    }
}