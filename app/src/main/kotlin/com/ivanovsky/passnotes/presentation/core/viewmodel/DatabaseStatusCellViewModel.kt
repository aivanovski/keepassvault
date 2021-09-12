package com.ivanovsky.passnotes.presentation.core.viewmodel

import androidx.lifecycle.MutableLiveData
import com.ivanovsky.passnotes.presentation.core.BaseMutableCellViewModel
import com.ivanovsky.passnotes.presentation.core.model.DatabaseStatusCellModel

class DatabaseStatusCellViewModel(
    initModel: DatabaseStatusCellModel
) : BaseMutableCellViewModel<DatabaseStatusCellModel>(initModel) {

    val text = MutableLiveData(initModel.text)
    val isVisible = MutableLiveData(initModel.isVisible)

    override fun setModel(newModel: DatabaseStatusCellModel) {
        super.setModel(newModel)
        text.value = newModel.text
        isVisible.value = newModel.isVisible
    }
}