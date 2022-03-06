package com.ivanovsky.passnotes.presentation.note.factory

import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.core.factory.CellViewModelFactory
import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.core.model.DatabaseStatusCellModel
import com.ivanovsky.passnotes.presentation.core.model.NotePropertyCellModel
import com.ivanovsky.passnotes.presentation.core.model.ProtectedNotePropertyCellModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.DatabaseStatusCellViewModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.NotePropertyCellViewModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.ProtectedNotePropertyCellViewModel

class NoteCellViewModelFactory : CellViewModelFactory {

    override fun createCellViewModel(
        model: BaseCellModel,
        eventProvider: EventProvider
    ): BaseCellViewModel {
        return when (model) {
            is NotePropertyCellModel -> NotePropertyCellViewModel(
                model,
                eventProvider
            )
            is ProtectedNotePropertyCellModel -> ProtectedNotePropertyCellViewModel(
                model,
                eventProvider
            )
            is DatabaseStatusCellModel -> DatabaseStatusCellViewModel(model)
            else -> throwUnsupportedModelException(model)
        }
    }
}