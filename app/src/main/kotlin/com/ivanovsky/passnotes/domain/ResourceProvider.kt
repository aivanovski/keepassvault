package com.ivanovsky.passnotes.domain

import android.content.Context
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat

class ResourceProvider(private val context: Context) {

    fun getString(@StringRes resId: Int): String {
        return context.getString(resId)
    }

    fun getString(@StringRes stringResId: Int, vararg formatArgs: Any): String {
        return context.getString(stringResId, *formatArgs)
    }

    fun getColor(@ColorRes resId: Int): Int {
        return ResourcesCompat.getColor(context.resources, resId, null)
    }

    fun getDimension(@DimenRes resId: Int): Int {
        return context.resources.getDimension(resId).toInt()
    }
}