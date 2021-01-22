package com.ivanovsky.passnotes.presentation.core_mvvm.factory

import com.ivanovsky.passnotes.presentation.core_mvvm.BaseItemViewModel
import com.ivanovsky.passnotes.presentation.core_mvvm.model.BaseItemModel
import kotlin.reflect.jvm.jvmName

interface ItemViewModelFactory {

    fun createItemViewModel(model: BaseItemModel): BaseItemViewModel

    fun throwViewModelNotFoundException(model: BaseItemModel): Nothing {
        throw IllegalArgumentException("unable to find ViewModel for model: ${model::class.jvmName}")
    }

    fun createItemViewModels(models: List<BaseItemModel>): List<BaseItemViewModel> {
        return models.map { createItemViewModel(it) }
    }
}