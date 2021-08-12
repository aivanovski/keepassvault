package com.ivanovsky.passnotes.extensions

import android.view.Menu
import androidx.annotation.IdRes

fun Menu.setItemVisibility(@IdRes id: Int, isVisible: Boolean) {
    findItem(id)?.isVisible = isVisible
}