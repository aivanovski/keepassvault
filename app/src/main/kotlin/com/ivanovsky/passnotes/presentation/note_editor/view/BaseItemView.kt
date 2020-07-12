package com.ivanovsky.passnotes.presentation.note_editor.view

interface BaseItemView<T : BaseDataItem> {

    fun getDataItem(): T
    fun setDataItem(item: T)
    fun isDataValid(): Boolean
    fun displayError()
}