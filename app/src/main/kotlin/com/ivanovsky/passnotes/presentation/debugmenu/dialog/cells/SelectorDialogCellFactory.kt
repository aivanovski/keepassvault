package com.ivanovsky.passnotes.presentation.debugmenu.dialog.cells

import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.debugmenu.dialog.cells.model.CheckboxCellModel
import com.ivanovsky.passnotes.presentation.debugmenu.dialog.cells.model.EditableTextCellModel
import com.ivanovsky.passnotes.presentation.debugmenu.dialog.cells.viewModel.CheckboxCellViewModel
import com.ivanovsky.passnotes.presentation.debugmenu.dialog.cells.viewModel.EditableTextCellViewModel
import com.ivanovsky.passnotes.presentation.debugmenu.dialog.model.SelectorOption

class SelectorDialogCellFactory(
    private val resourceProvider: ResourceProvider
) {

    fun createSearchCell(
        eventProvider: EventProvider
    ): EditableTextCellViewModel {
        return EditableTextCellViewModel(
            initModel = EditableTextCellModel(
                id = "id",
                text = "",
                hint = "Type to filetr"
            ),
            eventProvider = eventProvider
        )
    }

    fun createOptionCells(
        options: List<SelectorOption>,
        selectedIndices: List<Int>,
        eventProvider: EventProvider
    ): List<CheckboxCellViewModel> {
        val cells = mutableListOf<CheckboxCellViewModel>()

        val selectedIndicesSet = selectedIndices.toSet()

        for ((index, option) in options.withIndex()) {
            cells.add(
                CheckboxCellViewModel(
                    initModel = CheckboxCellModel(
                        id = "item_$index",
                        title = option.title,
                        description = option.description,
                        isChecked = index in selectedIndicesSet
                    ),
                    eventProvider = eventProvider
                )
            )
        }

        return cells
    }
}