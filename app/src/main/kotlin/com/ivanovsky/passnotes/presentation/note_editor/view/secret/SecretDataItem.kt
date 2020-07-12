package com.ivanovsky.passnotes.presentation.note_editor.view.secret

import com.ivanovsky.passnotes.presentation.note_editor.view.BaseDataItem

data class SecretDataItem(
    override val id: Int,
    val name: String,
    override val value: String,
    val secretInputType: SecretInputType
) : BaseDataItem(id, value) {

    override val isEmpty: Boolean = value.isEmpty()
}