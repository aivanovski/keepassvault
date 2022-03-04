package com.ivanovsky.passnotes.presentation.selectdb

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.ConflictResolutionStrategy
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.SyncConflictInfo
import com.ivanovsky.passnotes.data.entity.SyncProgressStatus
import com.ivanovsky.passnotes.data.entity.SyncState
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.selectdb.SelectDatabaseInteractor
import com.ivanovsky.passnotes.presentation.Screens.SelectDatabaseScreen
import com.ivanovsky.passnotes.presentation.core.BaseScreenViewModel
import com.ivanovsky.passnotes.presentation.core.DefaultScreenStateHandler
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.ViewModelTypes
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.selectdb.cells.viewmodel.DatabaseFileCellViewModel
import com.ivanovsky.passnotes.presentation.selectdb.cells.factory.SelectDatabaseCellModelFactory
import com.ivanovsky.passnotes.presentation.selectdb.cells.factory.SelectDatabaseCellViewModelFactory
import com.ivanovsky.passnotes.util.takeUUID
import com.ivanovsky.passnotes.util.toLinkedMap
import java.util.UUID
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.launch

class SelectDatabaseViewModel(
    private val interactor: SelectDatabaseInteractor,
    private val errorInteractor: ErrorInteractor,
    private val resourceProvider: ResourceProvider,
    private val observerBus: ObserverBus,
    private val router: Router,
    private val modelFactory: SelectDatabaseCellModelFactory,
    private val viewModelFactory: SelectDatabaseCellViewModelFactory,
    private val args: SelectDatabaseArgs
) : BaseScreenViewModel(),
    ObserverBus.SyncProgressStatusObserver {

    val screenStateHandler = DefaultScreenStateHandler()
    val screenState = MutableLiveData(ScreenState.notInitialized())

    val cellViewTypes = ViewModelTypes()
        .add(DatabaseFileCellViewModel::class, R.layout.cell_database_file)

    val showRemoveConfirmationDialogEvent = SingleLiveEvent<Pair<UUID, FileDescriptor>>()
    val showResolveConflictDialog = SingleLiveEvent<Pair<UUID, SyncConflictInfo>>()

    private var cellUidToFileMap = mutableMapOf<UUID, FileDescriptor>()
    private var cellModels = mutableMapOf<UUID, BaseCellModel>()

    init {
        subscribeToEvents()
        observerBus.register(this)
    }

    override fun onCleared() {
        super.onCleared()
        observerBus.unregister(this)
    }

    override fun onSyncProgressStatusChanged(
        fsAuthority: FSAuthority,
        uid: String,
        status: SyncProgressStatus
    ) {
        val entry = cellUidToFileMap.entries.firstOrNull { it.value.uid == uid } ?: return
        val cellUid = entry.key
        val file = entry.value

        viewModelScope.launch {
            val state = interactor.getSyncState(file)

            cellModels[cellUid] = modelFactory.createCellModel(
                file = file,
                cellUid = cellUid,
                syncState = state
            )
            setCellModels(cellModels.values.toList())
        }
    }

    fun loadData() {
        screenState.value = ScreenState.loading()

        viewModelScope.launch {
            val getFiles = interactor.getRecentlyOpenedFiles()
            if (getFiles.isSucceededOrDeferred) {
                val files = getFiles.obj

                if (files.isNotEmpty()) {
                    val cellUidsAndFiles = files
                        .map { file -> Pair(UUID.randomUUID(), file) }

                    cellUidToFileMap = cellUidsAndFiles.toLinkedMap()
                    cellModels = modelFactory.createCellModels(cellUidsAndFiles)
                    setCellModels(cellModels.values.toList())

                    screenState.value = ScreenState.data()

                    for ((cellUid, file) in cellUidsAndFiles) {
                        val syncState = suspendCoroutine<SyncState> { continuation ->
                            launch {
                                val state = interactor.getSyncState(file)
                                continuation.resumeWith(Result.success(state))
                            }
                        }

                        if (cellModels.containsKey(cellUid)) {
                            cellModels[cellUid] = modelFactory.createCellModel(
                                file = file,
                                cellUid = cellUid,
                                syncState = syncState
                            )

                            setCellModels(cellModels.values.toList())
                        }
                    }
                } else {
                    val emptyText = resourceProvider.getString(R.string.no_databases)
                    screenState.value = ScreenState.empty(emptyText)
                }
            } else {
                val message = errorInteractor.processAndGetMessage(getFiles.error)
                screenState.value = ScreenState.error(message)
            }
        }
    }

    fun onRemoveConfirmed(uid: UUID) {
        val file = cellUidToFileMap[uid] ?: return

        cellModels.remove(uid)
        cellUidToFileMap.remove(uid)

        setCellModels(cellModels.values.toList())

        viewModelScope.launch {
            val remove = interactor.removeFromUsedFiles(file)
            if (remove.isSucceeded) {
                loadData()
            } else {
                val message = errorInteractor.processAndGetMessage(remove.error)
                screenState.value = ScreenState.error(message)
            }
        }
    }

    fun onResolveConflictConfirmed(uid: UUID, resolutionStrategy: ConflictResolutionStrategy) {
        val file = cellUidToFileMap[uid] ?: return

        screenState.value = ScreenState.loading()

        viewModelScope.launch {
            val resolvedConflict = interactor.resolveConflict(file, resolutionStrategy)

            if (resolvedConflict.isSucceeded) {
                loadData()
            } else {
                screenState.value = ScreenState.dataWithError(
                    errorText = errorInteractor.processAndGetMessage(resolvedConflict.error)
                )
            }
        }
    }

    fun navigateBack() = router.exit()

    private fun subscribeToEvents() {
        eventProvider.subscribe(this) { event ->
            when {
                event.containsKey(DatabaseFileCellViewModel.CLICK_EVENT) -> {
                    onFileClicked(
                        uid = event.takeUUID(DatabaseFileCellViewModel.CLICK_EVENT)
                    )
                }
                event.containsKey(DatabaseFileCellViewModel.REMOVE_CLICK_EVENT) -> {
                    onRemoveButtonClicked(
                        uid = event.takeUUID(DatabaseFileCellViewModel.REMOVE_CLICK_EVENT)
                    )
                }
                event.containsKey(DatabaseFileCellViewModel.RESOLVE_CLICK_EVENT) -> {
                    onResolveButtonClicked(
                        uid = event.takeUUID(DatabaseFileCellViewModel.RESOLVE_CLICK_EVENT)
                    )
                }
            }
        }
    }

    private fun setCellModels(models: List<BaseCellModel>) {
        setCellElements(viewModelFactory.createCellViewModels(models, eventProvider))
    }

    private fun onFileClicked(uid: UUID) {
        val file = cellUidToFileMap[uid] ?: return

        if (file != args.selectedFile) {
            router.sendResult(SelectDatabaseScreen.RESULT_KEY, file)
            router.exit()
        } else {
            router.exit()
        }
    }

    private fun onRemoveButtonClicked(uid: UUID) {
        val file = cellUidToFileMap[uid] ?: return

        showRemoveConfirmationDialogEvent.call(Pair(uid, file))
    }

    private fun onResolveButtonClicked(uid: UUID) {
        val file = cellUidToFileMap[uid] ?: return

        screenState.value = ScreenState.loading()

        viewModelScope.launch {
            val conflict = interactor.getSyncConflictInfo(file)
            if (conflict.isSucceeded) {
                showResolveConflictDialog.call(Pair(uid, conflict.obj))
                screenState.value = ScreenState.data()
            } else {
                screenState.value = ScreenState.dataWithError(
                    errorText = errorInteractor.processAndGetMessage(conflict.error)
                )
            }
        }
    }

    companion object {
        private val TAG = SelectDatabaseViewModel::class.simpleName
    }
}