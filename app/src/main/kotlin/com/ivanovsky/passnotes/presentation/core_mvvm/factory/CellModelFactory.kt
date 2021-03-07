package com.ivanovsky.passnotes.presentation.core_mvvm.factory

import com.ivanovsky.passnotes.presentation.core_mvvm.model.BaseCellModel

interface CellModelFactory<T> {

    fun createCellModels(data: T): List<BaseCellModel>
}