package com.ivanovsky.passnotes.presentation.note_editor.view.text

import com.ivanovsky.passnotes.presentation.note_editor.view.BaseDataItem

data class TextDataItem(
    override val id: Int,
    val name: String,
    override val value: String
) : BaseDataItem(id, value) {

    override val isEmpty: Boolean = value.isEmpty()
}