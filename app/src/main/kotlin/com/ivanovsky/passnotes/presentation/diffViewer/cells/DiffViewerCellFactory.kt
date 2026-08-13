package com.ivanovsky.passnotes.presentation.diffViewer.cells

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.domain.DateFormatProvider
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.entity.Timestamp
import com.ivanovsky.passnotes.domain.usecases.diff.entity.DiffEvent
import com.ivanovsky.passnotes.domain.usecases.diff.entity.DiffListItem
import com.ivanovsky.passnotes.domain.usecases.diff.getEntity
import com.ivanovsky.passnotes.presentation.core.ThemeProvider
import com.ivanovsky.passnotes.presentation.core.compose.DoubleElementMargin
import com.ivanovsky.passnotes.presentation.core.compose.ElementMargin
import com.ivanovsky.passnotes.presentation.core.compose.GroupMargin
import com.ivanovsky.passnotes.presentation.core.compose.cells.CellEventProvider
import com.ivanovsky.passnotes.presentation.core.compose.cells.CellIdGenerator
import com.ivanovsky.passnotes.presentation.core.compose.cells.CellModel
import com.ivanovsky.passnotes.presentation.core.compose.cells.CellViewModel
import com.ivanovsky.passnotes.presentation.core.compose.cells.IllegalCellModelException
import com.ivanovsky.passnotes.presentation.core.compose.cells.IntCellId
import com.ivanovsky.passnotes.presentation.core.compose.cells.model.ErrorPanelCellModel
import com.ivanovsky.passnotes.presentation.core.compose.cells.model.SpaceCellModel
import com.ivanovsky.passnotes.presentation.core.compose.cells.toBaseId
import com.ivanovsky.passnotes.presentation.core.compose.cells.toId
import com.ivanovsky.passnotes.presentation.core.compose.cells.viewModel.ErrorPanelCellViewModel
import com.ivanovsky.passnotes.presentation.core.compose.cells.viewModel.SpaceCellViewModel
import com.ivanovsky.passnotes.presentation.core.compose.toComposeTheme
import com.ivanovsky.passnotes.presentation.core.widget.entity.RoundedShape
import com.ivanovsky.passnotes.presentation.diffViewer.cells.model.DiffHeaderCellModel
import com.ivanovsky.passnotes.presentation.diffViewer.cells.model.EntryDiffCellModel
import com.ivanovsky.passnotes.presentation.diffViewer.cells.model.EventType
import com.ivanovsky.passnotes.presentation.diffViewer.cells.model.Field
import com.ivanovsky.passnotes.presentation.diffViewer.cells.model.FieldValue
import com.ivanovsky.passnotes.presentation.diffViewer.cells.model.GroupDiffCellModel
import com.ivanovsky.passnotes.presentation.diffViewer.cells.viewmodel.DiffHeaderCellViewModel
import com.ivanovsky.passnotes.presentation.diffViewer.cells.viewmodel.EntryDiffCellViewModel
import com.ivanovsky.passnotes.presentation.diffViewer.cells.viewmodel.GroupDiffCellViewModel
import java.util.Date

class DiffViewerCellFactory(
    private val resourceProvider: ResourceProvider,
    private val themeProvider: ThemeProvider,
    private val dateFormatProvider: DateFormatProvider
) {

    fun createErrorPanelCell(
        error: OperationError,
        isErrorState: Boolean,
        eventProvider: CellEventProvider
    ): ErrorPanelCellViewModel {
        val theme = themeProvider.resolveTheme().toComposeTheme()

        val background = if (isErrorState) {
            null
        } else {
            theme.colors.errorBackground
        }

        return ErrorPanelCellViewModel(
            model = ErrorPanelCellModel(
                id = CellIds.ERROR_PANEL,
                error = error,
                background = background,
                isCloseButtonVisible = !isErrorState,
                actionId = null,
                actionButtonText = null
            ),
            eventProvider = eventProvider
        )
    }

    fun createMergeCellViewModel(
        localDiffItems: List<Pair<IntCellId, DiffListItem>>,
        remoteDiffItems: List<Pair<IntCellId, DiffListItem>>,
        localTimestamp: Timestamp?,
        remoteTimestamp: Timestamp?,
        eventProvider: CellEventProvider
    ): List<CellViewModel> {
        val models = mutableListOf<CellModel>()

        val idGenerator = CellIdGenerator(startFrom = CellIds.SPACE)
        models.add(SpaceCellModel(id = idGenerator.nextId(), height = ElementMargin))

        models.add(
            createLocalHeaderCell(
                time = localTimestamp?.formatDateAndTime().orEmpty()
            )
        )
        models.add(SpaceCellModel(id = idGenerator.nextId(), height = ElementMargin))

        models.addAll(
            createDiffCells(
                items = localDiffItems,
                isAllCheckable = true,
                isAllChecked = true,
                idCounter = idGenerator
            )
        )

        models.add(SpaceCellModel(id = idGenerator.nextId(), height = DoubleElementMargin))

        models.add(
            createRemoteHeaderCell(
                time = remoteTimestamp?.formatDateAndTime().orEmpty()
            )
        )
        models.add(SpaceCellModel(id = idGenerator.nextId(), height = ElementMargin))

        models.addAll(
            createDiffCells(
                items = remoteDiffItems,
                isAllCheckable = true,
                isAllChecked = true,
                idCounter = idGenerator
            )
        )

        models.add(SpaceCellModel(id = idGenerator.nextId(), height = GroupMargin))

        return models.createViewModels(eventProvider)
    }

    fun createCompareCellViewModels(
        diffItems: List<Pair<IntCellId, DiffListItem>>,
        eventProvider: CellEventProvider
    ): List<CellViewModel> {
        val models = mutableListOf<CellModel>()

        val idCounter = CellIdGenerator(startFrom = CellIds.SPACE)
        models.add(SpaceCellModel(id = idCounter.nextId(), height = GroupMargin))

        models.addAll(
            createDiffCells(
                items = diffItems,
                isAllCheckable = false,
                isAllChecked = false,
                idCounter = idCounter
            )
        )

        models.add(SpaceCellModel(id = idCounter.nextId(), height = GroupMargin))

        return models.createViewModels(eventProvider)
    }

    private fun createDiffCells(
        items: List<Pair<IntCellId, DiffListItem>>,
        isAllCheckable: Boolean,
        isAllChecked: Boolean,
        idCounter: CellIdGenerator
    ): List<CellModel> {
        val models = mutableListOf<CellModel>()

        for ((index, idAndItem) in items.withIndex()) {
            val (id, item) = idAndItem

            val model = when (item) {
                is DiffListItem.GroupItem -> createGroupModel(
                    id = id,
                    item = item,
                    isCheckable = isAllCheckable,
                    isChecked = isAllChecked
                )

                is DiffListItem.NoteItem -> createNoteModel(
                    id = id,
                    item = item,
                    isCheckable = isAllCheckable,
                    isChecked = isAllChecked
                )

                is DiffListItem.PropertiesItem -> createPropertiesModel(
                    id = id,
                    item = item,
                    isCheckable = isAllCheckable,
                    isChecked = isAllChecked
                )
            }

            if (index > 0) {
                models.add(SpaceCellModel(id = idCounter.nextId(), height = ElementMargin))
            }
            models.add(model)
        }

        return models
    }

    private fun List<CellModel>.createViewModels(
        eventProvider: CellEventProvider
    ): List<CellViewModel> {
        return this.map { model ->
            when (model) {
                is DiffHeaderCellModel -> DiffHeaderCellViewModel(model, eventProvider)
                is EntryDiffCellModel -> EntryDiffCellViewModel(model, eventProvider)
                is GroupDiffCellModel -> GroupDiffCellViewModel(model, eventProvider)
                is SpaceCellModel -> SpaceCellViewModel(model)
                else -> throw IllegalCellModelException(model)
            }
        }
    }

    private fun createGroupModel(
        id: IntCellId,
        item: DiffListItem.GroupItem,
        isCheckable: Boolean,
        isChecked: Boolean
    ): CellModel {
        val event = item.event
        val group = event.getEntity()

        val path = item.parents.joinToString(
            separator = " / ",
            transform = { group -> group.title }
        )

        return GroupDiffCellModel(
            id = id,
            title = resourceProvider.getString(R.string.group_with_str, group.title),
            description = event.formatDescription(),
            path = path,
            chipBackgroundTint = event.getIconTint(themeProvider),
            isCheckable = isCheckable,
            isChecked = isChecked
        )
    }

    private fun createNoteModel(
        id: IntCellId,
        item: DiffListItem.NoteItem,
        isCheckable: Boolean,
        isChecked: Boolean
    ): CellModel {
        val entity = item.event.getEntity()

        val fields = entity.properties
            .filter { property -> !property.value.isNullOrEmpty() }
            .toFields(item.event)

        val path = item.parents.joinToString(
            separator = " / ",
            transform = { group -> group.title }
        )

        return EntryDiffCellModel(
            id = id,
            eventType = item.event.toCellEventType(),
            title = resourceProvider.getString(R.string.entry_with_str, entity.title),
            path = path,
            fields = fields,
            icon = item.event.getIcon(),
            iconBackgroundTint = item.event.getIconTint(themeProvider),
            accentTextColor = item.event.getAccentTextColor(themeProvider),
            isCheckable = isCheckable,
            isChecked = isChecked,
            isExpanded = true
        )
    }

    private fun createPropertiesModel(
        id: IntCellId,
        item: DiffListItem.PropertiesItem,
        isCheckable: Boolean,
        isChecked: Boolean
    ): CellModel {
        val note = item.note

        val fields = item.events.mapIndexed { index, event ->
            val shape = when {
                item.events.size == 1 -> RoundedShape.ALL
                index == 0 -> RoundedShape.TOP
                index == item.events.lastIndex -> RoundedShape.BOTTOM
                else -> RoundedShape.NONE
            }

            when (event) {
                is DiffEvent.Insert -> {
                    val entity = event.entity

                    Field(
                        eventType = EventType.INSERT,
                        name = entity.name.orEmpty(),
                        value = FieldValue.Value(entity.value.orEmpty()),
                        backgroundShape = shape
                    )
                }

                is DiffEvent.Delete -> {
                    val entity = event.entity

                    Field(
                        eventType = EventType.DELETE,
                        name = entity.name.orEmpty(),
                        value = FieldValue.Value(entity.value.orEmpty()),
                        backgroundShape = shape
                    )
                }

                is DiffEvent.Update -> {
                    val old = event.oldEntity
                    val new = event.newEntity

                    Field(
                        eventType = EventType.UPDATE,
                        name = new.name.orEmpty(),
                        value = FieldValue.Update(
                            oldValue = old.value.orEmpty(),
                            newValue = new.value.orEmpty()
                        ),
                        backgroundShape = shape
                    )
                }
            }
        }

        val firstEvent = item.events.first()

        val path = item.parentGroups.joinToString(
            separator = " / ",
            transform = { group -> group.title }
        )

        return EntryDiffCellModel(
            id = id,
            eventType = EventType.UPDATE,
            title = resourceProvider.getString(R.string.entry_with_str, note.title),
            path = "$path / ${note.title}",
            fields = fields,
            icon = firstEvent.getIcon(),
            iconBackgroundTint = firstEvent.getIconTint(themeProvider),
            accentTextColor = firstEvent.getAccentTextColor(themeProvider),
            isCheckable = isCheckable,
            isChecked = isChecked,
            isExpanded = true
        )
    }

    private fun createLocalHeaderCell(time: String): DiffHeaderCellModel {
        return DiffHeaderCellModel(
            id = CellIds.LOCAL_CHANGES_HEADER,
            title = resourceProvider.getString(R.string.local_changes),
            description = time,
            isCheckable = false,
            isChecked = false
        )
    }

    private fun createRemoteHeaderCell(time: String): DiffHeaderCellModel {
        return DiffHeaderCellModel(
            id = CellIds.REMOTE_CHANGES_HEADER,
            title = resourceProvider.getString(R.string.remote_changes),
            description = time,
            isCheckable = false,
            isChecked = false
        )
    }

    private fun Timestamp.formatDateAndTime(): String {
        val date = Date(timeInMillis)

        val dateFormat = dateFormatProvider.getLongDateFormat()
        val timeFormat = dateFormatProvider.getTimeFormat()

        return dateFormat.format(date) + " " + timeFormat.format(date)
    }

    private fun List<Property>.toFields(
        event: DiffEvent<*>
    ): List<Field> {
        val properties = this

        return properties.mapIndexed { index, property ->
            val value = if (property.value.isNullOrEmpty()) {
                "''"
            } else {
                property.value
            }

            val shape = when {
                properties.size == 1 -> RoundedShape.ALL
                index == 0 -> RoundedShape.TOP
                index == properties.lastIndex -> RoundedShape.BOTTOM
                else -> RoundedShape.NONE
            }

            Field(
                eventType = event.toCellEventType(),
                name = property.name.orEmpty(),
                value = FieldValue.Value(value),
                backgroundShape = shape
            )
        }
    }

    private fun DiffEvent<*>.toCellEventType(): EventType =
        when (this) {
            is DiffEvent.Insert -> EventType.INSERT
            is DiffEvent.Delete -> EventType.DELETE
            is DiffEvent.Update -> EventType.UPDATE
        }

    private fun DiffEvent<*>.formatValuePrefix(): String =
        when (this) {
            is DiffEvent.Insert -> "+ "
            is DiffEvent.Delete -> "- "
            is DiffEvent.Update -> ""
        }

    private fun DiffEvent<*>.formatDescription(): String =
        when (this) {
            is DiffEvent.Insert -> resourceProvider.getString(R.string.added)
            is DiffEvent.Delete -> resourceProvider.getString(R.string.deleted)
            is DiffEvent.Update -> resourceProvider.getString(R.string.changed)
        }

    private fun DiffEvent<*>.getIcon(): ImageVector =
        when (this) {
            is DiffEvent.Insert -> Icons.Default.Add
            is DiffEvent.Delete -> Icons.Default.Remove
            is DiffEvent.Update -> Icons.Default.Edit
        }

    private fun DiffEvent<*>.getIconTint(themeProvider: ThemeProvider): Color {
        val theme = themeProvider.resolveTheme().toComposeTheme()

        return when (this) {
            is DiffEvent.Insert -> theme.colors.diffInsert
            is DiffEvent.Delete -> theme.colors.diffDelete
            is DiffEvent.Update -> theme.colors.diffUpdate
        }
    }

    private fun DiffEvent<*>.getAccentTextColor(themeProvider: ThemeProvider): Color {
        val theme = themeProvider.resolveTheme().toComposeTheme()

        return when (this) {
            is DiffEvent.Insert -> theme.colors.diffInsertText
            is DiffEvent.Delete -> theme.colors.diffDeleteText
            is DiffEvent.Update -> theme.colors.diffUpdateText
        }
    }

    object CellIds {
        val ERROR_PANEL = 100.toId()
        val LOCAL_CHANGES_HEADER = 101.toId()
        val REMOTE_CHANGES_HEADER = 102.toId()

        val SPACE = 200.toBaseId()
        val LOCAL_ITEMS = 1000.toBaseId()
        val REMOTE_ITEMS = 2000.toBaseId()
    }
}