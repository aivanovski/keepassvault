package com.ivanovsky.passnotes.presentation.settings.app

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.domain.PermissionHelper
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.biometric.BiometricResolver
import com.ivanovsky.passnotes.domain.entity.SystemPermission
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.settings.app.AppSettingsInteractor
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import com.ivanovsky.passnotes.util.StringUtils
import java.io.File
import kotlinx.coroutines.launch

class AppSettingsViewModel(
    private val interactor: AppSettingsInteractor,
    private val biometricResolver: BiometricResolver,
    private val errorInteractor: ErrorInteractor,
    private val permissionHelper: PermissionHelper,
    private val resourceProvider: ResourceProvider,
    private val settings: Settings,
    private val router: Router
) : ViewModel() {

    val isLoading = MutableLiveData(false)
    val isSendLogFileEnabled = MutableLiveData(false)
    val isRemoveLogFilesEnabled = MutableLiveData(false)
    val isLockNotificationEnabled = MutableLiveData(false)
    val lockNotificationSummary = MutableLiveData(StringUtils.EMPTY)
    val isEnableNotificationPermissionVisible = MutableLiveData(false)
    val showErrorDialogEvent = SingleLiveEvent<String>()
    val showToastEvent = SingleLiveEvent<String>()
    val shareFileEvent = SingleLiveEvent<File>()
    val requestPermissionEvent = SingleLiveEvent<SystemPermission>()

    fun navigateBack() = router.exit()

    fun isBiometricUnlockAvailable(): Boolean {
        return biometricResolver.getInteractor()
            .isBiometricUnlockAvailable()
    }

    fun start() {
        isSendLogFileEnabled.value = settings.isFileLogEnabled
        isRemoveLogFilesEnabled.value = settings.isFileLogEnabled
        updateNotificationPermissionData()

        viewModelScope.launch {
            isLoading.value = false
        }
    }

    fun updateNotificationPermissionData() {
        val isNotificationPermissionGranted =
            permissionHelper.isPermissionGranted(SystemPermission.NOTIFICATION)

        isLockNotificationEnabled.value = isNotificationPermissionGranted
        isEnableNotificationPermissionVisible.value = !isNotificationPermissionGranted
        lockNotificationSummary.value = if (!isNotificationPermissionGranted) {
            resourceProvider.getString(R.string.pref_is_lock_notification_visible_summary)
        } else {
            StringUtils.EMPTY
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

    fun onRequestNotificationPermissionClicked() {
        requestPermissionEvent.call(SystemPermission.NOTIFICATION)
    }

    fun onNotificationPermissionResult(isGranted: Boolean) {
        updateNotificationPermissionData()
    }
}