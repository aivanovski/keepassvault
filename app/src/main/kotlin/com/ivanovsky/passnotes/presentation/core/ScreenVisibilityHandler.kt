package com.ivanovsky.passnotes.presentation.core

import android.view.View

interface ScreenVisibilityHandler {

    fun applyScreenState(view: View, screenState: ScreenState)
}