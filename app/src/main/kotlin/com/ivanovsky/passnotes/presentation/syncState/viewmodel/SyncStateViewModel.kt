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

    val isProgressVisible = MutableLiveData(initModel.isProgressVisible)
    val isActionButtonVisible = MutableLiveData(false)
    val isDismissButtonVisible = MutableLiveData(initModel.buttonAction == ButtonAction.DISMISS)
    val isMessageVisible = MutableLiveData(initModel.message.isNotEmpty())
    val message = MutableLiveData(initModel.message)
    val messageColor = MutableLiveData(initModel.messageColor)
    val buttonText = MutableLiveData(EMPTY)
    val actionButtonTextColor = MutableLiveData(resourceProvider.getColor(R.color.primary))
    val showResolveConflictDialogEvent = SingleLiveEvent<FileDescriptor>()

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
        isProgressVisible.value = newModel.isProgressVisible
        isMessageVisible.value = newModel.message.isNotEmpty() && !newModel.isMessageDismissed
        isActionButtonVisible.value = (newModel.buttonAction == ButtonAction.RESOLVE)
        isDismissButtonVisible.value =
            (newModel.buttonAction == ButtonAction.DISMISS) && !newModel.isMessageDismissed
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

        loadData()
    }

    fun synchronize() {
        if (isCheckingStatus) {
            return
        }

        loadData()
    }

    private fun loadData() {
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
            onSyncDataLoaded(syncState, db)
        }
    }

    fun onActionButtonClicked() {
        val model = getModel()

        when (model.buttonAction) {
            ButtonAction.RESOLVE -> onResolveConflictButtonClicked()
            else -> throw IllegalStateException()
        }
    }

    fun onDismissButtonClicked() {
        val model = getModel()
        if (!model.isMessageDismissed) {
            val newModel = model.copy(isMessageDismissed = true)
            setModel(newModel)
            interactor.cache.setValue(newModel)
        }
    }

    private fun onSyncDataLoaded(syncState: SyncState, db: EncryptedDatabase) {
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
                    interactor.getSyncState(dbFile),
                    isForceMessage = hasRemoteChanges
                )
                updateCachedModel()
                isCheckingStatus = false
            }
        } else {
            showSyncState(syncState)
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
        isForceMessage: Boolean = false
    ) {
        Timber.d("showSyncState: syncStatus=%s", syncState.status)

        if (syncState.progress == SyncProgressStatus.SYNCING) {
            setModel(modelFactory.createLoadingState())
            return
        }

        setModel(modelFactory.createFromSyncState(syncState, isForceMessage))
    }

    private fun updateCachedModel() {
        interactor.cache.setValue(getModel())
    }

    private fun getModel(): SyncStateModel {
        return mutableModel as SyncStateModel
    }

    private fun ButtonAction.getActionButtonText(): String {
        return when (this) {
            ButtonAction.DISMISS -> resourceProvider.getString(R.string.dismiss)
            ButtonAction.RESOLVE -> resourceProvider.getString(R.string.resolve)
            ButtonAction.NONE -> EMPTY
        }
    }

    private fun ButtonAction.getActionButtonColor(): Int {
        return when (this) {
            ButtonAction.RESOLVE -> {
                resourceProvider.getColor(R.color.error_text)
            }

            else -> resourceProvider.getColor(R.color.primary)
        }
    }
}