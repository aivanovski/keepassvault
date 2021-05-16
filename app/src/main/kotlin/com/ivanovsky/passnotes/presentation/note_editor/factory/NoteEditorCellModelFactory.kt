package com.ivanovsky.passnotes.presentation.note_editor.factory

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.PropertyType
import com.ivanovsky.passnotes.data.entity.Template
import com.ivanovsky.passnotes.data.entity.TemplateFieldType
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.entity.PropertyMap
import com.ivanovsky.passnotes.domain.entity.PropertyFilter
import com.ivanovsky.passnotes.presentation.core_mvvm.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.core_mvvm.model.SpaceCellModel
import com.ivanovsky.passnotes.presentation.note_editor.NoteEditorViewModel.CellId
import com.ivanovsky.passnotes.presentation.note_editor.cells.model.ExtendedTextPropertyCellModel
import com.ivanovsky.passnotes.presentation.note_editor.cells.model.SecretPropertyCellModel
import com.ivanovsky.passnotes.presentation.note_editor.cells.model.TextPropertyCellModel
import com.ivanovsky.passnotes.presentation.note_editor.view.SecretInputType
import com.ivanovsky.passnotes.presentation.note_editor.view.TextInputLines.MULTIPLE_LINES
import com.ivanovsky.passnotes.presentation.note_editor.view.TextInputLines.SINGLE_LINE
import com.ivanovsky.passnotes.presentation.note_editor.view.TextInputType.*
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import com.ivanovsky.passnotes.util.toCleanString
import java.util.*

class NoteEditorCellModelFactory(
    private val resourceProvider: ResourceProvider
) {

    fun createModelsForNewNote(template: Template?): List<BaseCellModel> {
        return if (template == null) {
            listOf(
                createTitleCell(EMPTY),
                createUserNameCell(EMPTY),
                createPasswordCell(EMPTY),
                createUrlCell(EMPTY),
                createNotesCell(EMPTY)
            )
        } else {
            val models = mutableListOf<BaseCellModel>()

            models.add(createTitleCell(EMPTY))

            for (field in template.fields) {
                val isProtected = (field.type == TemplateFieldType.PROTECTED_INLINE)

                models.add(
                    ExtendedTextPropertyCellModel(
                        id = template.uid.toString() + "_" + field.title,
                        name = field.title,
                        value = EMPTY,
                        isProtected = isProtected,
                        isCollapsed = true,
                        inputType = TEXT
                    )
                )
            }

            models.add(createSpaceCell())

            models
        }
    }

    fun createModelsForNote(note: Note, template: Template?): List<BaseCellModel> {
        return when {
            isTemplateNote(note) -> {
                createModelsForTemplateNote(note)
            }
            template != null -> {
                createModelsForNoteWithTemplate(note, template)
            }
            else -> {
                createModelsForNote(note)
            }
        }
    }

    fun createCustomPropertyModels(): List<BaseCellModel> {
        val cellId = UUID.randomUUID().toCleanString()
        return listOf(
            ExtendedTextPropertyCellModel(
                id = cellId,
                name = EMPTY,
                value = EMPTY,
                isProtected = false,
                isCollapsed = false,
                inputType = TEXT
            ),
            createSpaceCell()
        )
    }

    private fun isTemplateNote(note: Note): Boolean {
        return PropertyFilter.filterTemplateIndicator(note.properties) != null
    }

    private fun createModelsForTemplateNote(note: Note): List<BaseCellModel> {
        val models = mutableListOf<BaseCellModel>()

        val properties = PropertyFilter.Builder()
            .visible()
            .excludeTitle()
            .notEmpty()
            .build()
            .apply(note.properties)

        models.add(createTitleCell(note.title))

        for (property in properties) {
            val cellId = note.uid.toString() + "_" + (property.name ?: EMPTY)

            models.add(
                ExtendedTextPropertyCellModel(
                    id = cellId,
                    name = property.name ?: EMPTY,
                    value = property.value ?: EMPTY,
                    isProtected = property.isProtected,
                    isCollapsed = true,
                    inputType = TEXT
                )
            )
        }

        models.add(createSpaceCell())

        return models
    }

    private fun createModelsForNoteWithTemplate(
        note: Note,
        template: Template
    ): List<BaseCellModel> {
        val models = mutableListOf<BaseCellModel>()

        models.add(createTitleCell(note.title))

        val propertyMap = PropertyMap.mapByName(note.properties)

        for (field in template.fields) {
            val property = propertyMap.get(field.title)

            val isProtected = property?.isProtected
                ?: (field.type == TemplateFieldType.PROTECTED_INLINE)

            val cellId = note.uid.toString() + "_" + field.title

            when (property?.type) {
                PropertyType.USER_NAME -> {
                    models.add(createUserNameCell(property.value ?: EMPTY))
                }
                PropertyType.PASSWORD -> {
                    models.add(createPasswordCell(property.value ?: EMPTY))
                }
                PropertyType.URL -> {
                    models.add(createUrlCell(property.value ?: EMPTY))
                }
                PropertyType.NOTES -> {
                    models.add(createNotesCell(property.value ?: EMPTY))
                }
                else -> {
                    models.add(
                        ExtendedTextPropertyCellModel(
                            id = cellId,
                            name = field.title,
                            value = property?.value ?: EMPTY,
                            isProtected = isProtected,
                            isCollapsed = true,
                            inputType = TEXT
                        )
                    )
                }
            }
        }

        val excludedNames = template.fields.map { it.title }
        val filter = PropertyFilter.Builder()
            .visible()
            .excludeByName(*excludedNames.toTypedArray())
            .excludeDefaultTypes() // TODO: some of default properties may contains information
            .sortedByType()
            .build()

        val otherProperties = filter.apply(note.properties)
        for (property in otherProperties) {
            // TODO: what if property.name will be null ?
            val cellId = note.uid.toString() + "_" + (property.name ?: EMPTY)

            models.add(
                ExtendedTextPropertyCellModel(
                    id = cellId,
                    name = property.name ?: EMPTY,
                    value = property.value ?: EMPTY,
                    isProtected = property.isProtected,
                    isCollapsed = true,
                    inputType = TEXT
                )
            )
        }

        models.add(createSpaceCell())

        return models
    }

    private fun createModelsForNote(note: Note): List<BaseCellModel> {
        val models = mutableListOf<BaseCellModel>()

        val visibleProperties = PropertyMap.mapByType(
            properties = PropertyFilter.Builder()
                .visible()
                .sortedByType()
                .build()
                .apply(note.properties)
        )

        val userName = visibleProperties.get(PropertyType.USER_NAME)?.value ?: EMPTY
        val url = visibleProperties.get(PropertyType.URL)?.value ?: EMPTY
        val notes = visibleProperties.get(PropertyType.NOTES)?.value ?: EMPTY
        val password = visibleProperties.get(PropertyType.PASSWORD)?.value ?: EMPTY

        val otherProperties = PropertyFilter.Builder()
            .visible()
            .notEmptyName()
            .excludeDefaultTypes()
            .build()
            .apply(note.properties)

        models.add(createTitleCell(note.title))
        models.add(createUserNameCell(userName))
        models.add(createPasswordCell(password))
        models.add(createUrlCell(url))
        models.add(createNotesCell(notes))

        for (property in otherProperties) {
            val cellId = note.uid.toString() + "_" + (property.name ?: EMPTY)
            models.add(
                ExtendedTextPropertyCellModel(
                    id = cellId,
                    name = property.name ?: EMPTY,
                    value = property.value ?: EMPTY,
                    isProtected = property.isProtected,
                    isCollapsed = true,
                    inputType = TEXT
                )
            )
        }

        models.add(createSpaceCell())

        return models
    }

    private fun createSpaceCell(): BaseCellModel {
        return SpaceCellModel()
    }

    private fun createTitleCell(title: String): TextPropertyCellModel {
        return TextPropertyCellModel(
            id = CellId.TITLE,
            name = resourceProvider.getString(R.string.title),
            value = title,
            textInputType = TEXT_CAP_SENTENCES,
            inputLines = SINGLE_LINE,
            isAllowEmpty = false,
            propertyType = PropertyType.TITLE,
            propertyName = PropertyType.TITLE.propertyName
        )
    }

    private fun createUserNameCell(userName: String): TextPropertyCellModel {
        return TextPropertyCellModel(
            id = CellId.USER_NAME,
            name = resourceProvider.getString(R.string.username),
            value = userName,
            textInputType = TEXT,
            inputLines = SINGLE_LINE,
            isAllowEmpty = true,
            propertyType = PropertyType.USER_NAME,
            propertyName = PropertyType.USER_NAME.propertyName
        )
    }

    private fun createUrlCell(url: String): TextPropertyCellModel {
        return TextPropertyCellModel(
            id = CellId.URL,
            name = resourceProvider.getString(R.string.url_cap),
            value = url,
            textInputType = URL,
            inputLines = MULTIPLE_LINES,
            isAllowEmpty = true,
            propertyType = PropertyType.URL,
            propertyName = PropertyType.URL.propertyName
        )
    }

    private fun createNotesCell(notes: String): TextPropertyCellModel {
        return TextPropertyCellModel(
            id = CellId.NOTES,
            name = resourceProvider.getString(R.string.notes),
            value = notes,
            textInputType = TEXT_CAP_SENTENCES,
            inputLines = MULTIPLE_LINES,
            isAllowEmpty = true,
            propertyType = PropertyType.NOTES,
            propertyName = PropertyType.NOTES.propertyName
        )
    }

    private fun createPasswordCell(password: String): SecretPropertyCellModel {
        return SecretPropertyCellModel(
            id = CellId.PASSWORD,
            name = resourceProvider.getString(R.string.password),
            confirmationName = resourceProvider.getString(R.string.confirm_password),
            value = password,
            inputType = SecretInputType.TEXT,
            propertyType = PropertyType.PASSWORD,
            propertyName = PropertyType.PASSWORD.propertyName
        )
    }
}