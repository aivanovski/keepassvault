package com.ivanovsky.passnotes.presentation.core.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.databinding.WidgetSelectorBinding
import com.ivanovsky.passnotes.util.StringUtils.EMPTY

class SelectorView(
    context: Context,
    attrs: AttributeSet
) : ConstraintLayout(context, attrs) {

    var text: String?
        get() = getTextFromView()
        set(value) {
            setTextToView(value)
        }

    var hint: String?
        get() = getHintFromView()
        set(value) {
            setHintToView(value)
        }

    var onChangeClicked: OnChangeButtonClickListener? = null

    private val binding: WidgetSelectorBinding

    init {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.widget_selector, this, true)
        binding = WidgetSelectorBinding.bind(view)

        binding.changeButton.setOnClickListener {
            onChangeClicked?.onChangeButtonClicked()
        }
    }

    private fun setTextToView(text: String?) {
        binding.text.text = text ?: EMPTY
    }

    private fun getTextFromView(): String {
        return binding.text.text.toString()
    }

    private fun setHintToView(hint: String?) {
        binding.hint.text = hint
    }

    private fun getHintFromView(): String {
        return binding.hint.text.toString()
    }

    interface OnChangeButtonClickListener {
        fun onChangeButtonClicked()
    }
}