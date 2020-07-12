package com.ivanovsky.passnotes.presentation.note_editor.view.extended_text

import com.ivanovsky.passnotes.presentation.note_editor.view.BaseDataItem
import com.ivanovsky.passnotes.presentation.note_editor.view.text.TextInputType

data class ExtTextDataItem(
    override val id: Int,
    val name: String,
    override val value: String,
    val isProtected: Boolean,
    val isCollapsed: Boolean,
    val textInputType: TextInputType
) : BaseDataItem(id, value) {

    override val isEmpty: Boolean = value.isEmpty() && name.isEmpty()
}