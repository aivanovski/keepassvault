package com.ivanovsky.passnotes.presentation.note_editor.cells.viewmodel

import androidx.lifecycle.MutableLiveData
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.event.Event.Companion.toEvent
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.note_editor.cells.model.SecretPropertyCellModel
import com.ivanovsky.passnotes.presentation.note_editor.view.TextTransformationMethod
import com.ivanovsky.passnotes.util.StringUtils.EMPTY

class SecretPropertyCellViewModel(
    override val model: SecretPropertyCellModel,
    private val eventProvider: EventProvider,
    private val resourceProvider: ResourceProvider
) : BaseCellViewModel(model), PropertyViewModel {

    // Somehow "MutableLiveData<String>" fixes error in 2-way data-binding
    val secretText: MutableLiveData<String> = MutableLiveData(model.value)
    val confirmationText = MutableLiveData(model.value)
    val confirmationError = MutableLiveData<String?>(null)
    val secretTransformationMethod = MutableLiveData(TextTransformationMethod.PASSWORD)
    val isConfirmationVisible = MutableLiveData(true)

    override fun createProperty(): Property {
        return Property(
            type = model.propertyType,
            name = model.propertyName,
            value = secretText.value?.trim(),
            isProtected = true
        )
    }

    override fun isDataValid(): Boolean {
        val secret = getSecretText()
        val confirmation = getConfirmationText()

        return !isConfirmationVisibleInternal() || secret == confirmation
    }

    override fun displayError() {
        val isConfirmationVisible = isConfirmationVisible.value ?: false

        if (isConfirmationVisible && getSecretText() != getConfirmationText()) {
            confirmationError.value =
                resourceProvider.getString(R.string.does_not_match_with_password)
        }
    }

    fun onVisibilityButtonClicked() {
        isConfirmationVisible.value = isConfirmationVisibleInternal().not()
        secretTransformationMethod.value = if (isConfirmationVisibleInternal()) {
            TextTransformationMethod.PASSWORD
        } else {
            TextTransformationMethod.PLANE_TEXT
        }
    }

    fun onGenerateButtonClicked() {
        eventProvider.send((GENERATE_CLICK_EVENT to model.id).toEvent())
    }

    private fun getSecretText(): String = secretText.value?.trim() ?: EMPTY

    private fun getConfirmationText(): String = confirmationText.value?.trim() ?: EMPTY

    private fun isConfirmationVisibleInternal() = isConfirmationVisible.value ?: false

    companion object {
        val GENERATE_CLICK_EVENT =
            SecretPropertyCellViewModel::class.simpleName + "_generateClickEvent"
    }
}