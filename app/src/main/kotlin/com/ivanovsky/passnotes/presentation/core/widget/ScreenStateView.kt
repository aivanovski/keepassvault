package com.ivanovsky.passnotes.presentation.core.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import com.ivanovsky.passnotes.R

class ScreenStateView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    constructor(context: Context) : this(context, null)

    private val progressBar: ProgressBar
    private val emptyTextView: TextView
    private val errorTextView: TextView
    private val errorLayout: View

    var emptyText: String?
        get() = emptyTextView.text.toString()
        set(value) {
            emptyTextView.text = value
        }

    var errorText: String?
        get() = errorTextView.text.toString()
        set(value) {
            errorTextView.text = value
        }

    var state: State = State.LOADING
        set(value) {
            applyState(value)
            field = value
        }

    init {
        LayoutInflater.from(context).inflate(R.layout.view_screen_state, this)

        progressBar = findViewById(R.id.progressBar)
        emptyTextView = findViewById(R.id.emptyTextView)
        errorTextView = findViewById(R.id.errorTextView)
        errorLayout = findViewById(R.id.errorLayout)
    }

    private fun applyState(state: State) {
        when (state) {
            State.LOADING -> {
                progressBar.isVisible = true
                emptyTextView.isVisible = false
                errorLayout.isVisible = false
            }
            State.EMPTY -> {
                progressBar.isVisible = false
                emptyTextView.isVisible = true
                errorLayout.isVisible = false
            }
            State.ERROR -> {
                progressBar.isVisible = false
                emptyTextView.isVisible = false
                errorLayout.isVisible = true
            }
        }
    }

    enum class State {
        LOADING,
        EMPTY,
        ERROR,
        ERROR_PANEL
    }
}