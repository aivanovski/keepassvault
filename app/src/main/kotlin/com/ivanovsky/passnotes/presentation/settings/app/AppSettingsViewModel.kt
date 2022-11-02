package com.ivanovsky.passnotes.presentation.settings.app

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.biometric.BiometricInteractor
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.settings.app.AppSettingsInteractor
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import kotlinx.coroutines.launch
import java.io.File

class AppSettingsViewModel(
    private val interactor: AppSettingsInteractor,
    private val biometricInteractor: BiometricInteractor,
    private val errorInteractor: ErrorInteractor,
    private val resourceProvider: ResourceProvider,
    private val settings: Settings,
    private val router: Router
) : ViewModel() {

    val isLoading = MutableLiveData(false)
    val isSendLogFileEnabled = MutableLiveData(false)
    val isRemoveLogFilesEnabled = MutableLiveData(false)
    val showErrorDialogEvent = SingleLiveEvent<String>()
    val showToastEvent = SingleLiveEvent<String>()
    val shareFileEvent = SingleLiveEvent<File>()

    fun navigateBack() = router.exit()

    fun isBiometricUnlockAvailable(): Boolean {
        return biometricInteractor.isBiometricUnlockAvailable()
    }

    fun start() {
        isSendLogFileEnabled.value = settings.isFileLogEnabled
        isRemoveLogFilesEnabled.value = settings.isFileLogEnabled

        viewModelScope.launch {
            isLoading.value = false
        }
    }

    fun onFileLogEnabledChanged(isEnabled: Boolean) {
        isSendLogFileEnabled.value = isEnabled
        isRemoveLogFilesEnabled.value = isEnabled
        interactor.reInitializeLogging()
    }

    fun onPostponedSyncEnabledChanged(isEnabled: Boolean) {
        interactor.lockDatabase()
    }

    fun onBiometricUnlockEnabledChanged(isEnabled: Boolean) {
        isLoading.value = true

        viewModelScope.launch {
            interactor.removeAllBiometricData()
            isLoading.value = false
        }
    }

    fun onSendLongFileClicked() {
        isLoading.value = true

        viewModelScope.launch {
            val getFileResult = interactor.getLogFile()

            isLoading.value = false
            if (getFileResult.isSucceeded) {
                shareFileEvent.call(getFileResult.obj)
            } else {
                val message = errorInteractor.processAndGetMessage(getFileResult.error)
                showErrorDialogEvent.call(message)
            }
        }
    }

    fun onRemoveLogFilesClicked() {
        isLoading.value = true

        viewModelScope.launch {
            val removeResult = interactor.removeAllLogFiles()

            isLoading.value = false
            if (removeResult.isSucceeded) {
                showToastEvent.call(resourceProvider.getString(R.string.successfully))
            } else {
                val message = errorInteractor.processAndGetMessage(removeResult.error)
                showErrorDialogEvent.call(message)
            }
        }
    }
}