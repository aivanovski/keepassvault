package com.ivanovsky.passnotes.presentation.groups.factory

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.EncryptedDatabaseEntry
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.presentation.core.factory.CellModelFactory
import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.core.model.GroupCellModel
import com.ivanovsky.passnotes.presentation.core.model.NoteCellModel
import com.ivanovsky.passnotes.presentation.core.model.OptionPanelCellModel
import com.ivanovsky.passnotes.presentation.groups.GroupsViewModel.OptionPanelState
import com.ivanovsky.passnotes.presentation.groups.GroupsViewModel.OptionPanelState.HIDDEN
import com.ivanovsky.passnotes.presentation.groups.GroupsViewModel.OptionPanelState.PASTE
import com.ivanovsky.passnotes.presentation.groups.GroupsViewModel.OptionPanelState.SAVE_AUTOFILL_DATA
import com.ivanovsky.passnotes.util.StringUtils

class GroupsCellModelFactory(
    private val resourceProvider: ResourceProvider
) : CellModelFactory<List<EncryptedDatabaseEntry>> {

    override fun createCellModels(data: List<EncryptedDatabaseEntry>): List<BaseCellModel> {
        return data.map { item ->
            when (item) {
                is Group -> {
                    GroupCellModel(
                        id = item.uid.toString(),
                        group = item,
                        noteCount = item.noteCount,
                        childGroupCount = item.groupCount
                    )
                }
                is Note -> {
                    NoteCellModel(
                        id = item.uid?.toString() ?: "",
                        note = item
                    )
                }
                else -> throw IllegalStateException()
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