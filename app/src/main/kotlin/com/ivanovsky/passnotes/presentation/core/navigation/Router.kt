package com.ivanovsky.passnotes.presentation.core.navigation

import kotlin.reflect.KClass

interface Router {
    fun setRoot(vararg screens: Screen)
    fun navigateTo(screen: Screen)
    fun backTo(screen: Screen)
    fun replaceCurrent(screen: Screen)
    fun navigateBack()
    // fun showDialog(dialog: Dialog)
    // fun startActivity(event: StartActivityEvent)

    fun setResultListener(
        screenType: KClass<out Screen>,
        onResult: ResultListener
    )
    fun setResult(
        screenType: KClass<out Screen>,
        result: Any
    )
}

fun interface ResultListener {
    fun onResult(result: Any)
}

class RouterImpl(
    private val navigator: Navigator
) : Router by navigator