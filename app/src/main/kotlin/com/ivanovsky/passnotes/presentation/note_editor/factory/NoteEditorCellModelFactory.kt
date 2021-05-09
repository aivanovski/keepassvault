package com.ivanovsky.passnotes.presentation.note_editor.factory

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.PropertyType
import com.ivanovsky.passnotes.data.entity.Template
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.presentation.core_mvvm.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.note_editor.NoteEditorViewModel.CellId
import com.ivanovsky.passnotes.presentation.note_editor.cells.model.SecretPropertyCellModel
import com.ivanovsky.passnotes.presentation.note_editor.cells.model.TextPropertyCellModel
import com.ivanovsky.passnotes.presentation.note_editor.view.secret.SecretInputType
import com.ivanovsky.passnotes.presentation.note_editor.view.text.TextInputLines.MULTIPLE_LINES
import com.ivanovsky.passnotes.presentation.note_editor.view.text.TextInputLines.SINGLE_LINE
import com.ivanovsky.passnotes.presentation.note_editor.view.text.TextInputType.*

class NoteEditorCellModelFactory(
    private val resourceProvider: ResourceProvider
) {

    fun createModelsForNewNote(template: Template?): List<BaseCellModel> {
        return if (template == null) {
            listOf(
                createTitleCell(""),
                createUserNameCell(""),
                createPasswordCell(""),
                createUrlCell(""),
                createNotesCell("")
            )
        } else {
            listOf()
        }
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