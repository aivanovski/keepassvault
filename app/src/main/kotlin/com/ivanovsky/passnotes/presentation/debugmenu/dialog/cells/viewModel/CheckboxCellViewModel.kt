package com.ivanovsky.passnotes.presentation.debugmenu.dialog.cells.viewModel

import androidx.lifecycle.MutableLiveData
import com.ivanovsky.passnotes.presentation.core.BaseMutableCellViewModel
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.debugmenu.dialog.cells.model.CheckboxCellModel

class CheckboxCellViewModel(
    initModel: CheckboxCellModel,
    private val eventProvider: EventProvider,
) : BaseMutableCellViewModel<CheckboxCellModel>(initModel) {

    val title = MutableLiveData(initModel.title)
    val description = MutableLiveData(initModel.description)
    val isChecked = MutableLiveData(initModel.isChecked)

    override fun setModel(newModel: CheckboxCellModel) {
        super.setModel(newModel)
        title.value = newModel.title
        description.value = newModel.description
        isChecked.value = newModel.isChecked
    }

    fun onCheckedChanged(isChecked: Boolean) {
        // val model = mutableModel as CheckboxCellModel
        // setModel(model.copy(isChecked = isChecked))
    }
}