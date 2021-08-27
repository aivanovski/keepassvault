package com.ivanovsky.passnotes.presentation.search

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.search.SearchInteractor
import com.ivanovsky.passnotes.presentation.Screens.GroupsScreen
import com.ivanovsky.passnotes.presentation.Screens.MainSettingsScreen
import com.ivanovsky.passnotes.presentation.Screens.NoteScreen
import com.ivanovsky.passnotes.presentation.Screens.UnlockScreen
import com.ivanovsky.passnotes.presentation.core.BaseScreenViewModel
import com.ivanovsky.passnotes.presentation.core.DefaultScreenStateHandler
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.ViewModelTypes
import com.ivanovsky.passnotes.presentation.core.binding.OnTextChangeListener
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import com.ivanovsky.passnotes.presentation.core.viewmodel.GroupCellViewModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.NoteCellViewModel
import com.ivanovsky.passnotes.presentation.groups.GroupsArgs
import com.ivanovsky.passnotes.presentation.search.factory.SearchCellModelFactory
import com.ivanovsky.passnotes.presentation.search.factory.SearchCellViewModelFactory
import com.ivanovsky.passnotes.util.toUUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

class SearchViewModel(
    private val interactor: SearchInteractor,
    private val errorInteractor: ErrorInteractor,
    private val resourceProvider: ResourceProvider,
    private val cellModelFactory: SearchCellModelFactory,
    private val cellViewModelFactory: SearchCellViewModelFactory,
    private val router: Router
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

    val hideKeyboardEvent = SingleLiveEvent<Unit>()
    val searchTextListener = object : OnTextChangeListener {
        override fun onTextChanged(text: String) {
            searchData(text)
        }
    }

    private var currentSearchJob: Job? = null

    init {
        subscribeToEvents()
    }

    fun navigateBack() = router.exit()

    fun onSettingsButtonClicked() = router.navigateTo(MainSettingsScreen())

    fun onLockButtonClicked() {
        interactor.lockDatabase()
        router.backTo(UnlockScreen())
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
                GroupsArgs(
                    groupUid = groupUid,
                    isCloseDatabaseOnExit = false
                )
            )
        )
    }

    private fun onNoteClicked(noteUid: UUID) {
        hideKeyboardEvent.call()

        router.navigateTo(NoteScreen(noteUid))
    }

    companion object {
        private const val SEARCH_DELAY = 500L
    }
}