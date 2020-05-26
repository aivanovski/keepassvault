package com.ivanovsky.passnotes.presentation.note_editor.view

abstract class BaseDataItem(open val id: Int, open val value: String) {
    abstract val isEmpty: Boolean
}
