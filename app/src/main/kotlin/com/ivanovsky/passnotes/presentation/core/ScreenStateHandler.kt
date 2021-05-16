package com.ivanovsky.passnotes.presentation.core

import android.view.View

interface ScreenStateHandler {

    fun applyScreenState(view: View, screenState: ScreenState)
}