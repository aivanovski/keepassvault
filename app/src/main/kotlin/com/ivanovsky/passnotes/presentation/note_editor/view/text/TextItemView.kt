package com.ivanovsky.passnotes.presentation.note_editor.view.text

import android.content.Context
import android.text.InputType
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.FrameLayout
import com.google.android.material.textfield.TextInputLayout
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.note_editor.view.BaseItemView
import com.ivanovsky.passnotes.presentation.note_editor.view.text.InputLines.MULTIPLE_LINES
import com.ivanovsky.passnotes.presentation.note_editor.view.text.InputLines.SINGLE_LINE
import com.ivanovsky.passnotes.presentation.note_editor.view.text.TextInputType.*

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

    override fun getDataItem(): TextDataItem {
        return item.copy(
            value = editText.text.toString().trim()
        )
    }

    override fun setDataItem(item: TextDataItem) {
        this.item = item

        textInputLayout.hint = item.name
        editText.setText(item.value)

        applyEditTextInputType(item.textInputType)
        applyInputLines(item.inputLines)
    }

    private fun applyEditTextInputType(textInputType: TextInputType) {
        val inputType = when (textInputType) {
            URL -> InputType.TYPE_CLASS_TEXT + InputType.TYPE_TEXT_VARIATION_URI
            TEXT -> InputType.TYPE_CLASS_TEXT
            EMAIL -> InputType.TYPE_CLASS_TEXT + InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
            TEXT_CAP_SENTENCES -> {
                InputType.TYPE_CLASS_TEXT + InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            }
        }
        editText.setRawInputType(inputType)
    }

    private fun applyInputLines(inputLines: InputLines) {
        when (inputLines) {
            SINGLE_LINE -> {
                editText.minLines = 1
                editText.maxLines = 1
            }
            MULTIPLE_LINES -> {
                editText.minLines = 1
                editText.maxLines = 5
            }
        }
    }

    override fun isDataValid(): Boolean {
        return !item.isShouldNotBeEmpty || getText().isNotEmpty()
    }

    override fun displayError() {
        if (item.isShouldNotBeEmpty && getText().isEmpty()) {
            editText.error = context.getString(R.string.should_not_be_empty)
        }
    }

    private fun getText(): String {
        return editText.text.toString().trim()
    }
}