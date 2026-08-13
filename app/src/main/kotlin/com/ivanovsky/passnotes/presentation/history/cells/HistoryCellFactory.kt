package com.ivanovsky.passnotes.presentation.history.cells

import androidx.compose.ui.graphics.Color
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.domain.DateFormatter
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.usecases.diff.entity.DiffEvent
import com.ivanovsky.passnotes.domain.usecases.diff.getEntity
import com.ivanovsky.passnotes.domain.usecases.history.entity.HistoryDiffItem
import com.ivanovsky.passnotes.presentation.core.compose.ElementMargin
import com.ivanovsky.passnotes.presentation.core.compose.cells.CellEventProvider
import com.ivanovsky.passnotes.presentation.core.compose.cells.CellIdGenerator
import com.ivanovsky.passnotes.presentation.core.compose.cells.CellModel
import com.ivanovsky.passnotes.presentation.core.compose.cells.CellViewModel
import com.ivanovsky.passnotes.presentation.core.compose.cells.IllegalCellModelException
import com.ivanovsky.passnotes.presentation.core.compose.cells.IntCellId
import com.ivanovsky.passnotes.presentation.core.compose.cells.model.DividerCellModel
import com.ivanovsky.passnotes.presentation.core.compose.cells.toBaseId
import com.ivanovsky.passnotes.presentation.core.compose.cells.viewModel.DividerCellViewModel
import com.ivanovsky.passnotes.presentation.core.widget.entity.RoundedShape
import com.ivanovsky.passnotes.presentation.history.cells.model.HistoryDiffCellModel
import com.ivanovsky.passnotes.presentation.history.cells.model.HistoryDiffPlaceholderCellModel
import com.ivanovsky.passnotes.presentation.history.cells.model.HistoryHeaderCellModel
import com.ivanovsky.passnotes.presentation.history.cells.viewModel.HistoryDiffCellViewModel
import com.ivanovsky.passnotes.presentation.history.cells.viewModel.HistoryDiffPlaceholderCellViewModel
import com.ivanovsky.passnotes.presentation.history.cells.viewModel.HistoryHeaderCellViewModel
import com.ivanovsky.passnotes.util.StringUtils

class HistoryCellFactory(
    private val resourceProvider: ResourceProvider,
    private val dateFormatter: DateFormatter
) {

    fun createHistoryDiffCellViewModels(
        diff: List<HistoryDiffItem>,
        eventProvider: CellEventProvider
    ): List<CellViewModel> {
        val models = mutableListOf<CellModel>()

        val idGenerator = CellIdGenerator(startFrom = 1.toBaseId())

        for ((diffItemIndex, item) in diff.withIndex()) {
            models.add(
                createHeaderCell(
                    cellId = idGenerator.nextId().id,
                    noteIndex = diffItemIndex,
                    title = dateFormatter.formatDateAndTime(item.newNote.modified)
                )
            )

            val eventCount = item.diffEvents.size

            if (item.diffEvents.isNotEmpty()) {
                for ((eventIndex, event) in item.diffEvents.withIndex()) {
                    models.add(
                        createDiffCell(
                            cellId = idGenerator.nextId().id,
                            event = event,
                            diffItemIndex = diffItemIndex,
                            eventIndex = eventIndex,
                            eventCount = eventCount
                        )
                    )

                    if (eventIndex < eventCount - 1 && eventCount > 1) {
                        models.add(createDividerCell(idGenerator.nextId()))
                    }
                }
            } else {
                models.add(createEmptyDiffCell(idGenerator.nextId().id))
            }
        }

        val oldestNote = diff.last().oldNote

        models.add(
            createHeaderCell(
                cellId = idGenerator.nextId().id,
                noteIndex = FIRST_VERSION_INDEX,
                title = dateFormatter.formatDateAndTime(oldestNote.created)
            )
        )

        return models.createViewModel(eventProvider = eventProvider)
    }

    private fun List<CellModel>.createViewModel(
        eventProvider: CellEventProvider
    ): List<CellViewModel> {
        return this.map { model ->
            when (model) {
                is HistoryHeaderCellModel -> HistoryHeaderCellViewModel(model, eventProvider)
                is HistoryDiffCellModel -> HistoryDiffCellViewModel(model, eventProvider)
                is HistoryDiffPlaceholderCellModel -> HistoryDiffPlaceholderCellViewModel(model)
                is DividerCellModel -> DividerCellViewModel(model)
                else -> throw IllegalCellModelException(model)
            }
        }
    }

    private fun createEmptyDiffCell(
        cellId: Int
    ): HistoryDiffPlaceholderCellModel {
        return HistoryDiffPlaceholderCellModel(
            id = cellId,
            title = resourceProvider.getString(R.string.no_changes)
        )
    }

    private fun createDiffCell(
        cellId: Int,
        event: DiffEvent<Property>,
        diffItemIndex: Int,
        eventIndex: Int,
        eventCount: Int
    ): HistoryDiffCellModel {
        val backgroundShape = when {
            eventCount == 1 -> RoundedShape.ALL
            eventIndex == 0 -> RoundedShape.TOP
            eventIndex == eventCount - 1 -> RoundedShape.BOTTOM
            else -> RoundedShape.NONE
        }

        val eventName = when (event) {
            is DiffEvent.Update -> resourceProvider.getString(R.string.changed)
            is DiffEvent.Insert -> resourceProvider.getString(R.string.added)
            is DiffEvent.Delete -> resourceProvider.getString(R.string.deleted)
        }

        val backgroundColor = when (event) {
            is DiffEvent.Update -> resourceProvider.getColor(R.color.diff_update)
            is DiffEvent.Insert -> resourceProvider.getColor(R.color.diff_insert)
            is DiffEvent.Delete -> resourceProvider.getColor(R.color.diff_delete)
        }

        val eventId = "$diffItemIndex:$eventIndex"

        return if (event is DiffEvent.Update) {
            val oldProperty = event.oldEntity
            val newProperty = event.newEntity

            HistoryDiffCellModel(
                id = cellId,
                eventId = eventId,
                name = newProperty.name ?: StringUtils.EMPTY,
                value = resourceProvider.getString(
                    R.string.history_event_property_updated,
                    oldProperty.value,
                    newProperty.value
                ),
                event = eventName,
                backgroundShape = backgroundShape,
                backgroundColor = backgroundColor
            )
        } else {
            val property = event.getEntity()

            HistoryDiffCellModel(
                id = cellId,
                eventId = eventId,
                name = property.name ?: StringUtils.EMPTY,
                value = property.value ?: StringUtils.EMPTY,
                event = eventName,
                backgroundShape = backgroundShape,
                backgroundColor = backgroundColor
            )
        }
    }

    private fun createHeaderCell(
        cellId: Int,
        noteIndex: Int,
        title: String
    ): HistoryHeaderCellModel {
        return HistoryHeaderCellModel(
            id = cellId,
            itemId = noteIndex,
            title = title,
            description = resourceProvider.getString(R.string.view),
            descriptionIcon = R.drawable.ic_chevron_right_24dp
        )
    }

    private fun createDividerCell(id: IntCellId): DividerCellModel {
        return DividerCellModel(
            id = id,
            color = Color.Transparent,
            paddingStart = ElementMargin,
            paddingEnd = ElementMargin
        )
    }

    companion object {
        const val FIRST_VERSION_INDEX = -1
    }
}