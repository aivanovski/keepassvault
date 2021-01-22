package com.ivanovsky.passnotes.presentation.core_mvvm.extensions

import androidx.annotation.IdRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity

fun AppCompatActivity.initActionBar(@IdRes toolbarId: Int, action: (ActionBar.() -> Unit)? = null) {
    setSupportActionBar(findViewById(toolbarId))
    supportActionBar?.run {
        action?.invoke(this)
    }
}