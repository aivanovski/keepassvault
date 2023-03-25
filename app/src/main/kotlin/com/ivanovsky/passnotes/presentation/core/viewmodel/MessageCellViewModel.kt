package com.ivanovsky.passnotes.presentation.core.viewmodel

import androidx.lifecycle.MutableLiveData
import com.ivanovsky.passnotes.presentation.core.BaseMutableCellViewModel
import com.ivanovsky.passnotes.presentation.core.model.MessageCellModel

class MessageCellViewModel(
    initModel: MessageCellModel
) : BaseMutableCellViewModel<MessageCellModel>(initModel) {

    val text = MutableLiveData(initModel.text)
    val backgroundColor = MutableLiveData(initModel.backgroundColor)
    val isVisible = MutableLiveData(initModel.isVisible)

    override fun setModel(newModel: MessageCellModel) {
        super.setModel(newModel)
        text.value = newModel.text
        backgroundColor.value = newModel.backgroundColor
        isVisible.value = newModel.isVisible
    }
}