package com.ivanovsky.passnotes.presentation.note_editor.factory

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.data.entity.PropertyType
import com.ivanovsky.passnotes.data.entity.Template
import com.ivanovsky.passnotes.data.entity.TemplateFieldType
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.entity.PropertyFilter
import com.ivanovsky.passnotes.domain.entity.PropertyMap
import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.core.model.SpaceCellModel
import com.ivanovsky.passnotes.presentation.note_editor.NoteEditorViewModel.CellId
import com.ivanovsky.passnotes.presentation.note_editor.cells.model.ExtendedTextPropertyCellModel
import com.ivanovsky.passnotes.presentation.note_editor.cells.model.SecretPropertyCellModel
import com.ivanovsky.passnotes.presentation.note_editor.cells.model.TextPropertyCellModel
import com.ivanovsky.passnotes.presentation.note_editor.view.SecretInputType
import com.ivanovsky.passnotes.presentation.note_editor.view.TextInputLines.MULTIPLE_LINES
import com.ivanovsky.passnotes.presentation.note_editor.view.TextInputLines.SINGLE_LINE
import com.ivanovsky.passnotes.presentation.note_editor.view.TextInputType.TEXT
import com.ivanovsky.passnotes.presentation.note_editor.view.TextInputType.TEXT_CAP_SENTENCES
import com.ivanovsky.passnotes.presentation.note_editor.view.TextInputType.URL
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import com.ivanovsky.passnotes.util.toCleanString
import java.util.UUID

class NoteEditorCellModelFactory(
    private val resourceProvider: ResourceProvider
) {

    fun createDefaultModels(): List<BaseCellModel> {
        return listOf(
            createTitleCell(EMPTY),
            createUserNameCell(EMPTY),
            createPasswordCell(EMPTY),
            createUrlCell(EMPTY),
            createNotesCell(EMPTY)
        )
    }

    fun createModelsFromTemplate(template: Template): List<BaseCellModel> {
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

        return models
    }

    fun createModelsFromProperties(properties: List<Property>): List<BaseCellModel> {
        return createModels(
            uid = null,
            title = EMPTY,
            properties = properties
        )
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
                createModels(note.uid, note.title, note.properties)
            }
        }
    }

    fun createCustomPropertyModels(type: PropertyType?): List<BaseCellModel> {
        val cellModel = when (type) {
            PropertyType.TITLE,
            PropertyType.PASSWORD,
            PropertyType.USER_NAME,
            PropertyType.URL,
            PropertyType.NOTES -> createCellByPropertyType(type, EMPTY)
            else -> ExtendedTextPropertyCellModel(
                id = UUID.randomUUID().toCleanString(),
                name = EMPTY,
                value = EMPTY,
                isProtected = false,
                isCollapsed = false,
                inputType = TEXT
            )
        }
        return listOf(cellModel, createSpaceCell())
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
                PropertyType.USER_NAME,
                PropertyType.PASSWORD,
                PropertyType.URL,
                PropertyType.NOTES -> models.add(
                    createCellByPropertyType(property.type, property.value ?: EMPTY)
                )
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
            .excludeByName(PropertyType.TITLE.propertyName)
            .notEmpty()
            .sortedByType()
            .build()

        val otherProperties = filter.apply(note.properties)
        for (property in otherProperties) {
            when (property.type) {
                PropertyType.USER_NAME,
                PropertyType.PASSWORD,
                PropertyType.URL,
                PropertyType.NOTES -> models.add(
                    createCellByPropertyType(property.type, property.value ?: EMPTY)
                )
                else -> {
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
            }
        }

        models.add(createSpaceCell())

        return models
    }

    private fun createModels(uid: UUID?, title: String, properties: List<Property>): List<BaseCellModel> {
        val models = mutableListOf<BaseCellModel>()

        val visibleProperties = PropertyMap.mapByType(
            properties = PropertyFilter.Builder()
                .visible()
                .sortedByType()
                .build()
                .apply(properties)
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
            .apply(properties)

        models.add(createTitleCell(title))
        models.add(createUserNameCell(userName))
        models.add(createPasswordCell(password))
        models.add(createUrlCell(url))
        models.add(createNotesCell(notes))

        for (property in otherProperties) {
            val cellId = if (uid != null) {
                uid.toString() + "_" + (property.name ?: EMPTY)
            } else {
                generateCellIdForProperty(property)
            }
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

    private fun generateCellIdForProperty(property: Property): String {
        return UUID.randomUUID().toString() + "_" + (property.name ?: EMPTY)
    }

    private fun createCellByPropertyType(type: PropertyType, value: String): BaseCellModel {
        return when (type) {
            PropertyType.TITLE -> createTitleCell(value)
            PropertyType.PASSWORD -> createPasswordCell(value)
            PropertyType.USER_NAME -> createUserNameCell(value)
            PropertyType.URL -> createUrlCell(value)
            PropertyType.NOTES -> createNotesCell(value)
        }
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