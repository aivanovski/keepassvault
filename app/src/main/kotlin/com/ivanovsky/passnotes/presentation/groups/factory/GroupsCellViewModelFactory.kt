package com.ivanovsky.passnotes.presentation.groups.factory

import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.core.factory.CellViewModelFactory
import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.core.model.DatabaseStatusCellModel
import com.ivanovsky.passnotes.presentation.core.model.GroupCellModel
import com.ivanovsky.passnotes.presentation.core.model.NoteCellModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.DatabaseStatusCellViewModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.GroupGridCellViewModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.NoteGridCellViewModel

class GroupsCellViewModelFactory : CellViewModelFactory {

    override fun createCellViewModel(
        model: BaseCellModel,
        eventProvider: EventProvider
    ): BaseCellViewModel {
        return when (model) {
            is GroupCellModel -> GroupGridCellViewModel(model, eventProvider)
            is NoteCellModel -> NoteGridCellViewModel(model, eventProvider)
            is DatabaseStatusCellModel -> DatabaseStatusCellViewModel(model)
            else -> throwUnsupportedModelException(model)
        }
    }
}