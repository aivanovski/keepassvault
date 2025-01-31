package com.ivanovsky.passnotes.presentation.core.dialog.optionDialog.factory

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.presentation.core.dialog.optionDialog.model.OptionItem
import com.ivanovsky.passnotes.presentation.core.factory.CellModelFactory
import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.core.model.DividerCellModel
import com.ivanovsky.passnotes.presentation.core.model.OneLineTextCellModel
import com.ivanovsky.passnotes.presentation.core.model.TwoLineTextCellModel

class OptionDialogCellModelFactory(
    private val resourceProvider: ResourceProvider
) : CellModelFactory<List<OptionItem>> {

    override fun createCellModels(data: List<OptionItem>): List<BaseCellModel> {
        val models = mutableListOf<BaseCellModel>()

        for ((index, item) in data.withIndex()) {
            if (index > 0) {
                models.add(createDividerCell())
            }

            val model = if (!item.description.isNullOrEmpty()) {
                TwoLineTextCellModel(
                    id = index.toString(),
                    title = item.title,
                    description = item.description
                )
            } else {
                OneLineTextCellModel(
                    id = index.toString(),
                    text = item.title
                )
            }

            models.add(model)
        }

        return models
    }

    private fun createDividerCell(): DividerCellModel =
        DividerCellModel(
            color = resourceProvider.getAttributeColor(R.attr.kpDividerColor),
            paddingStart = null,
            paddingEnd = null
        )
}