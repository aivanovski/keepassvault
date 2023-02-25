package com.ivanovsky.passnotes.presentation.core.extensions

import android.os.Parcelable
import androidx.annotation.IdRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity

fun AppCompatActivity.initActionBar(@IdRes toolbarId: Int, action: (ActionBar.() -> Unit)? = null) {
    setSupportActionBar(findViewById(toolbarId))
    supportActionBar?.run {
        action?.invoke(this)
    }
}

fun <T : Parcelable> AppCompatActivity.getMandatoryExtra(key: String): T {
    return intent.extras?.getParcelable(key) ?: requireExtraArgument(key)
}

fun AppCompatActivity.requireExtraArgument(argumentName: String): Nothing {
    throw IllegalStateException(
        "${this::class.simpleName} requires argument with name: $argumentName"
    )
}