package com.ivanovsky.passnotes.presentation.note_editor.view

import android.view.View

interface BaseItemView<T : BaseDataItem> {

    fun getContentView(): View
    fun getDataItem(): T
    fun setDataItem(item: T)
}