package com.ivanovsky.passnotes.presentation.core

import androidx.annotation.CallSuper
import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel

abstract class BaseMutableCellViewModel<T : BaseCellModel>(
    protected var mutableModel: BaseCellModel
) : BaseCellViewModel(mutableModel) {

    override val model: T
        get() = mutableModel as T

    @CallSuper
    open fun setModel(newModel: T) {
        mutableModel = newModel
    }
}