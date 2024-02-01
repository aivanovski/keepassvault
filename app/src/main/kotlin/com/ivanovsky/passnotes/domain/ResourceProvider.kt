package com.ivanovsky.passnotes.domain

import android.content.Context
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.extensions.cloneWithTheme
import com.ivanovsky.passnotes.presentation.core.ThemeProvider

class ResourceProvider {

    private val themeProvider: ThemeProvider?
    private val appContext: Context
    private var themedContext: Context

    constructor(appContext: Context, themeProvider: ThemeProvider) {
        this.appContext = appContext
        this.themeProvider = themeProvider
        this.themedContext = appContext.cloneWithTheme(R.style.AppTheme)

        themeProvider.subscribe { _ ->
            themedContext = appContext.cloneWithTheme(R.style.AppTheme)
        }
    }

    constructor(themedContext: Context) {
        if (themedContext == themedContext.applicationContext) {
            throw IllegalArgumentException("Context must be themed")
        }

        this.appContext = themedContext.applicationContext
        this.themeProvider = null
        this.themedContext = themedContext
    }

    fun getString(@StringRes resId: Int): String {
        return appContext.getString(resId)
    }

    fun getString(@StringRes stringResId: Int, vararg formatArgs: Any?): String {
        return appContext.getString(stringResId, *formatArgs)
    }

    fun getColor(@ColorRes resId: Int): Int {
        return ResourcesCompat.getColor(themedContext.resources, resId, null)
    }

    fun getDimension(@DimenRes resId: Int): Int {
        return appContext.resources.getDimension(resId).toInt()
    }

    fun getAttributeColor(@AttrRes attr: Int): Int {
        val typedValue = TypedValue()
        themedContext.theme.resolveAttribute(attr, typedValue, true)
        val resourceId = typedValue.resourceId
        return getColor(resourceId)
    }
}