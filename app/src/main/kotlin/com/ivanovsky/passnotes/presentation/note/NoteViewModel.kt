package com.ivanovsky.passnotes.presentation.note

import androidx.lifecycle.MutableLiveData
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
import com.ivanovsky.passnotes.presentation.Screens.NoteEditorScreen
import com.ivanovsky.passnotes.presentation.Screens.SearchScreen
import com.ivanovsky.passnotes.presentation.Screens.UnlockScreen
import com.ivanovsky.passnotes.presentation.core.BaseScreenViewModel
import com.ivanovsky.passnotes.presentation.core.DefaultScreenStateHandler
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.ViewModelTypes
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import com.ivanovsky.passnotes.presentation.core.factory.DatabaseStatusCellModelFactory
import com.ivanovsky.passnotes.presentation.core.viewmodel.DatabaseStatusCellViewModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.NotePropertyCellViewModel
import com.ivanovsky.passnotes.presentation.note.converter.toCellModels
import com.ivanovsky.passnotes.presentation.note_editor.LaunchMode
import com.ivanovsky.passnotes.presentation.note_editor.NoteEditorArgs
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import com.ivanovsky.passnotes.util.formatAccordingLocale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class NoteViewModel(
    private val interactor: NoteInteractor,
    private val errorInteractor: ErrorInteractor,
    private val resourceProvider: ResourceProvider,
    private val localeProvider: LocaleProvider,
    private val observerBus: ObserverBus,
    private val router: Router,
    private val statusCellModelFactory: DatabaseStatusCellModelFactory
) : BaseScreenViewModel(),
    ObserverBus.NoteContentObserver,
    ObserverBus.DatabaseStatusObserver {

    val viewTypes = ViewModelTypes()
        .add(NotePropertyCellViewModel::class, R.layout.cell_note_property)

    private val cellViewModelFactory = NoteCellFactory()

    val screenStateHandler = DefaultScreenStateHandler()
    val screenState = MutableLiveData(ScreenState.notInitialized())

    val actionBarTitle = MutableLiveData<String>()
    val modifiedText = MutableLiveData<String>()
    val showSnackbarMessageEvent = SingleLiveEvent<String>()
    val isMenuVisible = MutableLiveData(false)

    val statusViewModel = MutableLiveData(
        cellViewModelFactory.createCellViewModel(
            model = statusCellModelFactory.createDefaultStatusCellModel(),
            eventProvider = eventProvider
        ) as DatabaseStatusCellViewModel
    )

    private var note: Note? = null
    private var noteUid: UUID? = null

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

    fun start(noteUid: UUID) {
        this.noteUid = noteUid

        loadData()
    }

    fun onFabButtonClicked() {
        val note = this.note ?: return

        router.navigateTo(
            NoteEditorScreen(
                NoteEditorArgs(
                    launchMode = LaunchMode.EDIT,
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
        router.backTo(UnlockScreen())
    }

    fun onSearchButtonClicked() {
        router.navigateTo(SearchScreen())
    }

    fun navigateBack() = router.exit()

    private fun loadData() {
        val noteUid = this.noteUid ?: return

        isMenuVisible.value = false

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
                    .excludeTitle()
                    .sortedByType()
                    .build()

                val models = filter.apply(note.properties)
                    .toCellModels()

                setCellElements(cellViewModelFactory.createCellViewModels(models, eventProvider))

                if (status.isSucceededOrDeferred) {
                    updateStatusViewModel(status.obj)
                }

                isMenuVisible.value = true
                screenState.value = ScreenState.data()
            } else {
                val message = errorInteractor.processAndGetMessage(result.error)
                screenState.value = ScreenState.error(message)
            }
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
}