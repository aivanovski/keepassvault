package com.ivanovsky.passnotes.presentation.main.navigation.cells.factory

import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.core.factory.CellViewModelFactory
import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.core.model.SingleTextWithIconCellModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.SingleTextWithIconCellViewModel
import com.ivanovsky.passnotes.presentation.main.navigation.cells.model.NavigationHeaderCellModel
import com.ivanovsky.passnotes.presentation.main.navigation.cells.viewmodel.NavigationHeaderCellViewModel

class NavigationMenuCellViewModelFactory : CellViewModelFactory {

    override fun createCellViewModel(
        model: BaseCellModel,
        eventProvider: EventProvider
    ): BaseCellViewModel {
        return when (model) {
            is NavigationHeaderCellModel -> NavigationHeaderCellViewModel(
                model
            )
            is SingleTextWithIconCellModel -> SingleTextWithIconCellViewModel(
                model,
                eventProvider
            )
            else -> throwUnsupportedModelException(model)
        }
    }
}