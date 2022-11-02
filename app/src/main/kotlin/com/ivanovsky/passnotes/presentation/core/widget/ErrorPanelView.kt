package com.ivanovsky.passnotes.presentation.core.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.core.widget.ErrorPanelView.State.HIDDEN
import com.ivanovsky.passnotes.presentation.core.widget.ErrorPanelView.State.MESSAGE
import com.ivanovsky.passnotes.presentation.core.widget.ErrorPanelView.State.MESSAGE_WITH_RETRY
import com.ivanovsky.passnotes.presentation.core.widget.entity.OnButtonClickListener

class ErrorPanelView(
    context: Context,
    attrs: AttributeSet
) : ConstraintLayout(context, attrs) {

    var text: String?
        get() = errorTextView.text.toString()
        set(value) {
            errorTextView.text = value
        }

    var buttonText: String?
        get() = retryButton.text.toString()
        set(value) {
            retryButton.text = value
        }

    var state: State = HIDDEN
        set(value) {
            applyState(value)
            field = value
        }

    var buttonClickListener: OnButtonClickListener? = null

    private val errorTextView: TextView
    private val retryButton: Button

    init {
        setBackgroundResource(R.color.material_error_panel_background)

        LayoutInflater.from(context).inflate(R.layout.view_error_panel, this, true)

        errorTextView = findViewById(R.id.text)
        retryButton = findViewById(R.id.retryButton)
        retryButton.setOnClickListener { buttonClickListener?.onButtonClicked() }

        applyState(HIDDEN)
    }

    private fun applyState(state: State) {
        when (state) {
            MESSAGE -> {
                retryButton.isVisible = false
                errorTextView.isVisible = true
            }
            MESSAGE_WITH_RETRY -> {
                retryButton.isVisible = true
                errorTextView.isVisible = true
            }
            HIDDEN -> {
                retryButton.isVisible = false
                errorTextView.isVisible = false
            }
        }
    }

    enum class State {
        HIDDEN,
        MESSAGE,
        MESSAGE_WITH_RETRY
    }
}