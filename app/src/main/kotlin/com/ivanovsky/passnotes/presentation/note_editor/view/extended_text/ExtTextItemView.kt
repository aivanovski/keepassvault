package com.ivanovsky.passnotes.presentation.note_editor.view.extended_text

import android.content.Context
import android.text.InputType
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputLayout
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.note_editor.view.BaseItemView
import com.ivanovsky.passnotes.presentation.note_editor.view.text.TextInputType
import com.ivanovsky.passnotes.presentation.note_editor.view.text.TextInputType.*
import com.ivanovsky.passnotes.util.setVisible

class ExtTextItemView(
    context: Context
) : ConstraintLayout(context), BaseItemView<ExtTextDataItem> {

    private val primaryEditText: EditText
    private val secondaryEditText: EditText
    private val primaryTextLayout: TextInputLayout
    private val secondaryTextLayout: TextInputLayout
    private val expandButton: ImageButton
    private val protectionSwitch: SwitchMaterial
    private val removeButton: ImageButton
    private var isCollapsed = true
    private lateinit var item: ExtTextDataItem

    init {
        val view =
            LayoutInflater.from(context).inflate(R.layout.note_editor_ext_text_item_view, this)

        primaryEditText = view.findViewById(R.id.primaryText)
        secondaryEditText = view.findViewById(R.id.secondaryText)
        primaryTextLayout = view.findViewById(R.id.primary_text_layout)
        secondaryTextLayout = view.findViewById(R.id.secondary_text_layout)
        expandButton = view.findViewById(R.id.expand_button)
        protectionSwitch = view.findViewById(R.id.protection_switch)
        removeButton = view.findViewById(R.id.remove_button)

        expandButton.setOnClickListener { onExpandButtonClicked() }
    }

    private fun onExpandButtonClicked() {
        if (!isCollapsed && !isAbleToCollapse()) {
            if (getPrimaryText().isEmpty()) {
                primaryEditText.error = context.getString(R.string.empty_field_name_message)
            }
            return
        }

        val (fieldName, fieldValue) = getNameAndValue()

        isCollapsed = isCollapsed.not()

        setVisibilityStateToViews(isCollapsed, protectionSwitch.isChecked)
        setTextToViews(fieldName, fieldValue)
        applyEditTextInputType(item.textInputType)
    }

    private fun isAbleToCollapse(): Boolean {
        val (fieldName, _) = getNameAndValue()
        return fieldName.isNotEmpty() || isInputFieldsNotEmpty()
    }

    private fun getNameAndValue(): Pair<String, String> {
        return if (isCollapsed) {
            Pair(getPrimaryHint(), getPrimaryText())
        } else {
            Pair(getPrimaryText(), getSecondaryText())
        }
    }

    override fun getDataItem(): ExtTextDataItem {
        val (fieldName, fieldValue) = getNameAndValue()
        return item.copy(
            name = fieldName,
            value = fieldValue,
            isCollapsed = isCollapsed,
            isProtected = protectionSwitch.isChecked
        )
    }

    override fun setDataItem(item: ExtTextDataItem) {
        this.item = item

        isCollapsed = item.isCollapsed

        setVisibilityStateToViews(item.isCollapsed, item.isProtected)
        setTextToViews(item.name, item.value)
        applyEditTextInputType(item.textInputType)

        protectionSwitch.isChecked = item.isProtected
    }

    override fun isDataValid(): Boolean {
        return if (isCollapsed) {
            getPrimaryHint().isNotEmpty()
        } else {
            isInputFieldsEmpty() || isInputFieldsNotEmpty()
        }
    }

    override fun displayError() {
        val (fieldName, fieldValue) = getNameAndValue()

        if (isCollapsed) {
            if (fieldName.isEmpty() && fieldValue.isNotEmpty()) {
                primaryEditText.error = context.getString(R.string.empty_field_name_message)
            }
        } else {
            if (fieldName.isEmpty() && fieldValue.isNotEmpty()) {
                primaryEditText.error = context.getString(R.string.empty_field_name_message)
            }
        }
    }

    private fun setVisibilityStateToViews(isCollapsed: Boolean, isProtected: Boolean) {
        if (isCollapsed) {
            expandButton.setImageResource(R.drawable.ic_expand_more_grey_600_24dp)

            if (isProtected) {
                primaryEditText.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    R.drawable.ic_lock_grey_600_24dp,
                    0
                )
            } else {
                primaryEditText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            }

            protectionSwitch.setVisible(false)
            removeButton.setVisible(false)
            secondaryTextLayout.setVisible(false)
        } else {
            primaryEditText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)

            primaryEditText.setCompoundDrawables(null, null, null, null)
            expandButton.setImageResource(R.drawable.ic_expand_less_grey_600_24dp)

            protectionSwitch.setVisible(true)
            removeButton.setVisible(true)
            secondaryTextLayout.setVisible(true)
        }
    }

    private fun setTextToViews(name: String, value: String) {
        if (isCollapsed) {
            primaryTextLayout.hint = name
            primaryEditText.setText(value)
        } else {
            primaryEditText.setText(name)
            secondaryEditText.setText(value)

            primaryTextLayout.hint = context.getString(R.string.field_name)
            secondaryTextLayout.hint = context.getString(R.string.field_value)
        }
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
        primaryEditText.setRawInputType(inputType)
        secondaryEditText.setRawInputType(inputType)
    }

    private fun getPrimaryText(): String {
        return primaryEditText.text.toString().trim()
    }

    private fun getSecondaryText(): String {
        return secondaryEditText.text.toString().trim()
    }

    private fun getPrimaryHint(): String {
        return primaryTextLayout.hint.toString().trim()
    }

    private fun isInputFieldsEmpty() = getPrimaryText().isEmpty() && getSecondaryText().isEmpty()

    private fun isInputFieldsNotEmpty() =
        getPrimaryText().isNotEmpty() && getSecondaryText().isNotEmpty()
}