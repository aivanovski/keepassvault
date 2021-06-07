package com.ivanovsky.passnotes.presentation.unlock.cells.viewmodel

import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.unlock.cells.model.DatabaseCellModel

class DatabaseCellViewModel(
    override val model: DatabaseCellModel
) : BaseCellViewModel(model) {

    fun onClicked() {
        model.onClicked?.invoke(model.id)
    }
}