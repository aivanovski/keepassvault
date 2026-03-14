package com.ivanovsky.passnotes.presentation.core.navigation

import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import java.lang.ref.Reference
import java.lang.ref.WeakReference
import java.util.Deque
import java.util.LinkedList
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import timber.log.Timber

interface NavigationHost {
    fun handleEvent(event: NavigationEvent)
    fun setResultListener(
        screenType: KClass<out Screen>,
        onResult: ResultListener
    )

    fun setResult(
        screenType: KClass<out Screen>,
        result: Any
    )
}

class NavigationHostImpl(
    @IdRes
    private val fragmentContainerResId: Int,
    private val fragmentManager: FragmentManager,
    private val onExit: () -> Unit
) : NavigationHost {

    private val stack = LinkedList<StackItem>()
    private val resultListeners: MutableMap<String, Deque<ResultListener>> =
        ConcurrentHashMap()

    override fun handleEvent(event: NavigationEvent) {
        when (event) {
            is NavigationEvent.SetRoot -> setRoot(event.screens)
            is NavigationEvent.NavigateTo -> navigateTo(event.screen)
            is NavigationEvent.ReplaceCurrent -> replaceCurrent(event.screen)
            is NavigationEvent.BackTo -> backTo(event.screen)
            NavigationEvent.Back -> navigateBack()
        }
    }

    override fun setResultListener(
        screenType: KClass<out Screen>,
        onResult: ResultListener
    ) {
        val screenTag = screenType.java.name
        resultListeners[screenTag] = resultListeners.getOrDefault(screenTag, LinkedList())
            .apply {
                add(onResult)
            }
    }

    override fun setResult(
        screenType: KClass<out Screen>,
        result: Any
    ) {
        val screenTag = screenType.java.name
        val listeners = resultListeners[screenTag]

        Timber.d(
            "setResult: screenTag=%s, screenListeners=%s, listeners.size=%s",
            screenTag,
            listeners?.size ?: 0,
            resultListeners.size
        )

        listeners?.pop()?.onResult(result)

        if (listeners == null || listeners.isEmpty()) {
            resultListeners.remove(screenTag)
        }
    }

    private fun navigateTo(screen: Screen) {
        val fragment = screen.create()
        val newItem = screen.toStackItem()

        val currItem = stack.first()
        val currFragment = fragmentManager.findFragmentByTag(currItem.tag)

        if (currFragment != null) {
            val savedState = fragmentManager.saveFragmentInstanceState(currFragment)
            stack[0] = currItem.copy(
                savedState = savedState
            )
        }

        stack.push(newItem)

        fragmentManager.beginTransaction()
            .replace(fragmentContainerResId, fragment, newItem.tag)
            .commit()
    }

    private fun setRoot(screens: List<Screen>) {
        val fragment = screens.last().create()
        val item = screens.last().toStackItem()

        stack.clear()
        for (screen in screens.dropLast(1)) {
            stack.push(screen.toStackItem())
        }
        stack.push(item)

        resultListeners.clear()

        fragmentManager.beginTransaction()
            .replace(fragmentContainerResId, fragment, item.tag)
            .commit()
    }

    private fun replaceCurrent(screen: Screen) {
        val fragment = screen.create()
        val newItem = screen.toStackItem()

        stack.pop()
        stack.push(newItem)

        fragmentManager.beginTransaction()
            .replace(fragmentContainerResId, fragment, newItem.tag)
            .commit()
    }

    private fun backTo(screen: Screen) {
        val fragment = screen.create()
        val tag = fragment::class.java.simpleName

        while (stack.isNotEmpty() && tag != stack.firstOrNull()?.tag) {
            stack.pop()
        }

        val newItem = stack.first()
        fragment.setInitialSavedState(newItem.savedState)

        fragmentManager.beginTransaction()
            .replace(fragmentContainerResId, fragment, newItem.tag)
            .commit()
    }

    private fun navigateBack() {
        if (stack.size > 1) {
            stack.pop()

            val newItem = stack.first()
            val fragment = newItem.screen.create()
            fragment.setInitialSavedState(newItem.savedState);

            val screenTag = newItem.tag
            val listeners = resultListeners[screenTag]
            Timber.d(
                "exit: screenTag=%s, screenListener=%s, listeners.size=%s",
                screenTag,
                listeners?.size ?: 0,
                resultListeners.size
            )

            listeners?.pop()
            if (listeners != null && listeners.isEmpty()) {
                resultListeners.remove(screenTag)
            }

            fragmentManager.beginTransaction()
                .replace(fragmentContainerResId, fragment, newItem.tag)
                .commit()
        } else {
            onExit.invoke()
        }
    }

    private fun Screen.toStackItem(): StackItem =
        StackItem(
            screen = this,
            tag = this::class.java.name
        )

    private data class StackItem(
        val screen: Screen,
        val tag: String,
        val savedState: Fragment.SavedState? = null
    )
}