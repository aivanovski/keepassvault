package com.ivanovsky.passnotes.presentation.core_mvvm

import androidx.annotation.LayoutRes
import kotlin.math.sign
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

class ViewModelTypes {

    private val types = mutableListOf<Pair<String, Int>>()

    fun add(type: KClass<out BaseCellViewModel>, @LayoutRes layoutResId: Int): ViewModelTypes {
        types.add(Pair(type.jvmName, layoutResId))
        return this
    }

    fun getViewType(type: KClass<*>): Int = types.indexOfFirst { type.jvmName == it.first }

    @LayoutRes
    fun getLayoutResId(type: KClass<*>): Int {
        return types.firstOrNull { type.jvmName == it.first }?.second
            ?: throwNoLayoutId()
    }

    fun getLayoutResId(viewType: Int): Int {
        if (viewType < 0 || viewType > types.size) {
            throwNoLayoutId()
        }

        return types[viewType].second
    }

    private fun throwNoLayoutId(): Nothing {
        throw IllegalArgumentException("Unable to find layout id")
    }

}