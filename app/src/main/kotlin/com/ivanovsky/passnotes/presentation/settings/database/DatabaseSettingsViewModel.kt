package com.ivanovsky.passnotes.presentation.settings.database

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import arrow.core.raise.either
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseConfig
import com.ivanovsky.passnotes.domain.DatabaseLockInteractor
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.interactor.settings.database.DatabaseSettingsInteractor
import com.ivanovsky.passnotes.extensions.formatReadableMessage
import com.ivanovsky.passnotes.presentation.core.dialog.selectorDialog.SelectorDialogArgs
import com.ivanovsky.passnotes.presentation.core.dialog.selectorDialog.model.SelectorDialogItem
import com.ivanovsky.passnotes.presentation.core.event.LockScreenLiveEvent
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import com.ivanovsky.passnotes.presentation.settings.database.model.DatabaseSettingsData
import com.ivanovsky.passnotes.util.StringUtils
import kotlinx.coroutines.launch

class DatabaseSettingsViewModel(
    private val interactor: DatabaseSettingsInteractor,
    private val resourceProvider: ResourceProvider,
    lockInteractor: DatabaseLockInteractor,
    observerBus: ObserverBus
) : ViewModel() {

    val isLoading = MutableLiveData(true)
    val isRecycleBinEnabled = MutableLiveData(false)
    val recycleBinSummary = MutableLiveData("")
    val showErrorDialogEvent = SingleLiveEvent<String>()
    val showSelectRecycleBinGroupEvent = SingleLiveEvent<SelectorDialogArgs>()
    val lockScreenEvent = LockScreenLiveEvent(observerBus, lockInteractor)

    private var currentConfig: EncryptedDatabaseConfig? = null
    private var currentGroups: List<Group> = emptyList()

    fun start() {
        isLoading.value = true

        viewModelScope.launch {
            val data = interactor.loadData()
            onDataLoaded(data)
        }
    }

    fun onRecycleBinEnabledChanged(isEnabled: Boolean) {
        val config = currentConfig ?: return

        isLoading.value = true

        viewModelScope.launch {
            val data = either {
                val recycleBinUid = if (isEnabled) {
                    interactor.setupRecycleBinGroup()
                        .map { group -> group.uid }
                        .bind()
                } else {
                    null
                }

                interactor.applyDbConfig(
                    config = config.toMutableConfig().copy(
                        isRecycleBinEnabled = isEnabled,
                        recycleBinUid = recycleBinUid
                    )
                ).bind()

                interactor.loadData().bind()
            }

            onDataLoaded(data)
        }
    }

    fun onRecycleBinGroupClicked() {
        val config = currentConfig ?: return

        val selectedItemIndex = if (config.isRecycleBinEnabled) {
            currentGroups
                .indexOfFirst { group ->
                    group.uid == config.recycleBinUid
                }
                .takeIf { index -> index != -1 }
        } else {
            null
        }

        val items = currentGroups
            .map { group ->
                SelectorDialogItem(
                    title = group.title,
                    description = "UUID: ${group.uid}"
                )
            }

        showSelectRecycleBinGroupEvent.value = SelectorDialogArgs(
            title = resourceProvider.getString(R.string.pref_recycle_bin_group_title),
            description = null,
            selectedItemIndex = selectedItemIndex,
            items = items
        )
    }

    fun onRecycleBinGroupSelected(index: Int) {
        val recycleBinGroup = currentGroups.getOrNull(index) ?: return
        val config = currentConfig ?: return

        viewModelScope.launch {
            val data = either {
                interactor.applyDbConfig(
                    config = config.toMutableConfig().copy(
                        isRecycleBinEnabled = config.isRecycleBinEnabled,
                        recycleBinUid = recycleBinGroup.uid
                    )
                ).bind()

                interactor.loadData().bind()
            }

            onDataLoaded(data)
        }
    }

    private fun onDataLoaded(data: Either<OperationError, DatabaseSettingsData>) {
        data.fold(
            ifLeft = { error ->
                val message = error.formatReadableMessage(resourceProvider)
                showErrorDialogEvent.call(message)
            },
            ifRight = { (config, groups) ->
                currentConfig = config
                currentGroups = groups

                isRecycleBinEnabled.value = config.isRecycleBinEnabled
                recycleBinSummary.value = formatRecycleBinSummary(config)
                isLoading.value = false
            }
        )
    }

    private fun formatRecycleBinSummary(config: EncryptedDatabaseConfig): String {
        val group = currentGroups
            .firstOrNull { group -> group.uid == config.recycleBinUid }
            ?: return StringUtils.EMPTY

        return group.title
    }
}