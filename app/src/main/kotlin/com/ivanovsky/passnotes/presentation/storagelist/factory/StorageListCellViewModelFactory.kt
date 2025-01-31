package com.ivanovsky.passnotes.presentation.storagelist.factory

import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.core.factory.CellViewModelFactory
import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.core.model.OneLineTextCellModel
import com.ivanovsky.passnotes.presentation.core.model.TwoTextWithIconCellModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.OneLineTextCellViewModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.TwoTextWithIconCellViewModel

class StorageListCellViewModelFactory : CellViewModelFactory {

    override fun createCellViewModel(
        model: BaseCellModel,
        eventProvider: EventProvider
    ): BaseCellViewModel {
        return when (model) {
            is OneLineTextCellModel -> OneLineTextCellViewModel(model, eventProvider)
            is TwoTextWithIconCellModel -> TwoTextWithIconCellViewModel(model, eventProvider)
            else -> throwUnsupportedModelException(model)
        }
    }
}