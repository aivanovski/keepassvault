package com.ivanovsky.passnotes.presentation.note_editor.cells.viewmodel

import androidx.lifecycle.MutableLiveData
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.presentation.core_mvvm.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core_mvvm.event.EventProvider
import com.ivanovsky.passnotes.presentation.note_editor.cells.model.SecretPropertyCellModel
import com.ivanovsky.passnotes.presentation.note_editor.view.TextTransformationMethod
import com.ivanovsky.passnotes.util.StringUtils.EMPTY

class SecretPropertyCellViewModel(
    override val model: SecretPropertyCellModel,
    private val eventProvider: EventProvider,
    private val resourceProvider: ResourceProvider
) : BaseCellViewModel(model), PropertyViewModel {

    val secretText = MutableLiveData(model.value)
    val confirmationText = MutableLiveData(model.value)
    val confirmationError = MutableLiveData<String?>(null)
    val secretTransformationMethod = MutableLiveData(TextTransformationMethod.PASSWORD)
    val isConfirmationVisible = MutableLiveData(true)

    fun onVisibilityButtonClicked() {
        val isVisible = isConfirmationVisible.value ?: false

        isConfirmationVisible.value = isVisible.not()
        secretTransformationMethod.value = if (isVisible) {
            TextTransformationMethod.HIDE_RETURNS
        } else {
            TextTransformationMethod.PASSWORD
        }
    }

    override fun createProperty(): Property {
        return Property(
            type = model.propertyType,
            name = model.propertyName,
            value = secretText.value?.trim(),
            isProtected = true
        )
    }

    override fun isDataValid(): Boolean {
        val isConfirmationVisible = isConfirmationVisible.value ?: false
        val secret = getSecretText()
        val confirmation = getConfirmationText()

        return if (isConfirmationVisible) {
            secret.isNotEmpty() && secret == confirmation
        } else {
            secret.isNotEmpty()
        }
    }

    override fun displayError() {
        val isConfirmationVisible = isConfirmationVisible.value ?: false

        if (isConfirmationVisible && getSecretText() != getConfirmationText()) {
            confirmationError.value = resourceProvider.getString(R.string.does_not_match_with_password)
        }
    }

    private fun getSecretText(): String = secretText.value?.trim() ?: EMPTY

    private fun getConfirmationText(): String = confirmationText.value?.trim() ?: EMPTY
}