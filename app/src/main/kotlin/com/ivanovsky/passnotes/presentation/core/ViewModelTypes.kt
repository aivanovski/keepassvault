package com.ivanovsky.passnotes.presentation.core

import androidx.annotation.LayoutRes
import kotlin.reflect.KClass

class ViewModelTypes {

    private val types = mutableListOf<Pair<String, Int>>()

    fun add(type: KClass<out BaseCellViewModel>, @LayoutRes layoutResId: Int): ViewModelTypes {
        types.add(Pair(keyFromType(type), layoutResId))
        return this
    }

    fun getViewType(type: KClass<*>): Int = types.indexOfFirst { keyFromType(type) == it.first }

    @LayoutRes
    fun getLayoutResId(type: KClass<*>): Int {
        return types.firstOrNull { keyFromType(type) == it.first }?.second
            ?: throwNoLayoutId()
    }

    fun getLayoutResId(viewType: Int): Int {
        if (viewType < 0 || viewType > types.size) {
            throwNoLayoutId()
        }

        return types[viewType].second
    }

    private fun keyFromType(type: KClass<*>): String {
        return type.java.name
    }

    private fun throwNoLayoutId(): Nothing {
        throw IllegalArgumentException("Unable to find layout id")
    }

}