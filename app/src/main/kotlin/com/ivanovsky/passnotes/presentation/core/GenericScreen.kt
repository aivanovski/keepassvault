package com.ivanovsky.passnotes.presentation.core

interface GenericScreen {

    var screenState: ScreenState
    fun showSnackbarMessage(message: String)
    fun showToastMessage(message: String)
    fun hideKeyboard()
    fun finishScreen()
}