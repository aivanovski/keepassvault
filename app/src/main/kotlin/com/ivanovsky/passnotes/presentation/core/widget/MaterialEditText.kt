package com.ivanovsky.passnotes.presentation.core.widget

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.databinding.WidgetMaterialEditTextBinding
import com.ivanovsky.passnotes.presentation.core.widget.entity.ImeOptions
import com.ivanovsky.passnotes.presentation.core.widget.entity.OnEditorActionListener
import com.ivanovsky.passnotes.presentation.core.widget.entity.TextInputLines
import com.ivanovsky.passnotes.util.StringUtils

class MaterialEditText(
    context: Context,
    attrs: AttributeSet
) : ConstraintLayout(context, attrs) {

    var isTextVisible: Boolean = true
        set(value) {
            setTextVisibleInternal(value)
            field = value
        }

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
            setEyeButtonVisibleInternal(value)
            field = value
        }

    var inputLines: TextInputLines? = TextInputLines.SINGLE_LINE
        set(value) {
            setInputLinesInternal(value)
            field = value
        }

    var imeOptions: ImeOptions? = null
        set(value) {
            setImeOptionsInternal(value)
            field = value
        }

    private val textWatcher: CustomTextWatcher
    private val binding: WidgetMaterialEditTextBinding
    private var databindingTextChangedListener: ((String) -> Unit)? = null
    private var onEditorAction: OnEditorActionListener? = null

    init {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.widget_material_edit_text, this, true)
        binding = WidgetMaterialEditTextBinding.bind(view)

        readAttributes(attrs)

        binding.eyeButton.setOnClickListener {
            toggleTextVisibility()
        }

        binding.textInput.setOnEditorActionListener { _, actionId, _ ->
            handleEditorAction(actionId)
        }

        textWatcher = CustomTextWatcher()
        binding.textInput.addTextChangedListener(textWatcher)

        setInputLinesInternal(inputLines)
        setEyeButtonVisibleInternal(isEyeButtonVisible)
        setTextVisibleInternal(isTextVisible)
    }

    private fun readAttributes(attrs: AttributeSet) {
        val params = context.obtainStyledAttributes(attrs, R.styleable.MaterialEditText)

        val hint = params.getString(R.styleable.MaterialEditText_hint)
        if (hint != null) {
            this.hint = hint
        }

        isEyeButtonVisible = params.getBoolean(
            R.styleable.MaterialEditText_isEyeButtonVisible,
            false
        )

        params.recycle()
    }

    fun getEditText(): EditText = binding.textInput

    private fun handleEditorAction(actionId: Int): Boolean {
        return if (onEditorAction != null) {
            onEditorAction?.onEditorAction(actionId)
            true
        } else {
            false
        }
    }

    private fun setHintInternal(hint: String) {
        binding.textInputLayout.hint = hint
    }

    private fun setErrorInternal(error: String?) {
        binding.textInputLayout.error = error
    }

    private fun toggleTextVisibility() {
        isTextVisible = !isTextVisible
    }

    private fun setTextVisibleInternal(isVisible: Boolean) {
        binding.textInput.transformationMethod = if (isVisible) {
            HideReturnsTransformationMethod.getInstance()
        } else {
            PasswordTransformationMethod.getInstance()
        }
        binding.eyeButton.setImageResource(
            if (isVisible) {
                R.drawable.ic_visibility_on_24dp
            } else {
                R.drawable.ic_visibility_off_24dp
            }
        )
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

    private fun setEyeButtonVisibleInternal(isVisible: Boolean) {
        if (isVisible && isTextVisible) {
            isTextVisible = false
        }
        binding.eyeButton.isVisible = isVisible

        val textInput = binding.textInput

        val paddingEnd = if (isVisible) {
            context.resources.getDimension(R.dimen.borderless_icon_button_size).toInt()
        } else {
            context.resources.getDimension(R.dimen.half_margin).toInt()
        }

        textInput.setPadding(
            textInput.paddingStart,
            textInput.paddingTop,
            paddingEnd,
            textInput.paddingBottom
        )
    }

    private fun setInputLinesInternal(inputLines: TextInputLines?) {
        when (inputLines) {
            TextInputLines.SINGLE_LINE -> {
                binding.textInput.minLines = 1
                binding.textInput.maxLines = 1
                binding.textInput.isSingleLine = true
            }
            TextInputLines.MULTIPLE_LINES -> {
                binding.textInput.isSingleLine = false
                binding.textInput.minLines = 1
                binding.textInput.maxLines = 5
            }
            else -> {}
        }
        setTextVisibleInternal(isTextVisible)
        setImeOptionsInternal(imeOptions)
    }

    private fun setImeOptionsInternal(imeOptions: ImeOptions?) {
        when (imeOptions) {
            ImeOptions.ACTION_DONE -> {
                binding.textInput.imeOptions = EditorInfo.IME_ACTION_DONE
            }
            ImeOptions.ACTION_NEXT -> {
                binding.textInput.imeOptions = EditorInfo.IME_ACTION_NEXT
            }
            else -> {}
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

        @JvmStatic
        @BindingAdapter("onEditorAction")
        fun setOnEditorActionClickListener(
            view: MaterialEditText,
            listener: OnEditorActionListener?
        ) {
            view.onEditorAction = listener
        }
    }
}