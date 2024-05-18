package com.ivanovsky.passnotes.presentation.core.dialog.propertyAction

sealed interface PropertyAction {

    val title: String

    data class CopyText(
        override val title: String,
        val text: String,
        val isProtected: Boolean
    ) : PropertyAction

    data class OpenUrl(
        override val title: String,
        val url: String
    ) : PropertyAction
}