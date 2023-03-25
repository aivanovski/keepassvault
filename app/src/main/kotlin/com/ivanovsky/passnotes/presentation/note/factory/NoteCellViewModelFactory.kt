package com.ivanovsky.passnotes.presentation.note.factory

import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.core.factory.CellViewModelFactory
import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.core.model.MessageCellModel
import com.ivanovsky.passnotes.presentation.core.model.DividerCellModel
import com.ivanovsky.passnotes.presentation.core.model.HeaderCellModel
import com.ivanovsky.passnotes.presentation.core.model.SpaceCellModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.MessageCellViewModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.DividerCellViewModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.HeaderCellViewModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.SpaceCellViewModel
import com.ivanovsky.passnotes.presentation.note.cells.model.AttachmentCellModel
import com.ivanovsky.passnotes.presentation.note.cells.model.NotePropertyCellModel
import com.ivanovsky.passnotes.presentation.note.cells.viewmodel.AttachmentCellViewModel
import com.ivanovsky.passnotes.presentation.note.cells.viewmodel.NotePropertyCellViewModel

class NoteCellViewModelFactory(
    private val resourceProvider: ResourceProvider
) : CellViewModelFactory {

    override fun createCellViewModel(
        model: BaseCellModel,
        eventProvider: EventProvider
    ): BaseCellViewModel {
        return when (model) {
            is NotePropertyCellModel -> NotePropertyCellViewModel(
                model,
                eventProvider
            )
            is DividerCellModel -> DividerCellViewModel(
                model,
                resourceProvider
            )
            is SpaceCellModel -> SpaceCellViewModel(
                model,
                resourceProvider
            )
            is HeaderCellModel -> HeaderCellViewModel(
                model,
                resourceProvider
            )
            is AttachmentCellModel -> AttachmentCellViewModel(
                model,
                eventProvider
            )
            is MessageCellModel -> MessageCellViewModel(model)
            else -> throwUnsupportedModelException(model)
        }
    }
}