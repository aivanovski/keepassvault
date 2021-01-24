package com.ivanovsky.passnotes.presentation.storagelist

import com.ivanovsky.passnotes.presentation.core_mvvm.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core_mvvm.event.EventProvider
import com.ivanovsky.passnotes.presentation.core_mvvm.factory.ItemViewModelFactory
import com.ivanovsky.passnotes.presentation.core_mvvm.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.core_mvvm.model.SingleTextCellModel
import com.ivanovsky.passnotes.presentation.core_mvvm.viewmodels.SingleTextCellViewModel

class StorageListCellFactory : ItemViewModelFactory {

    override fun createCellViewModel(
        model: BaseCellModel,
        eventProvider: EventProvider
    ): BaseCellViewModel {
        return when (model) {
            is SingleTextCellModel -> SingleTextCellViewModel(model, eventProvider)
            else -> throwViewModelNotFoundException(model)
        }
    }
}