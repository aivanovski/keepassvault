package com.ivanovsky.passnotes.presentation.core_mvvm

import android.view.View
import androidx.core.view.isVisible
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.ivanovsky.passnotes.presentation.core.widget.ExpandableFloatingActionButton
import com.ivanovsky.passnotes.presentation.core_mvvm.ScreenDisplayingType.*
import com.ivanovsky.passnotes.presentation.core_mvvm.widget.ErrorPanelView
import com.ivanovsky.passnotes.presentation.core_mvvm.widget.ScreenStateView

open class DefaultScreenStateHandler : ScreenStateHandler {

    override fun applyScreenState(view: View, screenState: ScreenState) {
        when (screenState.screenDisplayingType) {
            DATA -> {
                view.isVisible = (view !is ScreenStateView && view !is ErrorPanelView)
            }
            DATA_WITH_ERROR -> {
                if (view is ErrorPanelView) {
                    view.text = screenState.errorText
                    view.state = ErrorPanelView.State.MESSAGE
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