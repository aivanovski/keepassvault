package com.ivanovsky.passnotes.presentation.note_editor.factory

import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.core.factory.CellViewModelFactory
import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.core.model.SpaceCellModel
import com.ivanovsky.passnotes.presentation.core.viewmodels.SpaceCellViewModel
import com.ivanovsky.passnotes.presentation.note_editor.cells.model.ExtendedTextPropertyCellModel
import com.ivanovsky.passnotes.presentation.note_editor.cells.model.SecretPropertyCellModel
import com.ivanovsky.passnotes.presentation.note_editor.cells.model.TextPropertyCellModel
import com.ivanovsky.passnotes.presentation.note_editor.cells.viewmodel.ExtendedTextPropertyCellViewModel
import com.ivanovsky.passnotes.presentation.note_editor.cells.viewmodel.SecretPropertyCellViewModel
import com.ivanovsky.passnotes.presentation.note_editor.cells.viewmodel.TextPropertyCellViewModel

class NoteEditorCellViewModelFactory(
    private val resourceProvider: ResourceProvider
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
                model
            )
            else -> throwUnsupportedModelException(model)
        }
    }
}