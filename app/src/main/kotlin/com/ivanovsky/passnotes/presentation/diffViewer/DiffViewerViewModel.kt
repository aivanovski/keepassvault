package com.ivanovsky.passnotes.presentation.diffViewer

import androidx.annotation.IdRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.EncryptedDatabaseElement
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.usecases.diff.entity.DiffEvent
import com.ivanovsky.passnotes.domain.usecases.diff.entity.DiffListItem
import com.ivanovsky.passnotes.presentation.core.ThemeProvider
import com.ivanovsky.passnotes.presentation.core.compose.cells.CellEventProviderImpl
import com.ivanovsky.passnotes.presentation.core.compose.cells.CellIdGenerator
import com.ivanovsky.passnotes.presentation.core.compose.cells.CellViewModel
import com.ivanovsky.passnotes.presentation.core.compose.cells.IntCellId
import com.ivanovsky.passnotes.presentation.core.compose.cells.model.ErrorPanelCellEvent
import com.ivanovsky.passnotes.presentation.core.compose.themeFlow
import com.ivanovsky.passnotes.presentation.core.dialog.confirmationDialog.ConfirmationDialogArgs
import com.ivanovsky.passnotes.presentation.core.dialog.reportErrorDialog.ReportErrorDialogArgs
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import com.ivanovsky.passnotes.presentation.core.menu.ScreenMenuItem
import com.ivanovsky.passnotes.presentation.diffViewer.cells.DiffViewerCellFactory
import com.ivanovsky.passnotes.presentation.diffViewer.cells.DiffViewerCellFactory.CellIds
import com.ivanovsky.passnotes.presentation.diffViewer.cells.viewmodel.EntryDiffCellViewModel
import com.ivanovsky.passnotes.presentation.diffViewer.cells.viewmodel.GroupDiffCellViewModel
import com.ivanovsky.passnotes.presentation.diffViewer.model.CompareData
import com.ivanovsky.passnotes.presentation.diffViewer.model.DiffViewerState
import com.ivanovsky.passnotes.presentation.diffViewer.model.MergeData
import com.ivanovsky.passnotes.util.TimeUtils.toTimestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class DiffViewerViewModel(
    private val interactor: DiffViewerInteractor,
    private val cellFactory: DiffViewerCellFactory,
    private val router: Router,
    private val resourceProvider: ResourceProvider,
    private val themeProvider: ThemeProvider,
    private val args: DiffViewerScreenArgs
) : ViewModel() {

    val theme = themeFlow(themeProvider)
    val state = MutableStateFlow<DiffViewerState>(DiffViewerState.Loading)
    val visibleMenuItems = MutableLiveData<List<DiffViewerMenuItem>>(emptyList())
    val showConfirmationDialogEvent = SingleLiveEvent<ConfirmationDialogArgs>()
    val showReportErrorDialogEvent = SingleLiveEvent<ReportErrorDialogArgs>()

    private var mergeData: MergeData? = null
    private var cellIdToDiffItemMap: Map<IntCellId, DiffListItem> = emptyMap()
    private val cellEventProvider = CellEventProviderImpl()

    init {
        subscribeToCellEvents()
    }

    override fun onCleared() {
        super.onCleared()
        unsubscribeFromCellEvents()
    }

    fun start() {
        loadData()
    }

    fun navigateBack() {
        router.exit()
    }

    fun onMergeButtonClicked() {
        showConfirmationDialogEvent.value = ConfirmationDialogArgs(
            actionId = MERGE_CONFIRMATION_ACTION_ID,
            message = resourceProvider.getString(R.string.merge_confirmation_message),
            positiveButtonText = resourceProvider.getString(R.string.merge),
            negativeButtonText = resourceProvider.getString(R.string.cancel)
        )
    }

    fun onMergeConfirmed() {
        val mode = args.mode as? DiffViewerMode.Merge ?: return
        val cellViewModels = state.value.asData()?.viewModels ?: return

        val selectedDiffEvents = getSelectedDiffEvents()

        state.value = DiffViewerState.Loading
        visibleMenuItems.value = listOf()

        viewModelScope.launch {
            interactor.applyDiff(
                key = mode.key,
                base = mode.base,
                output = mode.output,
                diff = selectedDiffEvents
            ).fold(
                ifLeft = { error -> onMergeFailed(error, cellViewModels) },
                ifRight = { router.exit() }
            )
        }
    }

    private fun getSelectedDiffEvents(): List<DiffEvent<EncryptedDatabaseElement>> {
        val viewModels = state.value.asData()?.viewModels ?: emptyList()

        val selectedCellIds = viewModels
            .mapNotNull { cellViewModel ->
                when (cellViewModel) {
                    is EntryDiffCellViewModel -> {
                        val model = cellViewModel.model
                        if (model.isChecked) model.id else null
                    }

                    is GroupDiffCellViewModel -> {
                        val model = cellViewModel.model
                        if (model.isChecked) model.id else null
                    }

                    else -> null
                }
            }

        val selectedDiffEvents = selectedCellIds
            .mapNotNull { cellId -> cellIdToDiffItemMap[cellId]?.getEvents() }
            .flatten()

        return selectedDiffEvents
    }

    private fun subscribeToCellEvents() {
        cellEventProvider.subscribe(this) { event ->
            when (event) {
                is ErrorPanelCellEvent.OnCloseButtonClick ->
                    onCloseErrorPanelButtonClicked()

                is ErrorPanelCellEvent.OnReportButtonClick ->
                    onReportErrorButtonClicked(event.error)
            }
        }
    }

    private fun unsubscribeFromCellEvents() {
        cellEventProvider.clear()
    }

    private fun onCloseErrorPanelButtonClicked() {
        val prevState = state.value.asData() ?: return

        state.value = DiffViewerState.Data(
            viewModels = prevState.viewModels
        )
    }

    private fun onReportErrorButtonClicked(error: OperationError) {
        showReportErrorDialogEvent.value = ReportErrorDialogArgs(error)
    }

    private fun loadData() {
        visibleMenuItems.value = emptyList()

        when (args.mode) {
            is DiffViewerMode.Compare -> loadCompareDiff(args.mode)
            is DiffViewerMode.Merge -> loadMergeDiff(args.mode)
        }
    }

    private fun loadCompareDiff(mode: DiffViewerMode.Compare) {
        state.value = DiffViewerState.Loading

        viewModelScope.launch {
            interactor.loadCompareData(mode)
                .fold(
                    ifLeft = { error -> setErrorState(error) },
                    ifRight = { data -> onCompareDataLoaded(data) }
                )
        }
    }

    private fun loadMergeDiff(mode: DiffViewerMode.Merge) {
        state.value = DiffViewerState.Loading

        viewModelScope.launch {
            interactor.loadMergeData(mode)
                .fold(
                    ifLeft = { error -> setErrorState(error) },
                    ifRight = { data -> onMergeDataLoaded(mode, data) }
                )
        }
    }

    private fun setErrorState(error: OperationError) {
        Timber.e(error.throwable)

        state.value = createErrorState(error)
    }

    private fun onMergeFailed(
        error: OperationError,
        cellViewModels: List<CellViewModel>
    ) {
        state.value = DiffViewerState.Data(
            viewModels = cellViewModels,
            errorCellViewModel = cellFactory.createErrorPanelCell(
                error = error,
                isErrorState = false,
                eventProvider = cellEventProvider
            )
        )
        visibleMenuItems.value = listOf(DiffViewerMenuItem.MERGE)
    }

    private fun onCompareDataLoaded(data: CompareData) {
        val viewModels = cellFactory.createCompareCellViewModels(
            diffItems = pairDiffItemWithCellId(
                diff = data.diff,
                idGenerator = CellIdGenerator(startFrom = CellIds.LOCAL_ITEMS)
            ),
            eventProvider = cellEventProvider
        )

        state.value = DiffViewerState.Data(
            viewModels = viewModels
        )
    }

    private fun onMergeDataLoaded(
        mode: DiffViewerMode.Merge,
        data: MergeData
    ) {
        mergeData = data

        val localIdsAndItems = pairDiffItemWithCellId(
            diff = data.localDiff,
            idGenerator = CellIdGenerator(startFrom = CellIds.LOCAL_ITEMS)
        )
        val remoteIdsAndItems = pairDiffItemWithCellId(
            diff = data.remoteDiff,
            idGenerator = CellIdGenerator(startFrom = CellIds.REMOTE_ITEMS)
        )

        cellIdToDiffItemMap = (localIdsAndItems.toMap() + remoteIdsAndItems.toMap())

        val viewModels = cellFactory.createMergeCellViewModel(
            localDiffItems = localIdsAndItems,
            remoteDiffItems = remoteIdsAndItems,
            localTimestamp = mode.local.modified?.toTimestamp(),
            remoteTimestamp = mode.remote.modified?.toTimestamp(),
            eventProvider = cellEventProvider
        )

        state.value = DiffViewerState.Data(
            viewModels = viewModels
        )

        visibleMenuItems.value = listOf(DiffViewerMenuItem.MERGE)
    }

    private fun createErrorState(error: OperationError): DiffViewerState.Error {
        return DiffViewerState.Error(
            cellViewModel = cellFactory.createErrorPanelCell(
                error = error,
                isErrorState = true,
                eventProvider = cellEventProvider
            )
        )
    }

    private fun pairDiffItemWithCellId(
        diff: List<DiffListItem>,
        idGenerator: CellIdGenerator
    ): List<Pair<IntCellId, DiffListItem>> {
        return diff.map { item ->
            idGenerator.nextId() to item
        }
    }

    private fun DiffListItem.getEvents(): List<DiffEvent<EncryptedDatabaseElement>> {
        return when (this) {
            is DiffListItem.GroupItem -> listOf(event)
            is DiffListItem.NoteItem -> listOf(event)
            is DiffListItem.PropertiesItem -> events
        }
    }

    private fun DiffViewerState.asData(): DiffViewerState.Data? {
        return this as? DiffViewerState.Data
    }

    enum class DiffViewerMenuItem(@IdRes override val menuId: Int) : ScreenMenuItem {
        MERGE(R.id.menu_merge)
    }

    companion object {
        private const val MERGE_CONFIRMATION_ACTION_ID = 100
    }
}