package com.ivanovsky.passnotes.presentation.settings.database

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseConfig
import com.ivanovsky.passnotes.domain.DatabaseLockInteractor
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.interactor.settings.database.DatabaseSettingsInteractor
import com.ivanovsky.passnotes.extensions.formatReadableMessage
import com.ivanovsky.passnotes.presentation.core.event.LockScreenLiveEvent
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import kotlinx.coroutines.launch

class DatabaseSettingsViewModel(
    private val interactor: DatabaseSettingsInteractor,
    private val resourceProvider: ResourceProvider,
    lockInteractor: DatabaseLockInteractor,
    observerBus: ObserverBus
) : ViewModel() {

    val isLoading = MutableLiveData(true)
    val isRecycleBinEnabled = MutableLiveData(false)
    val showErrorDialogEvent = SingleLiveEvent<String>()
    val lockScreenEvent = LockScreenLiveEvent(observerBus, lockInteractor)

    private var config: EncryptedDatabaseConfig? = null

    fun start() {
        isLoading.value = true

        viewModelScope.launch {
            val getConfig = interactor.getDbConfig()
            if (getConfig.isSucceededOrDeferred) {
                config = getConfig.obj

                isRecycleBinEnabled.value = config?.isRecycleBinEnabled ?: false
                isLoading.value = false
            } else {
                val message = getConfig.error.formatReadableMessage(resourceProvider)
                showErrorDialogEvent.call(message)
            }
        }
    }

    fun onRecycleBinEnabledChanged(isEnabled: Boolean) {
        val config = config ?: return

        isLoading.value = true

        viewModelScope.launch {
            val applyConfig = interactor.applyDbConfig(
                config = config.toMutableConfig().copy(
                    isRecycleBinEnabled = isEnabled
                )
            )

            if (applyConfig.isSucceededOrDeferred) {
                isRecycleBinEnabled.value = isEnabled
                isLoading.value = false
            } else {
                isRecycleBinEnabled.value = isRecycleBinEnabled.value
                val message = applyConfig.error.formatReadableMessage(resourceProvider)
                showErrorDialogEvent.call(message)
            }
        }
    }
}