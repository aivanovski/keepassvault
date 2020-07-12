package com.ivanovsky.passnotes.presentation.note_editor.view

abstract class BaseDataItem(
    open val id: Int,
    open val value: String
) {
    abstract val isEmpty: Boolean

    companion object {
        const val ITEM_ID_TITLE = 1
        const val ITEM_ID_USER_NAME = 2
        const val ITEM_ID_URL = 3
        const val ITEM_ID_EMAIL = 4
        const val ITEM_ID_NOTES = 5
        const val ITEM_ID_PASSWORD = 6
        const val ITEM_ID_CUSTOM = 7
    }
}
