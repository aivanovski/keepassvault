package com.ivanovsky.passnotes.presentation.history

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.usecases.diff.getEntity
import com.ivanovsky.passnotes.domain.usecases.history.entity.HistoryDiffItem
import com.ivanovsky.passnotes.extensions.getOrThrow
import com.ivanovsky.passnotes.presentation.Screens.NoteScreen
import com.ivanovsky.passnotes.presentation.core.ThemeProvider
import com.ivanovsky.passnotes.presentation.core.compose.cells.CellEventProviderImpl
import com.ivanovsky.passnotes.presentation.core.compose.cells.model.ErrorPanelCellEvent
import com.ivanovsky.passnotes.presentation.core.compose.themeFlow
import com.ivanovsky.passnotes.presentation.core.dialog.propertyAction.PropertyAction
import com.ivanovsky.passnotes.presentation.core.dialog.reportErrorDialog.ReportErrorDialogArgs
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import com.ivanovsky.passnotes.presentation.history.cells.HistoryCellFactory
import com.ivanovsky.passnotes.presentation.history.cells.HistoryCellFactory.Companion.FIRST_VERSION_INDEX
import com.ivanovsky.passnotes.presentation.history.cells.model.HistoryDiffCellEvent
import com.ivanovsky.passnotes.presentation.history.cells.model.HistoryHeaderCellEvent
import com.ivanovsky.passnotes.presentation.history.model.HistoryState
import com.ivanovsky.passnotes.presentation.note.NoteScreenArgs
import com.ivanovsky.passnotes.presentation.note.NoteSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val interactor: HistoryInteractor,
    private val resourceProvider: ResourceProvider,
    private val themeProvider: ThemeProvider,
    private val cellFactory: HistoryCellFactory,
    private val router: Router,
    private val args: HistoryScreenArgs
) : ViewModel() {

    val theme = themeFlow(themeProvider)
    val state = MutableStateFlow<HistoryState>(HistoryState.Loading)
    val showPropertyActionDialog = SingleLiveEvent<Property>()
    val showSnackbarMessageEvent = SingleLiveEvent<String>()
    val showReportErrorDialogEvent = SingleLiveEvent<ReportErrorDialogArgs>()

    private var diff: List<HistoryDiffItem> = emptyList()
    private val eventProvider = CellEventProviderImpl()

    init {
        subscribeEvents()
    }

    override fun onCleared() {
        super.onCleared()
        eventProvider.unsubscribe(this)
    }

    fun start() {
        loadData()
    }

    fun navigateBack() {
        router.exit()
    }

    fun onPropertyActionClicked(action: PropertyAction) {
        when (action) {
            is PropertyAction.CopyText -> {
                if (action.isProtected) {
                    copyProtectedText(action.text)
                } else {
                    copyText(action.text)
                }
            }

            else -> throw IllegalArgumentException()
        }
    }

    private fun subscribeEvents() {
        eventProvider.subscribe(this) { event ->
            when (event) {
                is HistoryHeaderCellEvent.OnClick -> onNoteClicked(event.itemId)
                is HistoryDiffCellEvent.OnClick -> {
                    val values = event.eventId.split(":").map { it.toInt() }
                    onPropertyClicked(values[0], values[1])
                }

                is ErrorPanelCellEvent.OnReportButtonClick -> onReportErrorButtonClicked(
                    event.error
                )
            }
        }
    }

    private fun onNoteClicked(noteIndex: Int) {
        val note = if (noteIndex != FIRST_VERSION_INDEX) {
            diff.getOrNull(noteIndex)?.newNote
        } else {
            diff.lastOrNull()?.oldNote
        }
            ?: return

        router.navigateTo(
            NoteScreen(
                NoteScreenArgs(
                    appMode = args.appMode,
                    noteSource = NoteSource.ByNote(note),
                    autofillStructure = args.autofillStructure,
                    isViewOnly = true
                )
            )
        )
    }

    private fun onPropertyClicked(
        diffItemIndex: Int,
        eventIndex: Int
    ) {
        val event = diff.getOrNull(diffItemIndex)
            ?.diffEvents
            ?.getOrNull(eventIndex)
            ?: return

        showPropertyActionDialog.call(event.getEntity())
    }

    private fun loadData() {
        state.value = HistoryState.Loading

        viewModelScope.launch {
            val getHistoryDiff = interactor.getHistoryDiff(args.noteUid)
            if (getHistoryDiff.isFailed) {
                state.value = HistoryState.Error(
                    cellViewModel = cellFactory.createErrorPanelCell(
                        error = getHistoryDiff.error,
                        eventProvider = eventProvider
                    )
                )
                return@launch
            }

            val diff = getHistoryDiff.getOrThrow()
            onDataLoaded(diff)
        }
    }

    private fun onDataLoaded(
        diff: List<HistoryDiffItem>
    ) {
        this.diff = diff

        if (diff.isNotEmpty()) {
            val viewModels = cellFactory.createHistoryDiffCellViewModels(diff, eventProvider)
            state.value = HistoryState.Data(
                viewModels = viewModels
            )
        } else {
            state.value = HistoryState.Empty
        }
    }

    private fun onReportErrorButtonClicked(error: OperationError) {
        showReportErrorDialogEvent.value = ReportErrorDialogArgs(error)
    }

    private fun copyText(text: String) {
        interactor.copyToClipboard(text, isProtected = false)
        if (Build.VERSION.SDK_INT < 33) {
            showSnackbarMessageEvent.call(resourceProvider.getString(R.string.copied))
        }
    }

    private fun copyProtectedText(text: String) {
        interactor.copyToClipboardWithTimeout(text, isProtected = true)

        val delayInSeconds = interactor.getClipboardTimeout().seconds

        val message = resourceProvider.getString(
            R.string.copied_clipboard_will_be_cleared_in_seconds,
            delayInSeconds
        )
        if (Build.VERSION.SDK_INT < 33) {
            showSnackbarMessageEvent.call(message)
        }
    }
}