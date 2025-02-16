package com.ivanovsky.passnotes.presentation.core.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.ivanovsky.passnotes.databinding.ViewScreenStateBinding
import com.ivanovsky.passnotes.presentation.core.ScreenState

class ScreenStateView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    // TODO: remove if not need
    constructor(context: Context) : this(context, null)

    var state: ScreenState? = null
        set(value) {
            field = value
            applyState(value)
        }

    private val binding = ViewScreenStateBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )

    private fun applyState(screenState: ScreenState?) {
        if (screenState == null) return

        binding.errorView.state = screenState
        binding.emptyTextView.text = screenState.emptyText

        binding.progressBar.isVisible = screenState.isDisplayingLoading
        binding.emptyTextView.isVisible = screenState.isDisplayingEmptyState
        binding.errorView.isVisible = screenState.isDisplayingError
    }
}