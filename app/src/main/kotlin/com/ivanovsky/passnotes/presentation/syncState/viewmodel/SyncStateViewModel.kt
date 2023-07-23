package com.ivanovsky.passnotes.presentation.syncState.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.SyncProgressStatus
import com.ivanovsky.passnotes.data.entity.SyncState
import com.ivanovsky.passnotes.data.entity.SyncStatus
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabase
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.interactor.syncState.SyncStateCache
import com.ivanovsky.passnotes.domain.interactor.syncState.SyncStateInteractor
import com.ivanovsky.passnotes.extensions.getOrThrow
import com.ivanovsky.passnotes.presentation.core.BaseMutableCellViewModel
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import com.ivanovsky.passnotes.presentation.syncState.factory.SyncStateCellModelFactory
import com.ivanovsky.passnotes.presentation.syncState.model.ButtonAction
import com.ivanovsky.passnotes.presentation.syncState.model.SyncStateModel
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import kotlinx.coroutines.launch
import timber.log.Timber

class SyncStateViewModel(
    private val interactor: SyncStateInteractor,
    private val modelFactory: SyncStateCellModelFactory,
    private val resourceProvider: ResourceProvider,
    private val observerBus: ObserverBus,
    initModel: SyncStateModel
) : BaseMutableCellViewModel<SyncStateModel>(initModel),
    ObserverBus.DatabaseSyncStateObserver,
    SyncStateCache.OnSyncStateChangeListener {

    val isVisible = MutableLiveData(initModel.isVisibleInternal())
    val isSyncIconVisible = MutableLiveData(initModel.isSyncIconVisible)
    val isActionButtonVisible = MutableLiveData(initModel.isButtonVisibleInternal())
    val isMessageVisible = MutableLiveData(initModel.isMessageVisibleInternal())
    val message = MutableLiveData(initModel.message)
    val messageColor = MutableLiveData(initModel.messageColor)
    val buttonText = MutableLiveData(EMPTY)
    val actionButtonTextColor = MutableLiveData(resourceProvider.getColor(R.color.primary))
    val showResolveConflictDialogEvent = SingleLiveEvent<FileDescriptor>()
    val showMessageDialogEvent = SingleLiveEvent<String>()

    private lateinit var dbFile: FileDescriptor
    private var isCheckingStatus = false
    private var lastSyncState: SyncState? = null

    override fun onAttach() {
        dbFile = interactor.getDatabase().getOrThrow().file
        interactor.cache.subscribe(this)
        observerBus.register(this)
    }

    override fun onDetach() {
        interactor.cache.unsubscribe(this)
        observerBus.unregister(this)
    }

    override fun onDatabaseSyncStateChanges(syncState: SyncState) {
        val isSyncProgressChanged = (
            lastSyncState != null &&
                lastSyncState?.progress != syncState.progress
            )

        Timber.d(
            "onDatabaseSyncStateChanges: isCheckingStatus=%s, isSyncProgressChanged=%s",
            isCheckingStatus,
            isSyncProgressChanged
        )

        if (isCheckingStatus && !isSyncProgressChanged) {
            return
        }

        showSyncState(syncState)
        updateCachedModel()
    }

    override fun setModel(newModel: SyncStateModel) {
        super.setModel(newModel)
        isVisible.value = newModel.isVisibleInternal()
        isSyncIconVisible.value = newModel.isSyncIconVisible
        isMessageVisible.value = newModel.isMessageVisibleInternal()
        isActionButtonVisible.value = newModel.isButtonVisibleInternal()
        buttonText.value = newModel.buttonAction.getActionButtonText()
        actionButtonTextColor.value = newModel.buttonAction.getActionButtonColor()
        message.value = newModel.message
        messageColor.value = newModel.messageColor
    }

    override fun onSyncStateCacheChanged(model: SyncStateModel) {
        Timber.d("onSyncStateCacheChanged: model=%s, isCheckingStatus=%s", model, isCheckingStatus)

        if (isCheckingStatus || model == getModel()) {
            return
        }

        setModel(model)
    }

    fun start() {
        val cachedModel = interactor.cache.getValue()
        Timber.d("start: cachedModel=%s", cachedModel)

        if (cachedModel != null) {
            setModel(cachedModel)
            return
        }

        loadData(isForceShowMessage = false)
    }

    fun synchronize() {
        if (isCheckingStatus) {
            return
        }

        loadData(isForceShowMessage = true)
    }

    private fun loadData(isForceShowMessage: Boolean) {
        setModel(modelFactory.createLoadingState())

        viewModelScope.launch {
            isCheckingStatus = true

            val getDatabaseResult = interactor.getDatabase()
            if (getDatabaseResult.isFailed) {
                return@launch
            }

            val db = getDatabaseResult.getOrThrow()
            dbFile = db.file

            val syncState = interactor.getSyncState(db.file)
            onSyncDataLoaded(syncState, db, isForceShowMessage)
        }
    }

    fun onActionButtonClicked() {
        val model = getModel()

        when (model.buttonAction) {
            ButtonAction.RESOLVE -> onResolveConflictButtonClicked()
            ButtonAction.DISMISS -> onDismissButtonClicked()
            ButtonAction.DETAILS -> onDetailsButtonClicked()
            else -> throw IllegalStateException()
        }
    }

    private fun onDismissButtonClicked() {
        val model = getModel()
        if (!model.isMessageDismissed) {
            val newModel = model.copy(isMessageDismissed = true)
            setModel(newModel)
            interactor.cache.setValue(newModel)
        }
    }

    private fun onDetailsButtonClicked() {
        val message = getModel().detailsMessage
        if (message.isEmpty()) {
            return
        }

        showMessageDialogEvent.value = message
    }

    private fun onSyncDataLoaded(
        syncState: SyncState,
        db: EncryptedDatabase,
        isForceShowMessage: Boolean
    ) {
        val hasRemoteChanges = (syncState.status == SyncStatus.REMOTE_CHANGES)
        val hasLocalChanges = (syncState.status == SyncStatus.LOCAL_CHANGES)
        val isSyncInIdle = (syncState.progress == SyncProgressStatus.IDLE)
        val isDatabaseWriteable = db.fsOptions.isWriteEnabled
        val isShouldSync = (hasRemoteChanges || (hasLocalChanges && isDatabaseWriteable))

        Timber.d(
            "onSyncStateLoaded: syncState=%s, isShouldSync=%s, fileUid=%s",
            syncState,
            isShouldSync,
            dbFile.uid
        )

        if (isShouldSync && isSyncInIdle) {
            lastSyncState = syncState

            viewModelScope.launch {
                interactor.processSync(dbFile)

                showSyncState(
                    syncState = interactor.getSyncState(dbFile),
                    isForceShowMessage = (hasRemoteChanges || isForceShowMessage)
                )
                updateCachedModel()
                isCheckingStatus = false
            }
        } else {
            showSyncState(
                syncState = syncState,
                isForceShowMessage = isForceShowMessage
            )
            updateCachedModel()
            isCheckingStatus = false
            lastSyncState = syncState
        }
    }

    private fun onResolveConflictButtonClicked() {
        showResolveConflictDialogEvent.call(dbFile)
    }

    private fun showSyncState(
        syncState: SyncState,
        isForceShowMessage: Boolean = false
    ) {
        Timber.d("showSyncState: syncState=%s, isForceMessage=%s", syncState, isForceShowMessage)

        if (syncState.progress == SyncProgressStatus.SYNCING) {
            setModel(modelFactory.createLoadingState())
            return
        }

        setModel(modelFactory.createFromSyncState(syncState, isForceShowMessage))
    }

    private fun updateCachedModel() {
        interactor.cache.setValue(getModel())
    }

    private fun getModel(): SyncStateModel {
        return mutableModel as SyncStateModel
    }

    private fun ButtonAction.getActionButtonText(): String {
        return when (this) {
            ButtonAction.DETAILS -> resourceProvider.getString(R.string.details)
            ButtonAction.DISMISS -> resourceProvider.getString(R.string.dismiss)
            ButtonAction.RESOLVE -> resourceProvider.getString(R.string.resolve)
            ButtonAction.NONE -> EMPTY
        }
    }

    private fun ButtonAction.getActionButtonColor(): Int {
        return when (this) {
            ButtonAction.RESOLVE, ButtonAction.DETAILS -> {
                resourceProvider.getColor(R.color.error_text)
            }

            else -> {
                resourceProvider.getColor(R.color.primary)
            }
        }
    }

    private fun SyncStateModel.isMessageVisibleInternal(): Boolean {
        return message.isNotEmpty() && !isMessageDismissed
    }

    private fun SyncStateModel.isButtonVisibleInternal(): Boolean {
        return buttonAction != ButtonAction.NONE && !isMessageDismissed
    }

    private fun SyncStateModel.isVisibleInternal(): Boolean {
        return this.isSyncIconVisible ||
            isButtonVisibleInternal() ||
            isMessageVisibleInternal()
    }
}