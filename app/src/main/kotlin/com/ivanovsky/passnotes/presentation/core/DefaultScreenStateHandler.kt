package com.ivanovsky.passnotes.presentation.core

import android.view.View
import androidx.core.view.isVisible
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.ivanovsky.passnotes.presentation.core.ScreenDisplayingType.DATA
import com.ivanovsky.passnotes.presentation.core.ScreenDisplayingType.DATA_WITH_ERROR
import com.ivanovsky.passnotes.presentation.core.ScreenDisplayingType.EMPTY
import com.ivanovsky.passnotes.presentation.core.ScreenDisplayingType.ERROR
import com.ivanovsky.passnotes.presentation.core.ScreenDisplayingType.LOADING
import com.ivanovsky.passnotes.presentation.core.ScreenDisplayingType.NOT_INITIALIZED
import com.ivanovsky.passnotes.presentation.core.widget.ErrorPanelView
import com.ivanovsky.passnotes.presentation.core.widget.ExpandableFloatingActionButton
import com.ivanovsky.passnotes.presentation.core.widget.ScreenStateView

open class DefaultScreenStateHandler : ScreenStateHandler {

    override fun applyScreenState(view: View, screenState: ScreenState) {
        when (screenState.screenDisplayingType) {
            DATA -> {
                view.isVisible = (view !is ScreenStateView && view !is ErrorPanelView)
            }
            DATA_WITH_ERROR -> {
                if (view is ErrorPanelView) {
                    view.state = if (screenState.errorButtonText.isNullOrEmpty()) {
                        ErrorPanelView.State.MESSAGE
                    } else {
                        ErrorPanelView.State.MESSAGE_WITH_RETRY
                    }
                    view.text = screenState.errorText
                    view.buttonText = screenState.errorButtonText
                    view.isVisible = true
                } else {
                    view.isVisible = (view !is ScreenStateView)
                }
            }
            EMPTY -> {
                if (view is ScreenStateView) {
                    view.emptyText = screenState.emptyText
                    view.state = ScreenStateView.State.EMPTY
                    view.isVisible = true
                } else {
                    view.isVisible = (view is FloatingActionButton || view is ExpandableFloatingActionButton)
                }
            }
            LOADING -> {
                if (view is ScreenStateView) {
                    view.state = ScreenStateView.State.LOADING
                    view.isVisible = true
                } else {
                    view.isVisible = false
                }
            }
            ERROR -> {
                if (view is ScreenStateView) {
                    view.state = ScreenStateView.State.ERROR
                    view.errorText = screenState.errorText
                    view.isVisible = true
                } else {
                    view.isVisible = false
                }
            }
            NOT_INITIALIZED -> {
            }
        }
    }
}