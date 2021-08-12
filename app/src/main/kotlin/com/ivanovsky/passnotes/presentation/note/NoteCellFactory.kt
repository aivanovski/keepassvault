package com.ivanovsky.passnotes.presentation.note

import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.core.factory.CellViewModelFactory
import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.core.model.DatabaseStatusCellModel
import com.ivanovsky.passnotes.presentation.core.model.NotePropertyCellModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.DatabaseStatusCellViewModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.NotePropertyCellViewModel

// TODO: refactor:
// - move to package "factory"
// - should be instantiated in KoinModule
// - NoteViewModel should receive it as dependency in constructor
class NoteCellFactory : CellViewModelFactory {

    override fun createCellViewModel(
        model: BaseCellModel,
        eventProvider: EventProvider
    ): BaseCellViewModel {
        return when (model) {
            is NotePropertyCellModel -> NotePropertyCellViewModel(
                model,
                eventProvider
            )
            is DatabaseStatusCellModel -> DatabaseStatusCellViewModel(model)
            else -> throwUnsupportedModelException(model)
        }
    }
}