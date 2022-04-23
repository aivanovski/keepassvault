package com.ivanovsky.passnotes.presentation.core.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
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

    var textColor: Int?
        get() = getTextColorFromView()
        set(value) {
            setTextColorToView(value)
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

    private fun getTextColorFromView(): Int {
        return binding.text.currentTextColor
    }

    private fun setTextColorToView(color: Int?) {
        val defaultColor = ResourcesCompat.getColor(resources, R.color.primary_text, null)
        return binding.text.setTextColor(color ?: defaultColor)
    }

    interface OnChangeButtonClickListener {
        fun onChangeButtonClicked()
    }
}