package com.ivanovsky.passnotes.presentation.core

import androidx.annotation.DrawableRes

sealed class BackNavigationIcon {

    object Arrow : BackNavigationIcon()

    data class Icon(
        @DrawableRes val iconResourceId: Int
    ) : BackNavigationIcon()
}