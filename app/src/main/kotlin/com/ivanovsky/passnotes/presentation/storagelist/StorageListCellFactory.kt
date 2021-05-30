package com.ivanovsky.passnotes.presentation.storagelist

import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.core.factory.CellViewModelFactory
import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.core.model.SingleTextCellModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.SingleTextCellViewModel

class StorageListCellFactory : CellViewModelFactory {

    override fun createCellViewModel(
        model: BaseCellModel,
        eventProvider: EventProvider
    ): BaseCellViewModel {
        return when (model) {
            is SingleTextCellModel -> SingleTextCellViewModel(model, eventProvider)
            else -> throwUnsupportedModelException(model)
        }
    }
}