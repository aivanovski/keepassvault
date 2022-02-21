package com.ivanovsky.passnotes.presentation.search

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.search.SearchInteractor
import com.ivanovsky.passnotes.injection.GlobalInjector
import com.ivanovsky.passnotes.presentation.ApplicationLaunchMode.AUTOFILL_AUTHORIZATION
import com.ivanovsky.passnotes.presentation.ApplicationLaunchMode.AUTOFILL_SELECTION
import com.ivanovsky.passnotes.presentation.ApplicationLaunchMode.NORMAL
import com.ivanovsky.passnotes.presentation.Screens.GroupsScreen
import com.ivanovsky.passnotes.presentation.Screens.MainSettingsScreen
import com.ivanovsky.passnotes.presentation.Screens.NoteScreen
import com.ivanovsky.passnotes.presentation.Screens.UnlockScreen
import com.ivanovsky.passnotes.presentation.autofill.model.AutofillStructure
import com.ivanovsky.passnotes.presentation.core.BaseScreenViewModel
import com.ivanovsky.passnotes.presentation.core.DefaultScreenStateHandler
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.ViewModelTypes
import com.ivanovsky.passnotes.presentation.core.binding.OnTextChangeListener
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import com.ivanovsky.passnotes.presentation.core.viewmodel.GroupCellViewModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.NoteCellViewModel
import com.ivanovsky.passnotes.presentation.groups.GroupsScreenArgs
import com.ivanovsky.passnotes.presentation.note.NoteScreenArgs
import com.ivanovsky.passnotes.presentation.search.factory.SearchCellModelFactory
import com.ivanovsky.passnotes.presentation.search.factory.SearchCellViewModelFactory
import com.ivanovsky.passnotes.presentation.unlock.UnlockScreenArgs
import com.ivanovsky.passnotes.util.toUUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf
import java.util.UUID

class SearchViewModel(
    private val interactor: SearchInteractor,
    private val errorInteractor: ErrorInteractor,
    private val resourceProvider: ResourceProvider,
    private val cellModelFactory: SearchCellModelFactory,
    private val cellViewModelFactory: SearchCellViewModelFactory,
    private val router: Router,
    private val args: SearchScreenArgs
) : BaseScreenViewModel() {

    val screenStateHandler = DefaultScreenStateHandler()
    val screenState = MutableLiveData(
        ScreenState.empty(
            resourceProvider.getString(R.string.input_text_to_start_search)
        )
    )

    val cellViewTypes = ViewModelTypes()
        .add(NoteCellViewModel::class, R.layout.cell_note)
        .add(GroupCellViewModel::class, R.layout.cell_group)

    val isMoreMenuVisible = MutableLiveData(args.appMode == NORMAL)
    val hideKeyboardEvent = SingleLiveEvent<Unit>()
    val sendAutofillResponseEvent = SingleLiveEvent<Pair<Note, AutofillStructure>>()
    val finishActivityEvent = SingleLiveEvent<Unit>()

    val searchTextListener = object : OnTextChangeListener {
        override fun onTextChanged(text: String) {
            searchData(text)
        }
    }

    private var currentSearchJob: Job? = null

    init {
        subscribeToEvents()

        if (args.appMode == AUTOFILL_AUTHORIZATION) {
            throwIncorrectLaunchMode(args.appMode)
        }
    }

    fun onBackClicked() = router.exit()

    fun onSettingsButtonClicked() = router.navigateTo(MainSettingsScreen())

    fun onLockButtonClicked() {
        interactor.lockDatabase()
        when (args.appMode) {
            AUTOFILL_SELECTION -> {
                finishActivityEvent.call()
            }
            else -> {
                router.backTo(UnlockScreen(UnlockScreenArgs(args.appMode)))
            }
        }
    }

    private fun searchData(query: String) {
        if (query.isEmpty()) {
            currentSearchJob?.apply {
                cancel()
                currentSearchJob = null
            }

            screenState.value = ScreenState.empty(
                resourceProvider.getString(R.string.input_text_to_start_search)
            )
            return
        }

        screenState.value = ScreenState.loading()

        currentSearchJob = viewModelScope.launch {
            currentSearchJob?.apply {
                cancel()
                currentSearchJob = null
            }

            delay(SEARCH_DELAY)

            val findResult = interactor.find(query)
            if (findResult.isSucceededOrDeferred) {
                val items = findResult.obj

                if (items.isNotEmpty()) {
                    val cellModels = cellModelFactory.createCellModels(findResult.obj)
                    val cellViewModels = cellViewModelFactory.createCellViewModels(
                        models = cellModels,
                        eventProvider = eventProvider
                    )
                    setCellElements(cellViewModels)
                    screenState.value = ScreenState.data()
                } else {
                    screenState.value = ScreenState.empty(
                        resourceProvider.getString(R.string.no_results)
                    )
                }
            } else {
                val message = errorInteractor.processAndGetMessage(findResult.error)
                screenState.value = ScreenState.error(message)
            }

            currentSearchJob = null
        }
    }

    private fun subscribeToEvents() {
        eventProvider.subscribe(this) { event ->
            when {
                event.containsKey(GroupCellViewModel.CLICK_EVENT) -> {
                    event.getString(GroupCellViewModel.CLICK_EVENT)?.toUUID()?.let {
                        onGroupClicked(it)
                    }
                }
                event.containsKey(NoteCellViewModel.CLICK_EVENT) -> {
                    event.getString(NoteCellViewModel.CLICK_EVENT)?.toUUID()?.let {
                        onNoteClicked(it)
                    }
                }
            }
        }
    }

    private fun onGroupClicked(groupUid: UUID) {
        hideKeyboardEvent.call()

        router.navigateTo(
            GroupsScreen(
                GroupsScreenArgs(
                    appMode = args.appMode,
                    groupUid = groupUid,
                    isCloseDatabaseOnExit = false,
                    autofillStructure = args.autofillStructure
                )
            )
        )
    }

    private fun onNoteClicked(noteUid: UUID) {
        when (args.appMode) {
            AUTOFILL_SELECTION -> {
                val structure = args.autofillStructure ?: return

                screenState.value = ScreenState.loading()

                viewModelScope.launch {
                    val getNoteResult = interactor.getNoteByUid(noteUid)
                    if (getNoteResult.isSucceededOrDeferred) {
                        val note = getNoteResult.obj
                        sendAutofillResponseEvent.call(Pair(note, structure))
                    } else {
                        val message = errorInteractor.processAndGetMessage(getNoteResult.error)
                        screenState.value = ScreenState.dataWithError(message)
                    }
                }
            }
            else -> {
                hideKeyboardEvent.call()
                router.navigateTo(
                    NoteScreen(
                        NoteScreenArgs(
                            appMode = args.appMode,
                            noteUid = noteUid,
                            autofillStructure = args.autofillStructure
                        )
                    )
                )
            }
        }
    }

    class Factory(private val args: SearchScreenArgs) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return GlobalInjector.get<SearchViewModel>(
                parametersOf(args)
            ) as T
        }
    }

    companion object {
        private const val SEARCH_DELAY = 500L
    }
}