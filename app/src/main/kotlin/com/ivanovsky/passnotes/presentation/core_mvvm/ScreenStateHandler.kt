package com.ivanovsky.passnotes.presentation.core_mvvm

import android.view.View

interface ScreenStateHandler {

    fun applyScreenState(view: View, screenState: ScreenState)
}