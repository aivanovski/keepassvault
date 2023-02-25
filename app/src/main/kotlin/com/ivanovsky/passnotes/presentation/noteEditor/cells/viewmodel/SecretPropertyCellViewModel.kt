package com.ivanovsky.passnotes.presentation.noteEditor.cells.viewmodel

import androidx.annotation.DrawableRes
import androidx.lifecycle.MutableLiveData
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.event.Event.Companion.toEvent
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.core.widget.entity.TextTransformationMethod
import com.ivanovsky.passnotes.presentation.noteEditor.cells.model.SecretPropertyCellModel
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
    val visibilityIconResId = MutableLiveData(getVisibilityIconResIdInternal())

    override fun createProperty(): Property {
        return Property(
            type = model.propertyType,
            name = model.propertyName,
            value = secretText.value?.trim(),
            isProtected = true
        )
    }

    override fun isDataValid(): Boolean {
        val secret = getSecretTextInternal()
        val confirmation = getConfirmationTextInternal()

        return !isConfirmationVisibleInternal() || secret == confirmation
    }

    override fun displayError() {
        val isConfirmationVisible = isConfirmationVisible.value ?: false

        if (isConfirmationVisible && getSecretTextInternal() != getConfirmationTextInternal()) {
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
        visibilityIconResId.value = getVisibilityIconResIdInternal()
    }

    fun onGenerateButtonClicked() {
        eventProvider.send((GENERATE_CLICK_EVENT to model.id).toEvent())
    }

    private fun getSecretTextInternal(): String = secretText.value?.trim() ?: EMPTY

    private fun getConfirmationTextInternal(): String = confirmationText.value?.trim() ?: EMPTY

    private fun isConfirmationVisibleInternal() = isConfirmationVisible.value ?: false

    @DrawableRes
    private fun getVisibilityIconResIdInternal(): Int {
        val textTransformationMethod = secretTransformationMethod.value
            ?: TextTransformationMethod.PLANE_TEXT

        return when (textTransformationMethod) {
            TextTransformationMethod.PLANE_TEXT -> R.drawable.ic_visibility_on_24dp
            else -> R.drawable.ic_visibility_off_24dp
        }
    }

    companion object {
        val GENERATE_CLICK_EVENT =
            SecretPropertyCellViewModel::class.simpleName + "_generateClickEvent"
    }
}