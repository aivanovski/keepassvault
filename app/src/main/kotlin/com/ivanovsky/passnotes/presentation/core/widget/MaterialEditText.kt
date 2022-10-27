package com.ivanovsky.passnotes.presentation.core.widget

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.databinding.WidgetMaterialEditTextBinding
import com.ivanovsky.passnotes.util.StringUtils

class MaterialEditText(
    context: Context,
    attrs: AttributeSet
) : ConstraintLayout(context, attrs) {

    var text: String
        get() = getTextInternal()
        set(value) {
            setTextInternal(value, isNotifyListener = true)
        }

    var hint: String? = null
        set(value) {
            setHintInternal(value ?: StringUtils.EMPTY)
            field = value
        }

    var error: String? = null
        set(value) {
            setErrorInternal(value)
            field = value
        }

    var isEyeButtonVisible: Boolean = false
        set(value) {
            setEyeButtonVisibility(value)
            field = value
        }

    private val textWatcher: CustomTextWatcher
    private val binding: WidgetMaterialEditTextBinding
    private var databindingTextChangedListener: ((String) -> Unit)? = null

    init {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.widget_material_edit_text, this, true)
        binding = WidgetMaterialEditTextBinding.bind(view)

        textWatcher = CustomTextWatcher()
        binding.textInput.addTextChangedListener(textWatcher)

        setEyeButtonVisibility(isEyeButtonVisible)
    }

    private fun setHintInternal(hint: String) {
        binding.textInputLayout.hint = hint
    }

    private fun setErrorInternal(error: String?) {
        binding.textInputLayout.error = error
    }

    private fun getTextInternal(): String = binding.textInput.text.toString()

    private fun setTextInternal(text: String, isNotifyListener: Boolean) {
        if (getTextInternal() == text) {
            return
        }

        if (!isNotifyListener) {
            textWatcher.isEnabled = false
        }

        binding.textInput.setText(text)

        if (!isNotifyListener) {
            textWatcher.isEnabled = true
        }
    }

    private fun onTextChanged(text: String) {
        databindingTextChangedListener?.invoke(text)
    }

    private fun setEyeButtonVisibility(isVisible: Boolean) {
        binding.eyeButton.isVisible = isVisible

        if (isVisible) {
            val iconPadding =
                context.resources.getDimension(R.dimen.borderless_icon_button_size).toInt()

            binding.textInputLayout.setPadding(0, 0, iconPadding, 0)
        } else {
            binding.textInputLayout.setPadding(0, 0, 0, 0)
        }
    }

    private inner class CustomTextWatcher : TextWatcher {

        var isEnabled = true

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(s: Editable?) {
            if (isEnabled) {
                onTextChanged(s.toString())
            }
        }
    }

    companion object {

        @JvmStatic
        @InverseBindingAdapter(attribute = "text")
        fun getText(view: MaterialEditText): String {
            return view.getTextInternal()
        }

        @JvmStatic
        @BindingAdapter("text")
        fun setText(view: MaterialEditText, text: String) {
            view.setTextInternal(text, isNotifyListener = false)
        }

        @JvmStatic
        @BindingAdapter("textAttrChanged")
        fun setListener(view: MaterialEditText, textAttrChanged: InverseBindingListener?) {
            if (textAttrChanged == null) {
                view.databindingTextChangedListener = null
                return
            }

            view.databindingTextChangedListener = { textAttrChanged.onChange() }
        }
    }
}