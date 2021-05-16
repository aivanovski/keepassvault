package com.ivanovsky.passnotes.presentation.core.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.core.widget.ErrorPanelView.State.MESSAGE
import com.ivanovsky.passnotes.presentation.core.widget.ErrorPanelView.State.MESSAGE_WITH_RETRY

class ErrorPanelView(
    context: Context,
    attrs: AttributeSet
) : LinearLayout(context, attrs) {

    var text: String?
        get() = errorTextView.text.toString()
        set(value) {
            errorTextView.text = value
        }

    var state: State = MESSAGE
        set(value) {
            applyState(value)
            field = value
        }

    private val errorTextView: TextView
    private val retryButton: View

    init {
        setBackgroundResource(R.color.material_error_panel_background)
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_HORIZONTAL

        LayoutInflater.from(context).inflate(R.layout.view_error_panel, this, true)

        errorTextView = findViewById(R.id.text)
        retryButton = findViewById(R.id.retryButton)
    }

    private fun applyState(state: State) {
        when (state) {
            MESSAGE -> {
                retryButton.isVisible = false
            }
            MESSAGE_WITH_RETRY -> {
                retryButton.isVisible = true
            }
        }
    }

    enum class State {
        MESSAGE,
        MESSAGE_WITH_RETRY
    }
}