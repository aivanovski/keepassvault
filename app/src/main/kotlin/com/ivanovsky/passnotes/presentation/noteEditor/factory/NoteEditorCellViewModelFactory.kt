package com.ivanovsky.passnotes.presentation.noteEditor.factory

import com.ivanovsky.passnotes.domain.DateFormatProvider
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.core.factory.CellViewModelFactory
import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.core.model.DividerCellModel
import com.ivanovsky.passnotes.presentation.core.model.HeaderCellModel
import com.ivanovsky.passnotes.presentation.core.model.SpaceCellModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.DividerCellViewModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.HeaderCellViewModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.SpaceCellViewModel
import com.ivanovsky.passnotes.presentation.noteEditor.cells.model.AttachmentCellModel
import com.ivanovsky.passnotes.presentation.noteEditor.cells.model.ExpirationCellModel
import com.ivanovsky.passnotes.presentation.noteEditor.cells.model.ExtendedTextPropertyCellModel
import com.ivanovsky.passnotes.presentation.noteEditor.cells.model.SecretPropertyCellModel
import com.ivanovsky.passnotes.presentation.noteEditor.cells.model.TextPropertyCellModel
import com.ivanovsky.passnotes.presentation.noteEditor.cells.viewmodel.AttachmentCellViewModel
import com.ivanovsky.passnotes.presentation.noteEditor.cells.viewmodel.ExpirationCellViewModel
import com.ivanovsky.passnotes.presentation.noteEditor.cells.viewmodel.ExtendedTextPropertyCellViewModel
import com.ivanovsky.passnotes.presentation.noteEditor.cells.viewmodel.SecretPropertyCellViewModel
import com.ivanovsky.passnotes.presentation.noteEditor.cells.viewmodel.TextPropertyCellViewModel

class NoteEditorCellViewModelFactory(
    private val resourceProvider: ResourceProvider,
    private val dateFormatProvider: DateFormatProvider
) : CellViewModelFactory {

    override fun createCellViewModel(
        model: BaseCellModel,
        eventProvider: EventProvider
    ): BaseCellViewModel {
        return when (model) {
            is TextPropertyCellModel -> TextPropertyCellViewModel(
                model,
                eventProvider,
                resourceProvider
            )
            is SecretPropertyCellModel -> SecretPropertyCellViewModel(
                model,
                eventProvider,
                resourceProvider
            )
            is ExtendedTextPropertyCellModel -> ExtendedTextPropertyCellViewModel(
                model,
                eventProvider,
                resourceProvider
            )
            is SpaceCellModel -> SpaceCellViewModel(
                model,
                resourceProvider
            )
            is AttachmentCellModel -> AttachmentCellViewModel(
                model,
                eventProvider
            )
            is DividerCellModel -> DividerCellViewModel(
                model,
                resourceProvider
            )
            is HeaderCellModel -> HeaderCellViewModel(
                model,
                eventProvider,
                resourceProvider
            )
            is ExpirationCellModel -> ExpirationCellViewModel(
                model,
                eventProvider,
                dateFormatProvider
            )
            else -> throwUnsupportedModelException(model)
        }
    }
}