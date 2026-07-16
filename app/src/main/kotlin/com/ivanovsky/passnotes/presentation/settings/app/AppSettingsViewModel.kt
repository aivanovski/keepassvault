package com.ivanovsky.passnotes.presentation.settings.app

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.repository.keepass.KeepassImplementation
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.domain.PermissionHelper
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.biometric.BiometricResolver
import com.ivanovsky.passnotes.domain.entity.SystemPermission
import com.ivanovsky.passnotes.domain.interactor.settings.app.AppSettingsInteractor
import com.ivanovsky.passnotes.domain.loggingAndReporting.CrashReporterInteractor.CrashReporterAvailability
import com.ivanovsky.passnotes.extensions.formatReadableMessage
import com.ivanovsky.passnotes.presentation.core.dialog.selectorDialog.SelectorDialogArgs
import com.ivanovsky.passnotes.presentation.core.dialog.selectorDialog.model.SelectorDialogItem
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import com.ivanovsky.passnotes.util.StringUtils
import java.io.File
import kotlinx.coroutines.launch

class AppSettingsViewModel(
    private val interactor: AppSettingsInteractor,
    private val biometricResolver: BiometricResolver,
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
    val showKeepassImplementationDialogEvent = SingleLiveEvent<SelectorDialogArgs>()
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
        settings.isFileLogEnabled = isEnabled
        interactor.reInitializeLogging()
    }

    fun isCrashReportingAvailable(): Boolean =
        interactor.getCrashReportingAvailability() == CrashReporterAvailability.AVAILABLE

    fun onCrashReportingEnabledChanged(isEnabled: Boolean) {
        settings.isCrashReportingEnabled = isEnabled
        interactor.setCrashReportingEnabled(isEnabled)
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

    fun onKeepassImplementationClicked() {
        val items = KeepassImplementation.entries
            .map { implementation ->
                val title = when (implementation) {
                    KeepassImplementation.KOTPASS -> "kotpass"
                    KeepassImplementation.KEEPASS_RS -> "keepass-rs"
                }

                val description = when (implementation) {
                    KeepassImplementation.KOTPASS -> "https://github.com/keemobile/kotpass"
                    KeepassImplementation.KEEPASS_RS -> "https://github.com/sseemayer/keepass-rs"
                }

                SelectorDialogItem(
                    title = title,
                    description = description
                )
            }

        val currImplementation = settings.keepassImplementation

        val selectedIndex = KeepassImplementation.entries
            .indexOfFirst { implementation -> implementation == currImplementation }
            .takeIf { index -> index != -1 }

        val args = SelectorDialogArgs(
            title = resourceProvider.getString(R.string.pref_keepass_implementation_title),
            description = resourceProvider.getString(R.string.pref_keepass_implementation_summary),
            items = items,
            selectedItemIndex = selectedIndex
        )

        showKeepassImplementationDialogEvent.value = args
    }

    fun onKeepassImplementationSelected(index: Int) {
        val implementation = KeepassImplementation.entries.getOrNull(index) ?: return

        settings.keepassImplementation = implementation
        interactor.lockDatabase()
    }

    fun onSendLongFileClicked() {
        isLoading.value = true

        viewModelScope.launch {
            val getFileResult = interactor.getLogFile()

            isLoading.value = false
            if (getFileResult.isSucceeded) {
                shareFileEvent.call(getFileResult.obj)
            } else {
                val message = getFileResult.error.formatReadableMessage(resourceProvider)
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
                val message = removeResult.error.formatReadableMessage(resourceProvider)
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

    fun getCrashReportingSummary(): String {
        val summaryResId = when (interactor.getCrashReportingAvailability()) {
            CrashReporterAvailability.UNAVAILABLE ->
                R.string.pref_is_crash_reporting_unavailable_summary

            CrashReporterAvailability.AVAILABLE ->
                R.string.pref_is_crash_reporting_available_summary

            CrashReporterAvailability.NOT_CONFIGURED ->
                R.string.pref_is_crash_reporting_not_configured_summary
        }

        return resourceProvider.getString(summaryResId)
    }
}