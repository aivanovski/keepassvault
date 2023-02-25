package com.ivanovsky.passnotes.presentation.noteEditor.cells.viewmodel

import androidx.lifecycle.MutableLiveData
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.noteEditor.cells.model.TextPropertyCellModel
import com.ivanovsky.passnotes.util.StringUtils.EMPTY

class TextPropertyCellViewModel(
    override val model: TextPropertyCellModel,
    private val eventProvider: EventProvider,
    private val resourceProvider: ResourceProvider
) : BaseCellViewModel(model), PropertyViewModel {

    val text = MutableLiveData(model.value)
    val error = MutableLiveData<String?>(null)

    override fun createProperty(): Property {
        return Property(
            type = model.propertyType,
            name = model.propertyName,
            value = text.value?.toString()?.trim(),
            isProtected = false
        )
    }

    override fun isDataValid(): Boolean {
        return model.isAllowEmpty || getTextInternal().isNotEmpty()
    }

    override fun displayError() {
        if (!model.isAllowEmpty && getTextInternal().isEmpty()) {
            error.value = resourceProvider.getString(R.string.should_not_be_empty)
        }
    }

    private fun getTextInternal(): String = text.value?.trim() ?: EMPTY
}