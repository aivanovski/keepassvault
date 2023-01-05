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
import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.data.entity.PropertyType
import com.ivanovsky.passnotes.domain.DatabaseLockInteractor
import com.ivanovsky.passnotes.domain.LocaleProvider
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.entity.DatabaseStatus
import com.ivanovsky.passnotes.domain.entity.PropertyFilter
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.note.NoteInteractor
import com.ivanovsky.passnotes.extensions.getOrThrow
import com.ivanovsky.passnotes.extensions.getOrNull
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
import com.ivanovsky.passnotes.presentation.core.event.LockScreenLiveEvent
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import com.ivanovsky.passnotes.presentation.core.factory.DatabaseStatusCellModelFactory
import com.ivanovsky.passnotes.presentation.core.menu.ScreenMenuItem
import com.ivanovsky.passnotes.presentation.core.viewmodel.DatabaseStatusCellViewModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.DividerCellViewModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.HeaderCellViewModel
import com.ivanovsky.passnotes.presentation.note.cells.viewmodel.NotePropertyCellViewModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.SpaceCellViewModel
import com.ivanovsky.passnotes.presentation.note.factory.NoteCellModelFactory
import com.ivanovsky.passnotes.presentation.note.factory.NoteCellViewModelFactory
import com.ivanovsky.passnotes.presentation.note_editor.NoteEditorMode
import com.ivanovsky.passnotes.presentation.note_editor.NoteEditorArgs
import com.ivanovsky.passnotes.presentation.search.SearchScreenArgs
import com.ivanovsky.passnotes.presentation.unlock.UnlockScreenArgs
import com.ivanovsky.passnotes.util.StringUtils.STAR
import com.ivanovsky.passnotes.util.UrlUtils
import com.ivanovsky.passnotes.util.formatAccordingLocale
import com.ivanovsky.passnotes.util.substituteAll
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf
import java.util.*

class NoteViewModel(
    private val interactor: NoteInteractor,
    private val errorInteractor: ErrorInteractor,
    lockInteractor: DatabaseLockInteractor,
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
        .add(DividerCellViewModel::class, R.layout.cell_divider)
        .add(SpaceCellViewModel::class, R.layout.cell_space)
        .add(HeaderCellViewModel::class, R.layout.cell_header)

    val screenStateHandler = DefaultScreenStateHandler()
    val screenState = MutableLiveData(ScreenState.notInitialized())

    val actionBarTitle = MutableLiveData<String>()
    val modifiedText = MutableLiveData<String>()
    val visibleMenuItems = MutableLiveData<List<NoteMenuItem>>(emptyList())
    val isFabButtonVisible = MutableLiveData(false)
    val showSnackbarMessageEvent = SingleLiveEvent<String>()
    val finishActivityEvent = SingleLiveEvent<Unit>()
    val sendAutofillResponseEvent = SingleLiveEvent<Pair<Note?, AutofillStructure>>()
    val showAddAutofillDataDialog = SingleLiveEvent<Note>()
    val showPropertyActionDialog = SingleLiveEvent<List<PropertyAction>>()
    val lockScreenEvent = LockScreenLiveEvent(observerBus, lockInteractor)
    val openUrlEvent = SingleLiveEvent<String>()

    val statusViewModel = MutableLiveData(
        cellViewModelFactory.createCellViewModel(
            model = statusCellModelFactory.createDefaultStatusCellModel(),
            eventProvider = eventProvider
        ) as DatabaseStatusCellViewModel
    )

    private var cellIdToPropertyMap: Map<String, Property>? = null
    private var note: Note? = null
    private var noteUid: UUID = args.noteUid
    private var isShowHiddenProperties = false

    private val visiblePropertiesFilter = PropertyFilter.Builder()
        .visible()
        .notEmpty()
        .sortedByType()
        .build()

    private val hiddenPropertiesFilter = PropertyFilter.Builder()
        .hidden()
        .notEmpty()
        .build()

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
        val structure = args.autofillStructure ?: return

        if (args.appMode != ApplicationLaunchMode.AUTOFILL_SELECTION) {
            return
        }

        val note = this.note
        if (note == null) {
            sendAutofillResponseEvent.call(Pair(null, structure))
            return
        }

        viewModelScope.launch {
            if (interactor.shouldUpdateNoteAutofillData(note, structure)) {
                showAddAutofillDataDialog.call(note)
            } else {
                sendAutofillResponseEvent.call(Pair(note, structure))
            }
        }
    }

    fun onAddAutofillDataConfirmed(note: Note) {
        val structure = args.autofillStructure ?: return

        screenState.value = ScreenState.loading()

        viewModelScope.launch {
            val updateNoteResult = interactor.updateNoteWithAutofillData(note, structure)
            if (updateNoteResult.isFailed) {
                val message = errorInteractor.processAndGetMessage(updateNoteResult.error)
                screenState.value = ScreenState.dataWithError(message)
                return@launch
            }

            sendAutofillResponseEvent.call(Pair(note, structure))
        }
    }

    fun onAddAutofillDataDenied(note: Note) {
        val structure = args.autofillStructure ?: return

        sendAutofillResponseEvent.call(Pair(note, structure))
    }

    fun loadData() {
        visibleMenuItems.value = getVisibleMenuItems()
        isFabButtonVisible.value = getFabButtonVisibility()

        viewModelScope.launch {
            val getNoteResult = interactor.getNoteByUid(noteUid)
            val getDbStatus = interactor.getDatabaseStatus()

            if (getNoteResult.isSucceededOrDeferred) {
                onNoteLoaded(getNoteResult.getOrThrow(), getDbStatus.getOrNull())
            } else {
                val message = errorInteractor.processAndGetMessage(getNoteResult.error)
                screenState.value = ScreenState.error(message)
            }

            isFabButtonVisible.value = getFabButtonVisibility()
        }
    }

    fun onPropertyActionClicked(action: PropertyAction) {
        when (action) {
            is PropertyAction.CopyText -> {
                if (action.isResetClipboard) {
                    copyProtectedText(action.text)
                } else {
                    copyText(action.text)
                }
            }
            is PropertyAction.OpenUrl -> {
                openUrlEvent.call(action.url)
            }
        }
    }

    fun onToggleHiddenClicked() {
        val note = note ?: return

        isShowHiddenProperties = !isShowHiddenProperties

        onNoteLoaded(note, null)
    }

    private fun onNoteLoaded(note: Note, dbStatus: DatabaseStatus?) {
        this.note = note

        actionBarTitle.value = note.title
        modifiedText.value =
            note.modified.formatAccordingLocale(localeProvider.getSystemLocale())

        val visibleProperties = visiblePropertiesFilter.apply(note.properties)
        val hiddenProperties = if (isShowHiddenProperties) {
            hiddenPropertiesFilter.apply(note.properties)
        } else {
            emptyList()
        }

        val visibleIdsAndProperties = pairCellIdAndProperties(visibleProperties, "visible")
        val hiddenIdsAndProperties = pairCellIdAndProperties(hiddenProperties, "hidden")
        cellIdToPropertyMap = visibleIdsAndProperties.toMap() + hiddenIdsAndProperties.toMap()

        val models = cellModelFactory.createCellModels(
            visibleIdsAndProperties,
            hiddenIdsAndProperties
        )

        setCellElements(cellViewModelFactory.createCellViewModels(models, eventProvider))

        if (dbStatus != null) {
            updateStatusViewModel(dbStatus)
        }

        screenState.value = ScreenState.data()
        visibleMenuItems.value = getVisibleMenuItems()
    }

    private fun subscribeToCellEvents() {
        eventProvider.subscribe(this) { event ->
            when {
                event.containsKey(NotePropertyCellViewModel.CLICK_EVENT) -> {
                    val id = event.getString(NotePropertyCellViewModel.CLICK_EVENT)
                        ?: return@subscribe

                    onNotePropertyClicked(id)
                }
                event.containsKey(NotePropertyCellViewModel.LONG_CLICK_EVENT) -> {
                    val id = event.getString(NotePropertyCellViewModel.LONG_CLICK_EVENT)
                        ?: return@subscribe

                    onNotePropertyLongClicked(id)
                }
            }
        }
    }

    private fun onNotePropertyClicked(cellId: String) {
        val property = cellIdToPropertyMap?.get(cellId) ?: return
        val value = property.value ?: return

        val url = if (property.type == PropertyType.URL) {
            UrlUtils.parseUrl(property.value)
        } else {
            null
        }

        if (url?.isValid() == true) {
            openUrlEvent.call(url.formatToString())
        } else {
            if (property.isProtected) {
                copyProtectedText(value)
            } else {
                copyText(value)
            }
        }
    }

    private fun copyText(text: String) {
        interactor.copyToClipboard(text)
        showSnackbarMessageEvent.call(resourceProvider.getString(R.string.copied))
    }

    private fun copyProtectedText(text: String) {
        interactor.copyToClipboardWithTimeout(text)

        val delayInSeconds = interactor.getTimeoutValueInMillis() / 1000L

        val message = resourceProvider.getString(
            R.string.copied_clipboard_will_be_cleared_in_seconds,
            delayInSeconds
        )
        showSnackbarMessageEvent.call(message)
    }

    private fun onNotePropertyLongClicked(cellId: String) {
        val property = cellIdToPropertyMap?.get(cellId) ?: return
        val viewModel = findCellViewModel(cellId, NotePropertyCellViewModel::class) ?: return

        val isValueProtected = viewModel.model.isValueProtected

        val options = mutableListOf<PropertyAction>()

        if (!property.name.isNullOrBlank()) {
            options.add(
                PropertyAction.CopyText(
                    title = property.name,
                    text = property.name,
                    isResetClipboard = false
                )
            )
        }

        if (!property.value.isNullOrBlank()) {
            val title = if (isValueProtected) {
                property.value.substituteAll(STAR)
            } else {
                property.value
            }

            options.add(
                PropertyAction.CopyText(
                    title = title,
                    text = property.value,
                    isResetClipboard = property.isProtected
                )
            )

            if (property.type == PropertyType.URL) {
                val url = UrlUtils.parseUrl(property.value)
                if (url?.isValid() == true) {
                    options.add(PropertyAction.OpenUrl(url.formatToString()))
                }
            }
        }

        showPropertyActionDialog.call(options)
    }

    private fun updateStatusViewModel(status: DatabaseStatus) {
        statusViewModel.value = cellViewModelFactory.createCellViewModel(
            model = statusCellModelFactory.createStatusCellModel(status),
            eventProvider = eventProvider
        ) as DatabaseStatusCellViewModel
    }

    private fun getVisibleMenuItems(): List<NoteMenuItem> {
        val screenState = this.screenState.value ?: return emptyList()
        val note = this.note

        val hasHiddenProperties = if (note != null) {
            hasHiddenProperties(note)
        } else {
            false
        }

        return when {
            screenState.isDisplayingData && args.appMode == ApplicationLaunchMode.NORMAL -> {
                listOfNotNull(
                    NoteMenuItem.LOCK,
                    NoteMenuItem.SEARCH,
                    NoteMenuItem.SETTINGS,
                    if (hasHiddenProperties) NoteMenuItem.TOGGLE_HIDDEN else null
                )
            }
            screenState.isDisplayingData && args.appMode == ApplicationLaunchMode.AUTOFILL_SELECTION -> {
                listOfNotNull(
                    NoteMenuItem.SELECT,
                    NoteMenuItem.LOCK,
                    NoteMenuItem.SEARCH,
                    if (hasHiddenProperties) NoteMenuItem.TOGGLE_HIDDEN else null
                )
            }
            else -> emptyList()
        }
    }

    private fun hasHiddenProperties(note: Note): Boolean {
        return hiddenPropertiesFilter.apply(note.properties).isNotEmpty()
    }

    private fun getFabButtonVisibility(): Boolean {
        val screenState = this.screenState.value ?: return false

        return screenState.isDisplayingData &&
            args.appMode == ApplicationLaunchMode.NORMAL
    }

    private fun pairCellIdAndProperties(
        properties: List<Property>,
        idPrefix: String
    ): List<Pair<String, Property>> {
        return properties.mapIndexed { idx, property -> Pair("$idPrefix-$idx", property) }
    }

    class Factory(private val args: NoteScreenArgs) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GlobalInjector.get<NoteViewModel>(
                parametersOf(args)
            ) as T
        }
    }

    enum class NoteMenuItem(@IdRes override val menuId: Int) : ScreenMenuItem {
        SELECT(R.id.menu_select),
        LOCK(R.id.menu_lock),
        SEARCH(R.id.menu_search),
        SETTINGS(R.id.menu_settings),
        TOGGLE_HIDDEN(R.id.menu_toggle_hidden)
    }

    sealed class PropertyAction {

        data class CopyText(
            val title: String,
            val text: String,
            val isResetClipboard: Boolean
        ) : PropertyAction()

        data class OpenUrl(
            val url: String
        ) : PropertyAction()
    }
}