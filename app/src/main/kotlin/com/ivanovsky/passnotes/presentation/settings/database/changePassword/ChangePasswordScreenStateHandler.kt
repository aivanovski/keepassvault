package com.ivanovsky.passnotes.presentation.settings.database.changePassword

import android.view.View
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.ivanovsky.passnotes.presentation.core.DefaultScreenStateHandler
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.ScreenStateType.LOADING
import com.ivanovsky.passnotes.presentation.core.widget.ErrorPanelView
import com.ivanovsky.passnotes.presentation.core.widget.ScreenStateView

class ChangePasswordScreenStateHandler : DefaultScreenStateHandler() {

    override fun applyScreenState(view: View, screenState: ScreenState) {
        when (screenState.type) {
            LOADING -> {
                // Make content invisible, in order to left dialog size as it is
                when (view) {
                    is ErrorPanelView -> {
                        view.isVisible = false
                    }
                    is ScreenStateView -> {
                        view.state = ScreenStateView.State.LOADING
                        view.isVisible = true
                    }
                    else -> view.isInvisible = true
                }
            }
            else -> super.applyScreenState(view, screenState)
        }
    }
}