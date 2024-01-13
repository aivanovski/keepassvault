package com.ivanovsky.passnotes.presentation.diffViewer.factory

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.EncryptedDatabaseElement
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.data.entity.PropertyType
import com.ivanovsky.passnotes.domain.DateFormatProvider
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.entity.PropertyFilter
import com.ivanovsky.passnotes.domain.usecases.diff.entity.DiffEvent
import com.ivanovsky.passnotes.domain.usecases.diff.entity.DiffEventType
import com.ivanovsky.passnotes.domain.usecases.diff.entity.DiffListItem
import com.ivanovsky.passnotes.domain.usecases.diff.getBackgroundColor
import com.ivanovsky.passnotes.domain.usecases.diff.getCharacter
import com.ivanovsky.passnotes.domain.usecases.diff.getEntity
import com.ivanovsky.passnotes.domain.usecases.diff.getType
import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.core.model.SpaceCellModel
import com.ivanovsky.passnotes.presentation.diffViewer.cells.model.DiffCellModel
import com.ivanovsky.passnotes.presentation.diffViewer.cells.model.DiffFilesCellModel
import com.ivanovsky.passnotes.presentation.diffViewer.cells.model.DiffHeaderCellModel
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import java.util.Date
import kotlin.text.StringBuilder

class DiffViewerCellModelFactory(
    private val resourceProvider: ResourceProvider,
    private val dateFormatProvider: DateFormatProvider
) {

    fun createDiffModels(
        leftName: String,
        leftTime: Long?,
        rightName: String,
        rightTime: Long?,
        diff: List<DiffListItem>,
        collapsedEventIds: Set<Int>
    ): List<BaseCellModel> {
        val models = mutableListOf<BaseCellModel>()

        models.add(
            createFilesModel(
                leftName = leftName,
                leftTime = leftTime,
                rightName = rightName,
                rightTime = rightTime
            )
        )

        models.add(SpaceCellModel(R.dimen.half_margin))

        var eventId = 0
        var cellId = 1
        var indentLevel = 0
        for ((idx, item) in diff.withIndex()) {

            when (item) {
                is DiffListItem.Parent -> {
                    val model = createParentCell(
                        indentLevel = indentLevel,
                        cellId = cellId,
                        entity = item.entity
                    )

                    models.add(model)
                    indentLevel++
                    cellId++
                }

                is DiffListItem.Event -> {
                    val eventModels = createEventCells(
                        indentLevel = indentLevel,
                        eventId = eventId,
                        event = item.event,
                        isExpanded = (eventId !in collapsedEventIds),
                        startCellId = cellId
                    )

                    models.addAll(eventModels)

                    eventId++
                    cellId += eventModels.size
                }
            }

            val nextItem = diff.getOrNull(idx + 1)
            if (item is DiffListItem.Event && nextItem is DiffListItem.Parent) {
                indentLevel = 0
            }
        }

        return models
    }

    private fun createFilesModel(
        leftName: String,
        leftTime: Long?,
        rightName: String,
        rightTime: Long?
    ): DiffFilesCellModel {
        return DiffFilesCellModel(
            id = CellId.FILES,
            leftTitle = leftName,
            leftTime = leftTime?.formatDateAndTime() ?: EMPTY,
            rightTitle = rightName,
            rightTime = rightTime?.formatDateAndTime() ?: EMPTY
        )
    }

    private fun createParentCell(
        indentLevel: Int,
        cellId: Int,
        entity: EncryptedDatabaseElement?
    ): DiffHeaderCellModel {
        val text = StringBuilder()
            .apply {
                append("~").append(INDENT.repeat(indentLevel))

                when (entity) {
                    is Group -> {
                        append(
                            resourceProvider.getString(
                                R.string.diff_event_group,
                                entity.title
                            )
                        )
                    }

                    is Note -> {
                        append(
                            resourceProvider.getString(
                                R.string.diff_event_note,
                                entity.title
                            )
                        )
                    }

                    else -> {
                        append(resourceProvider.getString(R.string.unknown_entity))
                    }
                }
            }
            .toString()

        return DiffHeaderCellModel(
            id = cellId,
            backgroundColor = resourceProvider.getColor(R.color.transparent),
            text = text
        )
    }

    private fun createEventCells(
        indentLevel: Int,
        eventId: Int,
        event: DiffEvent<EncryptedDatabaseElement>,
        isExpanded: Boolean,
        startCellId: Int
    ): List<DiffCellModel> {
        val models = mutableListOf<DiffCellModel>()

        val type = event.getType()
        var cellId = startCellId

        when (val entity = event.getEntity()) {
            is Group -> {
                val text = StringBuilder()
                    .apply {
                        append(type.getCharacter()).append(INDENT.repeat(indentLevel))
                        append(
                            resourceProvider.getString(
                                R.string.diff_event_group,
                                entity.title
                            )
                        )
                    }
                    .toString()

                models.add(
                    DiffCellModel(
                        id = cellId,
                        eventId = eventId,
                        backgroundColor = type.getBackgroundColor(resourceProvider),
                        text = text,
                        iconResId = null
                    )
                )
            }

            is Note -> {
                models.add(
                    DiffCellModel(
                        id = cellId,
                        eventId = eventId,
                        backgroundColor = type.getBackgroundColor(resourceProvider),
                        iconResId = if (isExpanded) {
                            R.drawable.ic_expand_less_24dp
                        } else {
                            R.drawable.ic_expand_more_24dp
                        },
                        text = StringBuilder()
                            .apply {
                                append(type.getCharacter()).append(INDENT.repeat(indentLevel))
                                append(
                                    resourceProvider.getString(
                                        R.string.diff_event_note,
                                        entity.title
                                    )
                                )
                            }
                            .toString()
                    )
                )

                cellId++

                if (isExpanded) {
                    val defaultProperties = DEFAULT_PROPERTY_FILTER.apply(entity.properties)
                    val customProperties = CUSTOM_PROPERTY_FILTER.apply(entity.properties)

                    for (property in (defaultProperties + customProperties)) {
                        if (PropertyType.DEFAULT_TYPES.contains(property.type) && property.value.isNullOrEmpty()) {
                            continue
                        }

                        val model = createPropertyCell(
                            indentLevel = indentLevel + 1,
                            cellId = cellId,
                            eventId = eventId,
                            type = type,
                            property = property
                        )

                        models.add(model)

                        cellId++
                    }
                }
            }

            is Property -> {
                if (event is DiffEvent.Update) {
                    val oldProperty = event.oldEntity as Property
                    val newProperty = event.newEntity as Property

                    val model = DiffCellModel(
                        id = eventId,
                        eventId = eventId,
                        backgroundColor = type.getBackgroundColor(resourceProvider),
                        iconResId = null,
                        text = StringBuilder()
                            .apply {
                                append(type.getCharacter()).append(INDENT.repeat(indentLevel))
                                append(
                                    resourceProvider.getString(
                                        R.string.diff_event_property_update,
                                        newProperty.name,
                                        oldProperty.value,
                                        newProperty.value
                                    )
                                )
                            }
                            .toString(),
                    )

                    models.add(model)
                } else {
                    models.add(
                        createPropertyCell(
                            indentLevel = indentLevel,
                            cellId = cellId,
                            eventId = eventId,
                            type = type,
                            property = entity
                        )
                    )
                }
            }

            else -> {
                throw IllegalStateException()
            }
        }

        return models
    }

    private fun createPropertyCell(
        indentLevel: Int,
        cellId: Int,
        eventId: Int,
        type: DiffEventType,
        property: Property
    ): DiffCellModel {
        return DiffCellModel(
            id = cellId,
            eventId = eventId,
            backgroundColor = type.getBackgroundColor(resourceProvider),
            iconResId = null,
            text = StringBuilder()
                .apply {
                    append(type.getCharacter()).append(INDENT.repeat(indentLevel))
                    append(
                        resourceProvider.getString(
                            R.string.diff_event_property,
                            property.name,
                            property.value
                        )
                    )
                }
                .toString(),
        )
    }

    private fun Long.formatDateAndTime(): String {
        val date = Date(this)

        val dateFormat = dateFormatProvider.getLongDateFormat()
        val timeFormat = dateFormatProvider.getTimeFormat()

        return dateFormat.format(date) + " " + timeFormat.format(date)
    }

    object CellId {
        const val FILES = 1
    }

    companion object {
        private const val INDENT = "  "

        private val DEFAULT_PROPERTY_FILTER = PropertyFilter.Builder()
            .filterDefaultTypes()
            .excludeTitle()
            .build()

        private val CUSTOM_PROPERTY_FILTER = PropertyFilter.Builder()
            .excludeDefaultTypes()
            .build()
    }
}