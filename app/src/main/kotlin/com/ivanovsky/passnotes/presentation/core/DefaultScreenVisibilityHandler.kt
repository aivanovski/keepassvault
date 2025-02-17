package com.ivanovsky.passnotes.presentation.core

import android.view.View
import androidx.core.view.isVisible
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.ivanovsky.passnotes.presentation.core.ScreenStateType.DATA
import com.ivanovsky.passnotes.presentation.core.ScreenStateType.DATA_WITH_ERROR
import com.ivanovsky.passnotes.presentation.core.ScreenStateType.EMPTY
import com.ivanovsky.passnotes.presentation.core.ScreenStateType.ERROR
import com.ivanovsky.passnotes.presentation.core.ScreenStateType.LOADING
import com.ivanovsky.passnotes.presentation.core.ScreenStateType.NOT_INITIALIZED
import com.ivanovsky.passnotes.presentation.core.widget.ErrorPanelView
import com.ivanovsky.passnotes.presentation.core.widget.ScreenStateView

open class DefaultScreenVisibilityHandler : ScreenVisibilityHandler {

    override fun applyScreenState(view: View, screenState: ScreenState) {
        when (screenState.type) {
            DATA -> {
                view.isVisible = (view !is ScreenStateView && view !is ErrorPanelView)
            }

            DATA_WITH_ERROR -> {
                view.isVisible = (view !is ScreenStateView)
            }

            EMPTY -> {
                view.isVisible = (view is ScreenStateView || view is FloatingActionButton)
            }

            LOADING -> {
                view.isVisible = (view is ScreenStateView)
            }

            ERROR -> {
                view.isVisible = (view is ScreenStateView)
            }

            NOT_INITIALIZED -> {
                view.isVisible = false
            }
        }
    }
}