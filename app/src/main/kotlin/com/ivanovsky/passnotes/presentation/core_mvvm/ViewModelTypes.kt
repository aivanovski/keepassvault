package com.ivanovsky.passnotes.presentation.core_mvvm

import androidx.annotation.LayoutRes
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

class ViewModelTypes {

    private val types = mutableListOf<Pair<String, Int>>()

    fun add(type: KClass<*>, @LayoutRes layoutResId: Int): ViewModelTypes {
        types.add(Pair(type.jvmName, layoutResId))
        return this
    }

    fun getViewType(type: KClass<*>): Int = types.indexOfFirst { type.jvmName == it.first }

    @LayoutRes
    fun getLayoutResId(type: KClass<*>): Int {
        return types.firstOrNull { type.jvmName == it.first }?.second
            ?: throw IllegalArgumentException("Unable to find layout id for type: ${type.jvmName}")
    }

    fun getLayoutResId(viewType: Int): Int = types[viewType].second
}