package com.ivanovsky.passnotes.presentation.note.factory

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.core.model.DividerCellModel
import com.ivanovsky.passnotes.presentation.core.model.HeaderCellModel
import com.ivanovsky.passnotes.presentation.note.cells.model.NotePropertyCellModel
import com.ivanovsky.passnotes.presentation.core.model.SpaceCellModel
import com.ivanovsky.passnotes.presentation.core.widget.entity.RoundedShape
import com.ivanovsky.passnotes.util.StringUtils

class NoteCellModelFactory(
    private val resourceProvider: ResourceProvider
) {

    fun createCellModels(
        visibleIdsAndProperties: List<Pair<String, Property>>,
        hiddenIdsAndProperties: List<Pair<String, Property>>
    ): List<BaseCellModel> {
        val models = mutableListOf<BaseCellModel>()

        for ((idx, cellIdAndProperty) in visibleIdsAndProperties.withIndex()) {
            val (cellId, property) = cellIdAndProperty

            if (idx == 0) {
                models.add(SpaceCellModel(R.dimen.element_margin))
            }

            val shape = when {
                visibleIdsAndProperties.size == 1 -> RoundedShape.ALL
                idx == 0 -> RoundedShape.TOP
                idx == visibleIdsAndProperties.lastIndex -> RoundedShape.BOTTOM
                else -> RoundedShape.NONE
            }

            models.add(
                NotePropertyCellModel(
                    id = cellId,
                    name = property.name ?: StringUtils.EMPTY,
                    value = property.value ?: StringUtils.EMPTY,
                    backgroundShape = shape,
                    isVisibilityButtonVisible = property.isProtected,
                    isValueProtected = property.isProtected
                )
            )

            if (idx == visibleIdsAndProperties.lastIndex) {
                if (hiddenIdsAndProperties.isEmpty()) {
                    models.add(SpaceCellModel(R.dimen.huge_margin))
                } else {
                    models.add(createHiddenHeaderCell())
                }
            } else {
                models.add(createDividerCell())
            }
        }

        for ((idx, cellIdAndProperty) in hiddenIdsAndProperties.withIndex()) {
            val (cellId, property) = cellIdAndProperty

            val shape = when {
                hiddenIdsAndProperties.size == 1 -> RoundedShape.ALL
                idx == 0 -> RoundedShape.TOP
                idx == hiddenIdsAndProperties.lastIndex -> RoundedShape.BOTTOM
                else -> RoundedShape.NONE
            }

            models.add(
                NotePropertyCellModel(
                    id = cellId,
                    name = property.name ?: StringUtils.EMPTY,
                    value = property.value ?: StringUtils.EMPTY,
                    backgroundShape = shape,
                    isVisibilityButtonVisible = property.isProtected,
                    isValueProtected = property.isProtected
                )
            )

            if (idx == hiddenIdsAndProperties.lastIndex) {
                models.add(SpaceCellModel(R.dimen.huge_margin))
            } else {
                models.add(createDividerCell())
            }
        }

        return models
    }

    private fun createDividerCell(): DividerCellModel =
        DividerCellModel(
            color = resourceProvider.getColor(R.color.transparent),
            paddingStart = R.dimen.element_margin,
            paddingEnd = R.dimen.element_margin
        )

    private fun createHiddenHeaderCell(): HeaderCellModel =
        HeaderCellModel(
            id = null,
            title = resourceProvider.getString(R.string.hidden),
            color = resourceProvider.getColor(R.color.secondary_text),
            paddingHorizontal = R.dimen.double_element_margin
        )
}