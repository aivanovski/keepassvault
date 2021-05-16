package com.ivanovsky.passnotes.presentation.core.extensions

import androidx.annotation.IdRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity

fun AppCompatActivity.initActionBar(@IdRes toolbarId: Int, action: (ActionBar.() -> Unit)? = null) {
    setSupportActionBar(findViewById(toolbarId))
    supportActionBar?.run {
        action?.invoke(this)
    }
}

fun AppCompatActivity.requireExtraValue(argumentName: String): Nothing {
    throw IllegalStateException("require extra argument with name: $argumentName")
}