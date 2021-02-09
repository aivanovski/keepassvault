package com.ivanovsky.passnotes.presentation.core.widget

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.widget.TextView

class SecureTextView : TextView {

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    fun hideText() {
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
    }

    fun showText() {
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL
    }
}