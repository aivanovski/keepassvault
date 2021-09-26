package com.ivanovsky.passnotes.presentation.settings.database.change_password

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.settings.database.DatabaseSettingsInteractor
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import com.ivanovsky.passnotes.presentation.newdb.NewDatabaseViewModel.Companion.PASSWORD_PATTERN
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import kotlinx.coroutines.launch

class ChangePasswordDialogViewModel(
    private val interactor: DatabaseSettingsInteractor,
    private val errorInteractor: ErrorInteractor,
    private val resourceProvider: ResourceProvider
) : ViewModel() {

    val screenStateHandler = ChangePasswordScreenStateHandler()
    val screenState = MutableLiveData(ScreenState.data())

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

        screenState.value = ScreenState.loading()

        viewModelScope.launch {
            val changeResult = interactor.changePassword(password, newPassword)

            if (changeResult.isSucceededOrDeferred) {
                finishScreenEvent.call()
            } else {
                val message = errorInteractor.processAndGetMessage(changeResult.error)
                screenState.value = ScreenState.dataWithError(message)
            }
        }
    }

    fun onCancelButtonClicked() {
        finishScreenEvent.call()
    }

    private fun isFieldsValid(
        password: String,
        newPassword: String,
        confirmation: String
    ): Boolean {
        passwordError.value = when {
            password.isBlank() -> {
                resourceProvider.getString(R.string.empty_field)
            }
            !PASSWORD_PATTERN.matcher(password).matches() -> {
                resourceProvider.getString(R.string.field_contains_illegal_character)
            }
            else -> null
        }

        newPasswordError.value = when {
            newPassword.isBlank() -> {
                resourceProvider.getString(R.string.empty_field)
            }
            !PASSWORD_PATTERN.matcher(newPassword).matches() -> {
                resourceProvider.getString(R.string.field_contains_illegal_character)
            }
            else -> null
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