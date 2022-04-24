package com.ivanovsky.passnotes.presentation.filepicker.factory

import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.core.factory.CellViewModelFactory
import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.core.model.FileCellModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.FileCellViewModel

class FilePickerCellViewModelFactory : CellViewModelFactory {

    override fun createCellViewModel(
        model: BaseCellModel,
        eventProvider: EventProvider
    ): BaseCellViewModel {
        return when (model) {
            is FileCellModel -> FileCellViewModel(
                model,
                eventProvider
            )
            else -> throwUnsupportedModelException(model)
        }
    }
}