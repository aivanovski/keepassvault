package com.ivanovsky.passnotes.presentation.core.compose.cells

import kotlinx.coroutines.flow.MutableStateFlow

abstract class MutableCellViewModel<T : CellModel>(
    initialModel: T
) : CellViewModel {

    val observableModel = MutableStateFlow(initialModel)

    override val model: T
        get() = observableModel.value
}