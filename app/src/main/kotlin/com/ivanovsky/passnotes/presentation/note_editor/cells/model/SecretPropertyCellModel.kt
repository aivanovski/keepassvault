package com.ivanovsky.passnotes.presentation.note_editor.cells.model

import com.ivanovsky.passnotes.data.entity.PropertyType
import com.ivanovsky.passnotes.presentation.core_mvvm.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.note_editor.view.SecretInputType

data class SecretPropertyCellModel(
    override val id: String,
    val name: String,
    val confirmationName: String,
    val value: String,
    val inputType: SecretInputType,
    val propertyType: PropertyType?,
    val propertyName: String?
) : BaseCellModel()