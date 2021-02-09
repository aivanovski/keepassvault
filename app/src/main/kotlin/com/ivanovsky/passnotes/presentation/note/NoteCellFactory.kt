package com.ivanovsky.passnotes.presentation.note

import com.ivanovsky.passnotes.presentation.core_mvvm.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core_mvvm.event.EventProvider
import com.ivanovsky.passnotes.presentation.core_mvvm.factory.CellViewModelFactory
import com.ivanovsky.passnotes.presentation.core_mvvm.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.core_mvvm.model.NotePropertyCellModel
import com.ivanovsky.passnotes.presentation.core_mvvm.viewmodels.NotePropertyCellViewModel

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
            else -> throwUnsupportedModelException(model)
        }
    }
}