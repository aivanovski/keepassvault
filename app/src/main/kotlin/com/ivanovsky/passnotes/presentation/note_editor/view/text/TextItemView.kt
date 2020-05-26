package com.ivanovsky.passnotes.presentation.note_editor.view.text

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import com.google.android.material.textfield.TextInputLayout
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.note_editor.view.BaseItemView

class TextItemView(
    context: Context
) : FrameLayout(context), BaseItemView<TextDataItem> {

    private val editText: EditText
    private val textInputLayout: TextInputLayout
    private lateinit var item: TextDataItem

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.note_editor_text_item_view, this)

        editText = view.findViewById(R.id.edit_text)
        textInputLayout = view.findViewById(R.id.text_input_layout)
    }

    override fun getContentView(): View {
        return View(context!!)
    }

    override fun getDataItem(): TextDataItem {
        return TextDataItem(
            item.id,
            item.name,
            editText.text.toString().trim()
        )
    }

    override fun setDataItem(item: TextDataItem) {
        this.item = item

        textInputLayout.hint = item.name
        editText.setText(item.value)
    }
}