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
import com.ivanovsky.passnotes.databinding.WidgetUnlockViewBinding
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.injection.GlobalInjector.inject
import com.ivanovsky.passnotes.presentation.core.binding.setMaterialBackgroundColor
import com.ivanovsky.passnotes.presentation.core.widget.entity.ImeOptions
import com.ivanovsky.passnotes.presentation.core.widget.entity.OnButtonClickListener
import com.ivanovsky.passnotes.presentation.core.widget.entity.OnButtonLongClickListener
import com.ivanovsky.passnotes.presentation.core.widget.entity.OnEditorActionListener
import com.ivanovsky.passnotes.util.StringUtils.EMPTY

class UnlockView(
    context: Context,
    attrs: AttributeSet
) : ConstraintLayout(context, attrs) {

    var title: String = EMPTY
        set(value) {
            setTitleInternal(value)
            field = value
        }

    var keyTitle: String = EMPTY
        set(value) {
            setKeyTitleInternal(value)
            field = value
        }

    var password: String
        get() = getPasswordInternal()
        set(value) {
            setPasswordInternal(value, isNotifyListener = true)
        }

    var unlockIconResId: Int? = null
        set(value) {
            setUnlockIconResIdInternal(value)
            field = value
        }

    var isAddButtonVisible: Boolean = true
        set(value) {
            setAddButtonVisibleInternal(value)
            field = value
        }

    var isRemoveButtonVisible: Boolean = false
        set(value) {
            setRemoveButtonVisibleInternal(value)
            field = value
        }

    var onUnlockButtonClicked: OnButtonClickListener? = null
    var onUnlockButtonLongClicked: OnButtonLongClickListener? = null
    var onAddButtonClicked: OnButtonClickListener? = null
    var onRemoveButtonClicked: OnButtonClickListener? = null
    var onEditorAction: OnEditorActionListener? = null

    private val binding: WidgetUnlockViewBinding
    private val resourceProvider: ResourceProvider by inject()
    private var twoWayBindingPasswordChangedListener: (InverseBindingListener)? = null
    private val passwordWatcher: PasswordTextWatcher

    init {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.widget_unlock_view, this, true)
        binding = WidgetUnlockViewBinding.bind(view)

        passwordWatcher = PasswordTextWatcher()
        binding.password.addTextWatcher(passwordWatcher)
        binding.password.onEditorAction = object : OnEditorActionListener {
            override fun onEditorAction(actionId: Int) {
                onEditorAction?.onEditorAction(actionId)
            }
        }
        binding.password.imeOptions = ImeOptions.ACTION_DONE

        setMaterialBackgroundColor(
            binding.unlockButton,
            resourceProvider.getAttributeColor(R.attr.kpPrimaryColor)
        )

        setAddButtonVisibleInternal(isAddButtonVisible)
        setRemoveButtonVisibleInternal(isRemoveButtonVisible)

        binding.unlockButton.setOnClickListener {
            onUnlockButtonClicked?.onButtonClicked()
        }
        binding.unlockButton.setOnLongClickListener {
            if (onUnlockButtonLongClicked != null) {
                onUnlockButtonLongClicked?.onButtonLongClicked()
                true
            } else {
                false
            }
        }
        binding.addKeyButton.setOnClickListener {
            onAddButtonClicked?.onButtonClicked()
        }
        binding.removeKeyButton.setOnClickListener {
            onRemoveButtonClicked?.onButtonClicked()
        }
    }

    fun requestSoftInput() {
        binding.password.requestSoftInput()
    }

    fun hideSoftInput() {
        binding.password.hideSoftInput()
    }

    private fun setTitleInternal(title: String) {
        binding.filename.text = title
    }

    private fun setKeyTitleInternal(keyTitle: String) {
        binding.removeKeyText.text = keyTitle
    }

    private fun getPasswordInternal(): String = binding.password.text

    private fun setPasswordInternal(
        password: String,
        isNotifyListener: Boolean
    ) {
        if (getPasswordInternal() == password) {
            return
        }

        if (!isNotifyListener) {
            passwordWatcher.isEnabled = false
        }

        binding.password.text = password

        if (!isNotifyListener) {
            passwordWatcher.isEnabled = true
        }
    }

    private fun setUnlockIconResIdInternal(iconResourceId: Int?) {
        if (iconResourceId != null) {
            binding.unlockButton.setImageResource(iconResourceId)
        } else {
            binding.unlockButton.setImageDrawable(null)
        }
    }

    private fun setAddButtonVisibleInternal(isVisible: Boolean) {
        binding.addKeyButton.isVisible = isVisible
    }

    private fun setRemoveButtonVisibleInternal(isVisible: Boolean) {
        binding.removeKeyButtonLayout.isVisible = isVisible
    }

    private fun onPasswordChanged(password: String) {
        twoWayBindingPasswordChangedListener?.onChange()
    }

    private inner class PasswordTextWatcher : TextWatcher {

        var isEnabled = true

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(s: Editable?) {
            if (isEnabled) {
                onPasswordChanged(s.toString())
            }
        }
    }

    companion object {
        @JvmStatic
        @InverseBindingAdapter(attribute = "password")
        fun getPassword(view: UnlockView): String = view.getPasswordInternal()

        @JvmStatic
        @BindingAdapter("password")
        fun setPassword(view: UnlockView, password: String) {
            view.setPasswordInternal(password, isNotifyListener = false)
        }

        @JvmStatic
        @BindingAdapter("passwordAttrChanged")
        fun setPasswordListener(
            view: UnlockView,
            passwordAttrChanged: InverseBindingListener?
        ) {
            if (passwordAttrChanged == null) {
                view.twoWayBindingPasswordChangedListener = null
                return
            }

            view.twoWayBindingPasswordChangedListener = passwordAttrChanged
        }

        @JvmStatic
        @BindingAdapter("onUnlockClicked")
        fun setOnUnlockButtonClickListener(
            view: UnlockView,
            listener: OnButtonClickListener?
        ) {
            view.onUnlockButtonClicked = listener
        }

        @JvmStatic
        @BindingAdapter("onUnlockLongClicked")
        fun setOnUnlockButtonLongClickListener(
            view: UnlockView,
            listener: OnButtonLongClickListener?
        ) {
            view.onUnlockButtonLongClicked = listener
        }

        @JvmStatic
        @BindingAdapter("onAddClicked")
        fun setOnAddButtonClickListener(
            view: UnlockView,
            listener: OnButtonClickListener?
        ) {
            view.onAddButtonClicked = listener
        }

        @JvmStatic
        @BindingAdapter("onRemoveClicked")
        fun setOnRemoveButtonClickListener(
            view: UnlockView,
            listener: OnButtonClickListener?
        ) {
            view.onRemoveButtonClicked = listener
        }

        @JvmStatic
        @BindingAdapter("onEditorAction")
        fun setOnEditorActionClickListener(
            view: UnlockView,
            listener: OnEditorActionListener?
        ) {
            view.onEditorAction = listener
        }
    }
}