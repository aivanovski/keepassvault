package com.ivanovsky.passnotes.presentation.core.factory

import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel

interface CellModelFactory<T> {

    fun createCellModels(data: T): List<BaseCellModel>
}