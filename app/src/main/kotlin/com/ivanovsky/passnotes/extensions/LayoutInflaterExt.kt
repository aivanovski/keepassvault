package com.ivanovsky.passnotes.extensions

import android.view.LayoutInflater
import androidx.annotation.StyleRes

fun LayoutInflater.cloneInContext(@StyleRes themeResId: Int): LayoutInflater {
    return cloneInContext(context.cloneWithTheme(themeResId))
}