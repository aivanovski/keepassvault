package com.ivanovsky.passnotes.presentation.filepicker

import com.ivanovsky.passnotes.presentation.core_mvvm.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core_mvvm.event.EventProvider
import com.ivanovsky.passnotes.presentation.core_mvvm.factory.ItemViewModelFactory
import com.ivanovsky.passnotes.presentation.core_mvvm.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.core_mvvm.model.FileCellModel
import com.ivanovsky.passnotes.presentation.core_mvvm.viewmodels.FileCellViewModel

class FilePickerCellFactory : ItemViewModelFactory {

    override fun createCellViewModel(
        model: BaseCellModel,
        eventProvider: EventProvider
    ): BaseCellViewModel {
        return when (model) {
            is FileCellModel -> FileCellViewModel(
                model,
                eventProvider
            )
            else -> throwViewModelNotFoundException(model)
        }
    }
}