package com.ivanovsky.passnotes.domain

import android.content.Context
import com.ivanovsky.passnotes.util.ThemeUtils
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ThemeProvider(
    dispatchers: DispatcherProvider
) {

    private val currentTheme = AtomicReference<Theme?>()
    private val listeners = CopyOnWriteArrayList<ThemeChangeListener>()
    private val scope = CoroutineScope(dispatchers.Main)

    fun onThemeContextCreated(context: Context) {
        val isNightMode = ThemeUtils.isNightMode(context)

        val theme = if (isNightMode) {
            Theme.DARK
        } else {
            Theme.LIGHT
        }

        if (theme != currentTheme.get()) {
            currentTheme.set(theme)

            scope.launch {
                listeners.forEach { listener -> listener.onThemeChanged(theme) }
            }
        }
    }

    fun subscribe(listener: ThemeChangeListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    fun unsubscribe(listener: ThemeChangeListener) {
        listeners.remove(listener)
    }

    fun interface ThemeChangeListener {
        fun onThemeChanged(theme: Theme)
    }

    enum class Theme {
        LIGHT,
        DARK
    }
}