package com.ivanovsky.passnotes.presentation.search

import androidx.annotation.IdRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.EncryptedDatabaseEntry
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.repository.settings.OnSettingsChangeListener
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl
import com.ivanovsky.passnotes.domain.DatabaseLockInteractor
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
import com.ivanovsky.passnotes.presentation.core.event.LockScreenLiveEvent
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import com.ivanovsky.passnotes.presentation.core.menu.ScreenMenuItem
import com.ivanovsky.passnotes.presentation.core.viewmodel.GroupCellViewModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.NoteCellViewModel
import com.ivanovsky.passnotes.presentation.groups.GroupsScreenArgs
import com.ivanovsky.passnotes.presentation.note.NoteScreenArgs
import com.ivanovsky.passnotes.presentation.search.factory.SearchCellModelFactory
import com.ivanovsky.passnotes.presentation.search.factory.SearchCellViewModelFactory
import com.ivanovsky.passnotes.presentation.unlock.UnlockScreenArgs
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import com.ivanovsky.passnotes.util.toUUID
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf

class SearchViewModel(
    private val interactor: SearchInteractor,
    private val errorInteractor: ErrorInteractor,
    lockInteractor: DatabaseLockInteractor,
    private val resourceProvider: ResourceProvider,
    private val cellModelFactory: SearchCellModelFactory,
    private val cellViewModelFactory: SearchCellViewModelFactory,
    private val observerBus: ObserverBus,
    private val settings: Settings,
    private val router: Router,
    private val args: SearchScreenArgs
) : BaseScreenViewModel(),
    ObserverBus.NoteDataSetChanged,
    ObserverBus.GroupDataSetObserver,
    OnSettingsChangeListener {

    val screenStateHandler = DefaultScreenStateHandler()
    val screenState = MutableLiveData(ScreenState.loading())

    val cellViewTypes = ViewModelTypes()
        .add(NoteCellViewModel::class, R.layout.cell_note)
        .add(GroupCellViewModel::class, R.layout.cell_group)

    val query = MutableLiveData(EMPTY)
    val visibleMenuItems = MutableLiveData(getVisibleMenuItems())
    val isKeyboardVisibleEvent = SingleLiveEvent<Boolean>()
    val sendAutofillResponseEvent = SingleLiveEvent<Pair<Note, AutofillStructure>>()
    val finishActivityEvent = SingleLiveEvent<Unit>()
    val showAddAutofillDataDialog = SingleLiveEvent<Note>()
    val lockScreenEvent = LockScreenLiveEvent(observerBus, lockInteractor)
    val showSortAndViewDialogEvent = SingleLiveEvent<Unit>()
    val savedScrollPosition = MutableLiveData(RecyclerView.NO_POSITION)

    private var data: List<EncryptedDatabaseEntry>? = null
    private var filteredData: List<EncryptedDatabaseEntry>? = null
    private var currentSearchJob: Job? = null
    private var currentQuery: String? = null

    init {
        observerBus.register(this)
        settings.register(this)
        subscribeToEvents()
        if (args.appMode == AUTOFILL_AUTHORIZATION) {
            throwIncorrectLaunchMode(args.appMode)
        }
        query.observeForever { query ->
            searchData(query)
        }
    }

    override fun onCleared() {
        super.onCleared()
        observerBus.unregister(this)
        settings.unregister(this)
    }

    override fun onNoteDataSetChanged(groupUid: UUID) {
        loadData()
    }

    override fun onGroupDataSetChanged() {
        loadData()
    }

    override fun onSettingsChanged(pref: SettingsImpl.Pref) {
        if (pref == SettingsImpl.Pref.SEARCH_TYPE ||
            pref == SettingsImpl.Pref.SORT_TYPE ||
            pref == SettingsImpl.Pref.SORT_DIRECTION ||
            pref == SettingsImpl.Pref.IS_GROUPS_AT_START_ENABLED
        ) {
            filteredData = null
            loadData()
        }
    }

    fun onScreenCreated() {
        isKeyboardVisibleEvent.value = true
        loadData()
    }

    fun onBackClicked() {
        isKeyboardVisibleEvent.value = false
        router.exit()
    }

    fun onSettingsButtonClicked() {
        isKeyboardVisibleEvent.value = false
        router.navigateTo(MainSettingsScreen())
    }

    fun onLockButtonClicked() {
        isKeyboardVisibleEvent.value = false
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

    fun onSortAndViewButtonClicked() {
        showSortAndViewDialogEvent.call()
    }

    fun onAddAutofillDataConfirmed(note: Note) {
        val structure = args.autofillStructure ?: return

        setScreenState(ScreenState.loading())

        viewModelScope.launch {
            val updateNoteResult = interactor.updateNoteWithAutofillData(note, structure)
            if (updateNoteResult.isFailed) {
                setScreenState(
                    ScreenState.dataWithError(
                        errorText = errorInteractor.processAndGetMessage(updateNoteResult.error)
                    )
                )
                return@launch
            }

            sendAutofillResponseEvent.call(Pair(note, structure))
        }
    }

    fun onAddAutofillDataDenied(note: Note) {
        val structure = args.autofillStructure ?: return

        sendAutofillResponseEvent.call(Pair(note, structure))
    }

    private fun loadData() {
        setScreenState(ScreenState.loading())

        viewModelScope.launch {
            val getAllDataResult = interactor.loadAllData(
                isRespectAutotypeProperty = (args.appMode == AUTOFILL_SELECTION)
            )
            if (getAllDataResult.isFailed) {
                setScreenState(
                    ScreenState.error(
                        errorText = errorInteractor.processAndGetMessage(getAllDataResult.error)
                    )
                )
                return@launch
            }

            val allData = getAllDataResult.obj
            val currentQuery = currentQuery

            data = allData

            if (currentQuery != null) {
                searchData(currentQuery)
            } else {
                filteredData = allData
                showItems(allData)
            }
        }
    }

    private fun searchData(query: String) {
        val data = data

        if (data == null || (currentQuery == query && filteredData != null)) {
            filteredData?.let {
                showItems(it)
            }
            return
        }

        savedScrollPosition.value = RecyclerView.NO_POSITION
        currentQuery = query

        setScreenState(ScreenState.loading())

        currentSearchJob = viewModelScope.launch {
            currentSearchJob?.apply {
                cancel()
                currentSearchJob = null
            }

            delay(SEARCH_DELAY)

            val items = interactor.filter(data, query)
            filteredData = items
            showItems(items)

            currentSearchJob = null
        }
    }

    private fun showItems(items: List<EncryptedDatabaseEntry>) {
        if (items.isNotEmpty()) {
            val cellModels = cellModelFactory.createCellModels(items)
            val cellViewModels = cellViewModelFactory.createCellViewModels(
                models = cellModels,
                eventProvider = eventProvider
            )
            setCellElements(cellViewModels)
            setScreenState(ScreenState.data())
        } else {
            setScreenState(
                ScreenState.empty(
                    emptyText = resourceProvider.getString(R.string.no_results)
                )
            )
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
        isKeyboardVisibleEvent.value = false

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
        isKeyboardVisibleEvent.value = false

        when (args.appMode) {
            AUTOFILL_SELECTION -> {
                setScreenState(ScreenState.loading())

                val structure = args.autofillStructure ?: return

                viewModelScope.launch {
                    val getNoteResult = interactor.getNoteByUid(noteUid)
                    if (getNoteResult.isFailed) {
                        setScreenState(
                            ScreenState.dataWithError(
                                errorText = errorInteractor.processAndGetMessage(
                                    getNoteResult.error
                                )
                            )
                        )
                        return@launch
                    }

                    val note = getNoteResult.obj
                    if (interactor.shouldUpdateNoteAutofillData(note, structure)) {
                        showAddAutofillDataDialog.call(note)
                    } else {
                        sendAutofillResponseEvent.call(Pair(note, structure))
                    }

                    setScreenState(ScreenState.data())
                }
            }
            else -> {
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

    private fun setScreenState(state: ScreenState) {
        screenState.value = state
    }

    private fun getVisibleMenuItems(): List<SearchMenuItem> {
        return when (args.appMode) {
            NORMAL -> SearchMenuItem.values().toList()
            AUTOFILL_SELECTION -> listOf(SearchMenuItem.VIEW_MODE)
            else -> emptyList()
        }
    }

    enum class SearchMenuItem(@IdRes override val menuId: Int) : ScreenMenuItem {
        LOCK(R.id.menu_lock),
        VIEW_MODE(R.id.menu_sort_and_view),
        SETTINGS(R.id.menu_settings)
    }

    class Factory(private val args: SearchScreenArgs) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GlobalInjector.get<SearchViewModel>(
                parametersOf(args)
            ) as T
        }
    }

    companion object {
        private const val SEARCH_DELAY = 300L
    }
}