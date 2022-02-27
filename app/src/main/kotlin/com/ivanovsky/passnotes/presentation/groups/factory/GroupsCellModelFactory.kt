package com.ivanovsky.passnotes.presentation.groups.factory

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.interactor.groups.GroupsInteractor
import com.ivanovsky.passnotes.presentation.core.factory.CellModelFactory
import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.core.model.GroupCellModel
import com.ivanovsky.passnotes.presentation.core.model.NoteCellModel
import com.ivanovsky.passnotes.presentation.core.model.OptionPanelCellModel
import com.ivanovsky.passnotes.presentation.groups.GroupsViewModel.OptionPanelState
import com.ivanovsky.passnotes.presentation.groups.GroupsViewModel.OptionPanelState.*
import com.ivanovsky.passnotes.util.StringUtils

class GroupsCellModelFactory(
    private val resourceProvider: ResourceProvider
) : CellModelFactory<List<GroupsInteractor.Item>> {

    override fun createCellModels(data: List<GroupsInteractor.Item>): List<BaseCellModel> {
        return data.mapNotNull { item ->
            when (item) {
                is GroupsInteractor.GroupItem -> {
                    GroupCellModel(
                        id = item.group.uid.toString(),
                        group = item.group,
                        noteCount = item.noteCount,
                        childGroupCount = item.childGroupCount
                    )
                }
                is GroupsInteractor.NoteItem -> {
                    NoteCellModel(
                        id = item.note.uid?.toString() ?: "",
                        note = item.note
                    )
                }
                else -> null
            }
        }
    }

    fun createOptionPanelCellModel(state: OptionPanelState): OptionPanelCellModel {
        return when (state) {
            PASTE -> OptionPanelCellModel(
                id = OPTION_PANEL_CELL_ID,
                positiveText = resourceProvider.getString(R.string.paste),
                negativeText = resourceProvider.getString(R.string.cancel),
                message = StringUtils.EMPTY,
                isVisible = true
            )
            SAVE_AUTOFILL_DATA -> OptionPanelCellModel(
                id = OPTION_PANEL_CELL_ID,
                positiveText = resourceProvider.getString(R.string.yes),
                negativeText = resourceProvider.getString(R.string.discard),
                message = resourceProvider.getString(R.string.autofill_save_note_message),
                isVisible = true
            )
            HIDDEN -> OptionPanelCellModel(
                id = OPTION_PANEL_CELL_ID,
                positiveText = StringUtils.EMPTY,
                negativeText = StringUtils.EMPTY,
                message = StringUtils.EMPTY,
                isVisible = false
            )
        }
    }

    companion object {
        private const val OPTION_PANEL_CELL_ID = "optionPanelCellId"
    }
}
