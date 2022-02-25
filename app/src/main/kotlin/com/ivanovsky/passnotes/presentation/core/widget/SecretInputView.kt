package com.ivanovsky.passnotes.presentation.core.widget

import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import com.ivanovsky.passnotes.databinding.WidgetSecretInputBinding
import com.ivanovsky.passnotes.presentation.note_editor.view.SecretInputType
import com.ivanovsky.passnotes.util.StringUtils.EMPTY

class SecretInputView(
    context: Context,
    attrs: AttributeSet
) : ConstraintLayout(context, attrs) {

    var isTextVisible: Boolean = false
        set(value) {
            setTextVisibilityToView(value)
            field = value
        }

    var inputType: SecretInputType = SecretInputType.TEXT
        set(value) {
            setInputTypeToView(value)
            field = value
        }

    var inputText: String
        get() = getTextFromView()
        set(value) {
            setTextToView(value, isNotifyListener = true)
        }

    var hint: String = EMPTY
        set(value) {
            setHintToView(value)
            field = value
        }

    var error: String? = null
        set(value) {
            setErrorToView(value)
            field = value
        }

    private val textInputWatcher: InputWatcher
    private val binding: WidgetSecretInputBinding
    private var onTextChanged: ((String) -> Unit)? = null

    init {
        val inflater = LayoutInflater.from(context)
        binding = WidgetSecretInputBinding.inflate(inflater, this, true)

        binding.visibilityButton.setOnClickListener {
            toggleTextVisibility()
        }

        textInputWatcher = InputWatcher()
        binding.textInput.addTextChangedListener(textInputWatcher)

        setTextVisibilityToView(isTextVisible)
        setInputTypeToView(inputType)
    }

    private fun setTextVisibilityToView(isVisible: Boolean) {
        binding.textInput.transformationMethod = if (isVisible) {
            HideReturnsTransformationMethod.getInstance()
        } else {
            PasswordTransformationMethod.getInstance()
        }
    }

    private fun setInputTypeToView(inputType: SecretInputType) {
        binding.textInput.setRawInputType(
            when (inputType) {
                SecretInputType.TEXT -> InputType.TYPE_CLASS_TEXT + InputType.TYPE_TEXT_VARIATION_PASSWORD
                SecretInputType.DIGITS -> InputType.TYPE_CLASS_NUMBER + InputType.TYPE_NUMBER_VARIATION_PASSWORD
            }
        )
    }

    private fun setTextToView(text: String, isNotifyListener: Boolean) {
        if (getTextFromView() == text) {
            return
        }

        if (!isNotifyListener) {
            textInputWatcher.isEnabled = false
        }

        binding.textInput.setText(text)

        if (!isNotifyListener) {
            textInputWatcher.isEnabled = true
        }
    }

    private fun setHintToView(hint: String) {
        binding.textInputLayout.hint = hint
    }

    private fun setErrorToView(error: String?) {
        binding.textInputLayout.error = error
    }

    private fun getTextFromView(): String = binding.textInput.text.toString()

    private fun toggleTextVisibility() {
        isTextVisible = !isTextVisible
    }

    private fun onTextChanged(text: String) {
        onTextChanged?.invoke(text)
    }

    private inner class InputWatcher : TextWatcher {

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
        fun getText(view: SecretInputView): String {
            return view.inputText
        }

        @JvmStatic
        @BindingAdapter("text")
        fun setText(view: SecretInputView, text: String) {
            view.setTextToView(text, isNotifyListener = false)
        }

        @JvmStatic
        @BindingAdapter("textAttrChanged")
        fun setListener(view: SecretInputView, textAttrChanged: InverseBindingListener?) {
            if (textAttrChanged == null) {
                view.onTextChanged = null
                return
            }

            view.onTextChanged = { textAttrChanged.onChange() }
        }
    }
}