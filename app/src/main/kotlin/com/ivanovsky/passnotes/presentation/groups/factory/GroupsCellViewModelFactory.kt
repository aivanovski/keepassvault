package com.ivanovsky.passnotes.presentation.groups.factory

import com.ivanovsky.passnotes.presentation.core_mvvm.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core_mvvm.event.EventProvider
import com.ivanovsky.passnotes.presentation.core_mvvm.factory.CellViewModelFactory
import com.ivanovsky.passnotes.presentation.core_mvvm.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.core_mvvm.model.GroupCellModel
import com.ivanovsky.passnotes.presentation.core_mvvm.model.NoteCellModel
import com.ivanovsky.passnotes.presentation.core_mvvm.viewmodels.GroupGridCellViewModel
import com.ivanovsky.passnotes.presentation.core_mvvm.viewmodels.NoteGridCellViewModel

class GroupsCellViewModelFactory : CellViewModelFactory {

    override fun createCellViewModel(
        model: BaseCellModel,
        eventProvider: EventProvider
    ): BaseCellViewModel {
        return when (model) {
            is GroupCellModel -> GroupGridCellViewModel(model, eventProvider)
            is NoteCellModel -> NoteGridCellViewModel(model, eventProvider)
            else -> throwUnsupportedModelException(model)
        }
    }
}