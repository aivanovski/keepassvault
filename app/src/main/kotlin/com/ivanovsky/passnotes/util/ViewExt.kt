package com.ivanovsky.passnotes.util

import android.view.View
import android.view.ViewGroup

fun View.setVisible(isVisible: Boolean) {
    this.visibility = if (isVisible) View.VISIBLE else View.GONE
}

fun ViewGroup.getChildViews(): List<View> {
    return (0 until childCount)
        .map { getChildAt(it) }
}