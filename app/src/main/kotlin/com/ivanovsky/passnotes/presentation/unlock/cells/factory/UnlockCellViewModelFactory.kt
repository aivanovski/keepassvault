package com.ivanovsky.passnotes.presentation.unlock.cells.factory

import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.core.factory.CellViewModelFactory
import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.unlock.cells.model.DatabaseFileCellModel
import com.ivanovsky.passnotes.presentation.unlock.cells.viewmodel.DatabaseFileCellViewModel

class UnlockCellViewModelFactory : CellViewModelFactory {

    override fun createCellViewModel(
        model: BaseCellModel,
        eventProvider: EventProvider
    ): BaseCellViewModel {
        return when (model) {
            is DatabaseFileCellModel -> DatabaseFileCellViewModel(model, eventProvider)
            else -> throwUnsupportedModelException(model)
        }
    }
}