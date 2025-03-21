package com.ivanovsky.passnotes.presentation.settings.database.changePassword

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.interactor.settings.database.DatabaseSettingsInteractor
import com.ivanovsky.passnotes.presentation.core.BaseScreenViewModel
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import kotlinx.coroutines.launch

class ChangePasswordDialogViewModel(
    private val interactor: DatabaseSettingsInteractor,
    private val resourceProvider: ResourceProvider
) : BaseScreenViewModel(
    initialState = ScreenState.data()
) {

    val visibilityHandler = ChangePasswordScreenVisibilityHandler()
    val password = MutableLiveData(EMPTY)
    val newPassword = MutableLiveData(EMPTY)
    val confirmation = MutableLiveData(EMPTY)
    val passwordError = MutableLiveData<String>(null)
    val newPasswordError = MutableLiveData<String>(null)
    val confirmationError = MutableLiveData<String>(null)
    val finishScreenEvent = SingleLiveEvent<Unit>()

    fun onApplyButtonClicked() {
        val password = this.password.value ?: return
        val newPassword = this.newPassword.value ?: return
        val confirmation = this.confirmation.value ?: return

        if (!isFieldsValid(password, newPassword, confirmation)) {
            return
        }

        setScreenState(ScreenState.loading())

        viewModelScope.launch {
            val changeResult = interactor.changePassword(password, newPassword)

            if (changeResult.isSucceededOrDeferred) {
                finishScreenEvent.call(Unit)
            } else {
                setErrorPanelState(changeResult.error)
            }
        }
    }

    fun onCancelButtonClicked() {
        finishScreenEvent.call(Unit)
    }

    private fun isFieldsValid(
        password: String,
        newPassword: String,
        confirmation: String
    ): Boolean {
        passwordError.value = if (password.isBlank()) {
            resourceProvider.getString(R.string.empty_field)
        } else {
            null
        }

        newPasswordError.value = if (newPassword.isBlank()) {
            resourceProvider.getString(R.string.empty_field)
        } else {
            null
        }

        confirmationError.value = when {
            confirmation.isBlank() -> {
                resourceProvider.getString(R.string.empty_field)
            }
            newPassword != confirmation -> {
                resourceProvider.getString(R.string.this_field_should_match_password)
            }
            else -> null
        }

        return passwordError.value == null &&
            newPasswordError.value == null &&
            confirmationError.value == null
    }
}