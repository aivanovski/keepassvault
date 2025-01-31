package com.ivanovsky.passnotes.presentation.core.dialog.optionDialog.factory

import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.core.factory.CellViewModelFactory
import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.core.model.DividerCellModel
import com.ivanovsky.passnotes.presentation.core.model.OneLineTextCellModel
import com.ivanovsky.passnotes.presentation.core.model.TwoLineTextCellModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.DividerCellViewModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.OneLineTextCellViewModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.TwoLineTextCellViewModel

class OptionDialogCellViewModelFactory(
    private val resourceProvider: ResourceProvider
) : CellViewModelFactory {

    override fun createCellViewModel(
        model: BaseCellModel,
        eventProvider: EventProvider
    ): BaseCellViewModel =
        when (model) {
            is OneLineTextCellModel -> OneLineTextCellViewModel(model, eventProvider)
            is TwoLineTextCellModel -> TwoLineTextCellViewModel(model, eventProvider)
            is DividerCellModel -> DividerCellViewModel(model, resourceProvider)
            else -> throwUnsupportedModelException(model)
        }
}