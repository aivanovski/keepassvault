package com.ivanovsky.passnotes.presentation.core.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.databinding.ViewScreenStateBinding

class ScreenStateView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    constructor(context: Context) : this(context, null)

    private val binding: ViewScreenStateBinding

    var emptyText: String?
        get() = binding.emptyTextView.text.toString()
        set(value) {
            binding.emptyTextView.text = value
        }

    var errorText: String?
        get() = binding.errorText.text.toString()
        set(value) {
            binding.errorText.text = value
        }

    var state: State = State.LOADING
        set(value) {
            applyState(value)
            field = value
        }

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.view_screen_state, this, true)
        binding = ViewScreenStateBinding.bind(view)

        applyState(state)
    }

    private fun applyState(state: State) {
        when (state) {
            State.LOADING -> {
                binding.progressBar.isVisible = true
                binding.emptyTextView.isVisible = false
                binding.errorLayout.isVisible = false
            }
            State.EMPTY -> {
                binding.progressBar.isVisible = false
                binding.emptyTextView.isVisible = true
                binding.errorLayout.isVisible = false
            }
            State.ERROR -> {
                binding.progressBar.isVisible = false
                binding.emptyTextView.isVisible = false
                binding.errorLayout.isVisible = true
            }
        }
    }

    enum class State {
        LOADING,
        EMPTY,
        ERROR
    }
}