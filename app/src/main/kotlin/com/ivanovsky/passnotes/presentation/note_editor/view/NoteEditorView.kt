package com.ivanovsky.passnotes.presentation.note_editor.view

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import com.ivanovsky.passnotes.presentation.note_editor.view.extended_text.ExtTextDataItem
import com.ivanovsky.passnotes.presentation.note_editor.view.extended_text.ExtTextItemView
import com.ivanovsky.passnotes.presentation.note_editor.view.secret.SecretDataItem
import com.ivanovsky.passnotes.presentation.note_editor.view.secret.SecretItemView
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
        when (item) {
            is TextDataItem -> addTextItem(item)
            is SecretDataItem -> addSecretItem(item)
            is ExtTextDataItem -> addExtendedTextItem(item)
        }
    }

    private fun addTextItem(item: TextDataItem) {
        val itemView = TextItemView(context)
        addView(itemView)
        itemView.setDataItem(item)

        itemViews.add(itemView)
    }

    private fun addSecretItem(item: SecretDataItem) {
        val itemView = SecretItemView(context)
        addView(itemView)
        itemView.setDataItem(item)

        itemViews.add(itemView)
    }

    private fun addExtendedTextItem(item: ExtTextDataItem) {
        val itemView = ExtTextItemView(context)
        addView(itemView)
        itemView.setDataItem(item)

        itemViews.add(itemView)
    }

    fun getItems(): List<BaseDataItem> {
        return itemViews.map { view -> view.getDataItem() }
    }

    fun isAllDataValid(): Boolean {
        return itemViews.all { view -> view.isDataValid() }
    }

    fun displayErrors() {
        for (view in itemViews) {
            view.displayError()
        }
    }
}