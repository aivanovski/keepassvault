package com.ivanovsky.passnotes.presentation.diffViewer.factory

import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.core.factory.CellViewModelFactory
import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.core.model.DividerCellModel
import com.ivanovsky.passnotes.presentation.core.model.SpaceCellModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.DividerCellViewModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.SpaceCellViewModel
import com.ivanovsky.passnotes.presentation.diffViewer.cells.model.DiffCellModel
import com.ivanovsky.passnotes.presentation.diffViewer.cells.model.DiffFilesCellModel
import com.ivanovsky.passnotes.presentation.diffViewer.cells.model.DiffHeaderCellModel
import com.ivanovsky.passnotes.presentation.diffViewer.cells.viewmodel.DiffCellViewModel
import com.ivanovsky.passnotes.presentation.diffViewer.cells.viewmodel.DiffFilesCellViewModel
import com.ivanovsky.passnotes.presentation.diffViewer.cells.viewmodel.DiffHeaderCellViewModel

class DiffViewerCellViewModelFactory(
    private val resourceProvider: ResourceProvider
) : CellViewModelFactory {

    override fun createCellViewModel(
        model: BaseCellModel,
        eventProvider: EventProvider
    ): BaseCellViewModel {
        return when (model) {
            is SpaceCellModel -> SpaceCellViewModel(model, resourceProvider)
            is DiffHeaderCellModel -> DiffHeaderCellViewModel(model)
            is DiffCellModel -> DiffCellViewModel(model, eventProvider)
            is DiffFilesCellModel -> DiffFilesCellViewModel(model, eventProvider)
            is DividerCellModel -> DividerCellViewModel(model, resourceProvider)
            else -> throwUnsupportedModelException(model)
        }
    }
}