package com.ivanovsky.passnotes.presentation.core.widget

import android.app.Activity
import android.content.Context
import android.os.Parcelable
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.databinding.WidgetMaterialEditTextBinding
import com.ivanovsky.passnotes.presentation.core.binding.OnTextChangeListener
import com.ivanovsky.passnotes.presentation.core.widget.entity.ImeOptions
import com.ivanovsky.passnotes.presentation.core.widget.entity.OnEditorActionListener
import com.ivanovsky.passnotes.presentation.core.widget.entity.TextInputLines
import com.ivanovsky.passnotes.presentation.core.widget.entity.TextInputType
import com.ivanovsky.passnotes.util.InputMethodUtils
import com.ivanovsky.passnotes.util.StringUtils.EMPTY

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
            setHintInternal(value ?: EMPTY)
            field = value
        }

    var error: String? = null
        set(value) {
            setErrorInternal(value)
            field = value
        }

    var isActionButtonVisible: Boolean = false
        set(value) {
            field = value
            setActionButtonVisibleInternal(determineIsActionButtonVisibleInternal())
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

    var actionButton: ActionButton = ActionButton.EYE
        set(value) {
            setActionButtonInternal(value, isResetTextVisibility = true)
            field = value
        }

    var inputType: TextInputType = TextInputType.TEXT
        set(value) {
            setInputTypeInternal(value)
            field = value
        }

    var maxLength: Int = Int.MAX_VALUE
        set(value) {
            setMaxLengthInternal(value)
            field = value
        }

    var onEditorAction: OnEditorActionListener? = null
    private val textWatcher: CustomTextWatcher
    private val binding: WidgetMaterialEditTextBinding
    private var twoWayBindingTextChangedListener: ((String) -> Unit)? = null
    private var onDoneAction: (() -> Unit)? = null

    init {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.widget_material_edit_text, this, true)
        binding = WidgetMaterialEditTextBinding.bind(view)

        readAttributes(attrs)

        if (minWidth != -1) {
            binding.textInput.minWidth = minWidth
        }
        binding.textInput.isEnabled = isEnabled

        binding.editTextActionButton.setOnClickListener {
            handleActionButtonClicked()
        }

        binding.textInput.setOnEditorActionListener { _, actionId, _ ->
            handleEditorAction(actionId)
        }

        textWatcher = CustomTextWatcher()
        binding.textInput.addTextChangedListener(textWatcher)

        setActionButtonInternal(actionButton, isResetTextVisibility = false)
        setInputLinesInternal(inputLines)
        setActionButtonVisibleInternal(determineIsActionButtonVisibleInternal())
        setTextVisibleInternal(isTextVisible)
        setInputTypeInternal(inputType)
    }

    private fun readAttributes(attrs: AttributeSet) {
        val params = context.obtainStyledAttributes(attrs, R.styleable.MaterialEditText)

        val text = params.getString(R.styleable.MaterialEditText_text)
        if (text != null) {
            this.text = text
        }

        val hint = params.getString(R.styleable.MaterialEditText_hint)
        if (hint != null) {
            this.hint = hint
        }

        val isEyeButtonEnabled = params.getBoolean(
            R.styleable.MaterialEditText_isEyeButtonEnabled,
            false
        )
        val isClearButtonEnabled = params.getBoolean(
            R.styleable.MaterialEditText_isClearButtonEnabled,
            false
        )

        isActionButtonVisible = (isEyeButtonEnabled || isClearButtonEnabled)
        when {
            isEyeButtonEnabled && isClearButtonEnabled -> {
                throw IllegalStateException()
            }
            isEyeButtonEnabled -> {
                actionButton = ActionButton.EYE
                isTextVisible = false
            }
            isClearButtonEnabled -> {
                actionButton = ActionButton.CLEAR
            }
        }

        params.recycle()
    }

    override fun setEnabled(isEnabled: Boolean) {
        super.setEnabled(isEnabled)
        binding.textInput.isEnabled = isEnabled
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        // State will be restored from ViewModel
        super.onRestoreInstanceState(null)
    }

    fun getEditText(): EditText = binding.textInput

    fun requestSoftInput() {
        binding.textInput.requestFocus()
        InputMethodUtils.showSoftInput(context, binding.textInput)
    }

    fun hideSoftInput() {
        InputMethodUtils.hideSoftInput(context as Activity)
    }

    private fun handleEditorAction(actionId: Int): Boolean {
        return if (onEditorAction != null || onDoneAction != null) {
            if (actionId == EditorInfo.IME_ACTION_DONE && onDoneAction != null) {
                onDoneAction?.invoke()
            } else {
                onEditorAction?.onEditorAction(actionId)
            }
            true
        } else {
            false
        }
    }

    private fun handleActionButtonClicked() {
        when (actionButton) {
            ActionButton.EYE -> toggleTextVisibility()
            ActionButton.CLEAR -> clearText()
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

    private fun clearText() {
        if (text.isNotEmpty()) {
            text = EMPTY
        }
    }

    private fun setTextVisibleInternal(isVisible: Boolean) {
        binding.textInput.transformationMethod = if (isVisible) {
            HideReturnsTransformationMethod.getInstance()
        } else {
            PasswordTransformationMethod.getInstance()
        }

        if (actionButton == ActionButton.EYE) {
            binding.editTextActionButton.setImageResource(getEyeIcon(isVisible))
        }
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
        twoWayBindingTextChangedListener?.invoke(text)
        setActionButtonVisibleInternal(determineIsActionButtonVisibleInternal())
    }

    private fun setInputTypeInternal(inputType: TextInputType) {
        val flags = when (inputType) {
            TextInputType.TEXT -> {
                if (isTextVisible) {
                    InputType.TYPE_CLASS_TEXT
                } else {
                    InputType.TYPE_CLASS_TEXT + InputType.TYPE_TEXT_VARIATION_PASSWORD
                }
            }
            TextInputType.DIGITS -> {
                if (isTextVisible) {
                    InputType.TYPE_CLASS_NUMBER
                } else {
                    InputType.TYPE_CLASS_NUMBER + InputType.TYPE_NUMBER_VARIATION_PASSWORD
                }
            }
            TextInputType.TEXT_CAP_SENTENCES -> {
                InputType.TYPE_CLASS_TEXT + InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            }
            TextInputType.URL -> {
                InputType.TYPE_CLASS_TEXT + InputType.TYPE_TEXT_VARIATION_URI
            }
            TextInputType.EMAIL -> {
                InputType.TYPE_CLASS_TEXT + InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
            }
        }

        binding.textInput.setRawInputType(flags)
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
                binding.textInput.maxLines = 25
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

    private fun setActionButtonVisibleInternal(isVisible: Boolean) {
        binding.editTextActionButton.isVisible = isVisible

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

    private fun setActionButtonInternal(
        actionButton: ActionButton,
        isResetTextVisibility: Boolean
    ) {
        when (actionButton) {
            ActionButton.EYE -> {
                if (isResetTextVisibility && isTextVisible) {
                    isTextVisible = false
                }
                binding.editTextActionButton.setImageResource(getEyeIcon(isTextVisible))
            }
            ActionButton.CLEAR -> {
                binding.editTextActionButton.setImageResource(R.drawable.ic_close_24dp)
            }
        }
    }

    private fun setMaxLengthInternal(maxLength: Int) {
        binding.textInput.filters = arrayOf(InputFilter.LengthFilter(maxLength))
    }

    private fun determineIsActionButtonVisibleInternal(): Boolean {
        val shouldBeVisible = actionButton == ActionButton.EYE ||
            (actionButton == ActionButton.CLEAR && getTextInternal().isNotEmpty())

        return isActionButtonVisible && shouldBeVisible
    }

    @DrawableRes
    private fun getEyeIcon(isTextVisible: Boolean): Int {
        return if (isTextVisible) {
            R.drawable.ic_visibility_on_24dp
        } else {
            R.drawable.ic_visibility_off_24dp
        }
    }

    fun addTextWatcher(textWatcher: TextWatcher) {
        binding.textInput.addTextChangedListener(textWatcher)
    }

    fun removeTextWatcher(textWatcher: TextWatcher) {
        binding.textInput.removeTextChangedListener(textWatcher)
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

    enum class ActionButton {
        EYE,
        CLEAR
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
                view.twoWayBindingTextChangedListener = null
                return
            }

            view.twoWayBindingTextChangedListener = { textAttrChanged.onChange() }
        }

        @JvmStatic
        @BindingAdapter("onTextChanged")
        fun setOnTextChangedListener(
            view: MaterialEditText,
            onTextChangeListener: OnTextChangeListener?
        ) {
            val existingListener = view.getTag(R.id.tagTextWatcher) as? TextWatcher
            existingListener?.let {
                view.removeTextWatcher(it)
            }

            if (onTextChangeListener == null) {
                return
            }

            val listener = object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    onTextChangeListener.onTextChanged(s.toString())
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                }
            }
            view.setTag(R.id.tagTextWatcher, listener)
            view.addTextWatcher(listener)
        }

        @JvmStatic
        @BindingAdapter("onEditorAction")
        fun setOnEditorActionClickListener(
            view: MaterialEditText,
            listener: OnEditorActionListener?
        ) {
            view.onEditorAction = listener
        }

        @JvmStatic
        @BindingAdapter("onDoneAction")
        fun setOnDoneActionListener(
            view: MaterialEditText,
            listener: (() -> Unit)?
        ) {
            view.onDoneAction = listener
        }
    }
}