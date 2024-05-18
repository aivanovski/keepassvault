package com.ivanovsky.passnotes.presentation.history

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.usecases.diff.getEntity
import com.ivanovsky.passnotes.domain.usecases.history.entity.HistoryDiffItem
import com.ivanovsky.passnotes.extensions.getOrThrow
import com.ivanovsky.passnotes.injection.GlobalInjector
import com.ivanovsky.passnotes.presentation.Screens.NoteScreen
import com.ivanovsky.passnotes.presentation.core.ThemeProvider
import com.ivanovsky.passnotes.presentation.core.compose.themeFlow
import com.ivanovsky.passnotes.presentation.core.dialog.propertyAction.PropertyAction
import com.ivanovsky.passnotes.presentation.core.event.EventProviderImpl
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import com.ivanovsky.passnotes.presentation.history.cells.viewModel.HistoryDiffCellViewModel
import com.ivanovsky.passnotes.presentation.history.cells.viewModel.HistoryHeaderCellViewModel
import com.ivanovsky.passnotes.presentation.history.factory.HistoryCellModelFactory
import com.ivanovsky.passnotes.presentation.history.factory.HistoryCellModelFactory.Companion.FIRST_VERSION_INDEX
import com.ivanovsky.passnotes.presentation.history.factory.HistoryCellViewModelFactory
import com.ivanovsky.passnotes.presentation.history.model.HistoryState
import com.ivanovsky.passnotes.presentation.note.NoteScreenArgs
import com.ivanovsky.passnotes.presentation.note.NoteSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf

class HistoryViewModel(
    private val interactor: HistoryInteractor,
    private val errorInteractor: ErrorInteractor,
    private val resourceProvider: ResourceProvider,
    private val themeProvider: ThemeProvider,
    private val modelFactory: HistoryCellModelFactory,
    private val viewModelFactory: HistoryCellViewModelFactory,
    private val router: Router,
    private val args: HistoryScreenArgs
) : ViewModel() {

    val theme = themeFlow(themeProvider)
    val state = MutableStateFlow<HistoryState>(HistoryState.Loading)
    val showPropertyActionDialog = SingleLiveEvent<Property>()
    val showSnackbarMessageEvent = SingleLiveEvent<String>()

    private var diff: List<HistoryDiffItem> = emptyList()
    private val eventProvider = EventProviderImpl()

    init {
        subscribeEvents()
    }

    override fun onCleared() {
        super.onCleared()
        eventProvider.unSubscribe(this)
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
            when (event.key()) {
                HistoryHeaderCellViewModel.ITEM_CLICK_EVENT -> {
                    val noteIndex = event.getInt(HistoryHeaderCellViewModel.ITEM_CLICK_EVENT)
                    if (noteIndex != null) {
                        onNoteClicked(noteIndex)
                    }
                }

                HistoryDiffCellViewModel.ITEM_CLICK_EVENT -> {
                    val id = event.getString(HistoryDiffCellViewModel.ITEM_CLICK_EVENT)
                    if (!id.isNullOrEmpty()) {
                        val values = id.split(":")
                            .map { it.toInt() }
                        onPropertyClicked(values[0], values[1])
                    }
                }
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
                    message = errorInteractor.processAndGetMessage(getHistoryDiff.error)
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
            val models = modelFactory.createHistoryDiffModels(diff)
            val viewModels = viewModelFactory.createCellViewModels(models, eventProvider)

            state.value = HistoryState.Data(
                viewModels = viewModels
            )
        } else {
            state.value = HistoryState.Empty
        }
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

    class Factory(private val args: HistoryScreenArgs) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GlobalInjector.get<HistoryViewModel>(
                parametersOf(args)
            ) as T
        }
    }
}