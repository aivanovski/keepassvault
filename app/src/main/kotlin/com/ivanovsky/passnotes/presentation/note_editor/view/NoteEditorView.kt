package com.ivanovsky.passnotes.presentation.note_editor.view

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import com.ivanovsky.passnotes.presentation.note_editor.view.text.TextDataItem
import com.ivanovsky.passnotes.presentation.note_editor.view.text.TextItemView

class NoteEditorView(
    context: Context,
    attrs: AttributeSet
) : LinearLayout(context, attrs) {

    private val itemViews = mutableListOf<BaseItemView<*>>()

    init {
        orientation = VERTICAL
    }

    fun addItem(item: BaseDataItem) {
        if (item is TextDataItem) {
            addTextItem(item)
        }
    }

    private fun addTextItem(item: TextDataItem) {
        val itemView = TextItemView(context)
        addView(itemView)
        itemView.setDataItem(item)
    }

    fun getItems(): List<BaseDataItem> {
        return itemViews.map { view -> view.getDataItem() }
    }
}