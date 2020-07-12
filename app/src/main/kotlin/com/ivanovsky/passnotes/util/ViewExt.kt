package com.ivanovsky.passnotes.util

import android.view.View
import androidx.annotation.StringRes

fun View.setVisible(isVisible: Boolean) {
    this.visibility = if (isVisible) View.VISIBLE else View.GONE
}

