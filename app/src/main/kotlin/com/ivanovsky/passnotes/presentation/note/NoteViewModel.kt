package com.ivanovsky.passnotes.presentation.note

import androidx.annotation.IdRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.domain.LocaleProvider
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.entity.DatabaseStatus
import com.ivanovsky.passnotes.domain.entity.PropertyFilter
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.note.NoteInteractor
import com.ivanovsky.passnotes.injection.GlobalInjector
import com.ivanovsky.passnotes.presentation.ApplicationLaunchMode
import com.ivanovsky.passnotes.presentation.Screens.MainSettingsScreen
import com.ivanovsky.passnotes.presentation.Screens.NoteEditorScreen
import com.ivanovsky.passnotes.presentation.Screens.SearchScreen
import com.ivanovsky.passnotes.presentation.Screens.UnlockScreen
import com.ivanovsky.passnotes.presentation.autofill.model.AutofillStructure
import com.ivanovsky.passnotes.presentation.core.BaseScreenViewModel
import com.ivanovsky.passnotes.presentation.core.DefaultScreenStateHandler
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.ViewModelTypes
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import com.ivanovsky.passnotes.presentation.core.factory.DatabaseStatusCellModelFactory
import com.ivanovsky.passnotes.presentation.core.menu.ScreenMenuItem
import com.ivanovsky.passnotes.presentation.core.viewmodel.DatabaseStatusCellViewModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.NotePropertyCellViewModel
import com.ivanovsky.passnotes.presentation.note.factory.NoteCellModelFactory
import com.ivanovsky.passnotes.presentation.note.factory.NoteCellViewModelFactory
import com.ivanovsky.passnotes.presentation.note_editor.NoteEditorMode
import com.ivanovsky.passnotes.presentation.note_editor.NoteEditorArgs
import com.ivanovsky.passnotes.presentation.search.SearchScreenArgs
import com.ivanovsky.passnotes.presentation.unlock.UnlockScreenArgs
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import com.ivanovsky.passnotes.util.formatAccordingLocale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.parameter.parametersOf
import java.util.*

class NoteViewModel(
    private val interactor: NoteInteractor,
    private val errorInteractor: ErrorInteractor,
    private val resourceProvider: ResourceProvider,
    private val localeProvider: LocaleProvider,
    private val observerBus: ObserverBus,
    private val cellModelFactory: NoteCellModelFactory,
    private val cellViewModelFactory: NoteCellViewModelFactory,
    private val router: Router,
    private val statusCellModelFactory: DatabaseStatusCellModelFactory,
    private val args: NoteScreenArgs
) : BaseScreenViewModel(),
    ObserverBus.NoteContentObserver,
    ObserverBus.DatabaseStatusObserver {

    val viewTypes = ViewModelTypes()
        .add(NotePropertyCellViewModel::class, R.layout.cell_note_property)

    val screenStateHandler = DefaultScreenStateHandler()
    val screenState = MutableLiveData(ScreenState.notInitialized())

    val actionBarTitle = MutableLiveData<String>()
    val modifiedText = MutableLiveData<String>()
    val visibleMenuItems = MutableLiveData<List<NoteMenuItem>>(emptyList())
    val isFabButtonVisible = MutableLiveData(false)
    val showSnackbarMessageEvent = SingleLiveEvent<String>()
    val finishActivityEvent = SingleLiveEvent<Unit>()
    val sendAutofillResponseEvent = SingleLiveEvent<Pair<Note?, AutofillStructure>>()

    val statusViewModel = MutableLiveData(
        cellViewModelFactory.createCellViewModel(
            model = statusCellModelFactory.createDefaultStatusCellModel(),
            eventProvider = eventProvider
        ) as DatabaseStatusCellViewModel
    )

    private var note: Note? = null
    private var noteUid: UUID = args.noteUid

    init {
        observerBus.register(this)
        subscribeToCellEvents()
    }

    override fun onDatabaseStatusChanged(status: DatabaseStatus) {
        updateStatusViewModel(status)
    }

    override fun onCleared() {
        super.onCleared()
        observerBus.unregister(this)
    }

    fun onFabButtonClicked() {
        val note = this.note ?: return

        router.navigateTo(
            NoteEditorScreen(
                NoteEditorArgs(
                    mode = NoteEditorMode.EDIT,
                    noteUid = note.uid,
                    title = note.title
                )
            )
        )
    }

    override fun onNoteContentChanged(groupUid: UUID, oldNoteUid: UUID, newNoteUid: UUID) {
        if (oldNoteUid == noteUid) {
            noteUid = newNoteUid

            loadData()
        }
    }

    fun onLockButtonClicked() {
        interactor.lockDatabase()
        when (args.appMode) {
            ApplicationLaunchMode.AUTOFILL_SELECTION -> {
                finishActivityEvent.call()
            }
            else -> {
                router.backTo(
                    UnlockScreen(
                        UnlockScreenArgs(
                            appMode = args.appMode,
                            autofillStructure = args.autofillStructure
                        )
                    )
                )
            }
        }
    }

    fun onSearchButtonClicked() {
        router.navigateTo(
            SearchScreen(
                SearchScreenArgs(
                    appMode = args.appMode,
                    autofillStructure = args.autofillStructure
                )
            )
        )
    }

    fun navigateBack() = router.exit()

    fun onSettingsButtonClicked() = router.navigateTo(MainSettingsScreen())

    fun onSelectButtonClicked() {
        val autofillStructure = args.autofillStructure ?: return

        if (args.appMode != ApplicationLaunchMode.AUTOFILL_SELECTION) {
            return
        }

        val note = this.note
        if (note == null) {
            sendAutofillResponseEvent.call(Pair(null, autofillStructure))
            return
        }

        screenState.value = ScreenState.loading()

        viewModelScope.launch {
            val updateResult = interactor.updateNoteWithAutofillData(note, autofillStructure)
            if (updateResult.isSucceededOrDeferred) {
                sendAutofillResponseEvent.call(Pair(note, autofillStructure))
            } else {
               val message = errorInteractor.processAndGetMessage(updateResult.error)
               screenState.value = ScreenState.dataWithError(message)
            }
        }
    }

    fun loadData() {
        visibleMenuItems.value = getVisibleMenuItems()
        isFabButtonVisible.value = getFabButtonVisibility()

        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                interactor.getNoteByUid(noteUid)
            }

            val status = interactor.getDatabaseStatus()

            if (result.isSucceededOrDeferred) {
                val note = result.obj
                this@NoteViewModel.note = note

                actionBarTitle.value = note.title
                modifiedText.value =
                    note.modified.formatAccordingLocale(localeProvider.getSystemLocale())

                val filter = PropertyFilter.Builder()
                    .visible()
                    .notEmpty()
                    .sortedByType()
                    .build()

                val models = cellModelFactory.createCellModels(filter.apply(note.properties))

                setCellElements(cellViewModelFactory.createCellViewModels(models, eventProvider))

                if (status.isSucceededOrDeferred) {
                    updateStatusViewModel(status.obj)
                }

                screenState.value = ScreenState.data()
                visibleMenuItems.value = getVisibleMenuItems()
            } else {
                val message = errorInteractor.processAndGetMessage(result.error)
                screenState.value = ScreenState.error(message)
            }
            isFabButtonVisible.value = getFabButtonVisibility()
        }
    }

    private fun subscribeToCellEvents() {
        eventProvider.subscribe(this) { event ->
            if (event.containsKey(NotePropertyCellViewModel.COPY_BUTTON_CLICK_EVENT)) {
                val text = event.getString(NotePropertyCellViewModel.COPY_BUTTON_CLICK_EVENT)
                    ?: EMPTY
                onCopyButtonClicked(text)
            }
        }
    }

    private fun onCopyButtonClicked(text: String) {
        interactor.copyToClipboardWithTimeout(text)

        val delayInSeconds = interactor.getTimeoutValueInMillis() / 1000L

        val message = resourceProvider.getString(
            R.string.copied_clipboard_will_be_cleared_in_seconds,
            delayInSeconds
        )
        showSnackbarMessageEvent.call(message)
    }

    private fun updateStatusViewModel(status: DatabaseStatus) {
        statusViewModel.value = cellViewModelFactory.createCellViewModel(
            model = statusCellModelFactory.createStatusCellModel(status),
            eventProvider = eventProvider
        ) as DatabaseStatusCellViewModel
    }

    private fun getVisibleMenuItems(): List<NoteMenuItem> {
        val screenState = this.screenState.value ?: return emptyList()

        return when {
            screenState.isDisplayingData && args.appMode == ApplicationLaunchMode.NORMAL -> {
                listOf(
                    NoteMenuItem.LOCK,
                    NoteMenuItem.SEARCH,
                    NoteMenuItem.SETTINGS
                )
            }
            screenState.isDisplayingData && args.appMode == ApplicationLaunchMode.AUTOFILL_SELECTION -> {
                listOf(
                    NoteMenuItem.SELECT,
                    NoteMenuItem.LOCK,
                    NoteMenuItem.SEARCH
                )
            }
            else -> emptyList()
        }
    }

    private fun getFabButtonVisibility(): Boolean {
        val screenState = this.screenState.value ?: return false

        return screenState.isDisplayingData &&
            args.appMode == ApplicationLaunchMode.NORMAL
    }

    class Factory(private val args: NoteScreenArgs) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return GlobalInjector.get<NoteViewModel>(
                parametersOf(args)
            ) as T
        }
    }

    enum class NoteMenuItem(@IdRes override val menuId: Int) : ScreenMenuItem {
        SELECT(R.id.menu_select),
        LOCK(R.id.menu_lock),
        SEARCH(R.id.menu_search),
        SETTINGS(R.id.menu_settings)
    }
}