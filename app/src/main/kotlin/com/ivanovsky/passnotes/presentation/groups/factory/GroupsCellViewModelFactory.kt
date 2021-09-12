package com.ivanovsky.passnotes.presentation.groups.factory

import com.ivanovsky.passnotes.domain.LocaleProvider
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.core.factory.CellViewModelFactory
import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.core.model.DatabaseStatusCellModel
import com.ivanovsky.passnotes.presentation.core.model.GroupCellModel
import com.ivanovsky.passnotes.presentation.core.model.NoteCellModel
import com.ivanovsky.passnotes.presentation.core.model.OptionPanelCellModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.DatabaseStatusCellViewModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.GroupCellViewModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.NoteCellViewModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.OptionPanelCellViewModel

class GroupsCellViewModelFactory(
    private val resourceProvider: ResourceProvider,
    private val localeProvider: LocaleProvider
) : CellViewModelFactory {

    override fun createCellViewModel(
        model: BaseCellModel,
        eventProvider: EventProvider
    ): BaseCellViewModel {
        return when (model) {
            is GroupCellModel -> GroupCellViewModel(model, eventProvider, resourceProvider)
            is NoteCellModel -> NoteCellViewModel(model, eventProvider, localeProvider)
            is DatabaseStatusCellModel -> DatabaseStatusCellViewModel(model)
            is OptionPanelCellModel -> OptionPanelCellViewModel(model, eventProvider)
            else -> throwUnsupportedModelException(model)
        }
    }
}