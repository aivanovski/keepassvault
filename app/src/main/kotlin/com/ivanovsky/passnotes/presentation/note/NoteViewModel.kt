package com.ivanovsky.passnotes.presentation.note

import android.os.Build
import androidx.annotation.IdRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.Attachment
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.data.entity.PropertyType
import com.ivanovsky.passnotes.domain.DatabaseLockInteractor
import com.ivanovsky.passnotes.domain.DateFormatter
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.entity.PropertyFilter
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.note.NoteInteractor
import com.ivanovsky.passnotes.extensions.getOrThrow
import com.ivanovsky.passnotes.injection.GlobalInjector
import com.ivanovsky.passnotes.presentation.ApplicationLaunchMode
import com.ivanovsky.passnotes.presentation.Screens
import com.ivanovsky.passnotes.presentation.Screens.GroupsScreen
import com.ivanovsky.passnotes.presentation.Screens.MainSettingsScreen
import com.ivanovsky.passnotes.presentation.Screens.NoteEditorScreen
import com.ivanovsky.passnotes.presentation.Screens.UnlockScreen
import com.ivanovsky.passnotes.presentation.autofill.model.AutofillStructure
import com.ivanovsky.passnotes.presentation.core.BaseScreenViewModel
import com.ivanovsky.passnotes.presentation.core.CellId
import com.ivanovsky.passnotes.presentation.core.DefaultScreenStateHandler
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.ViewModelTypes
import com.ivanovsky.passnotes.presentation.core.dialog.propertyAction.PropertyAction
import com.ivanovsky.passnotes.presentation.core.event.LockScreenLiveEvent
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import com.ivanovsky.passnotes.presentation.core.menu.ScreenMenuItem
import com.ivanovsky.passnotes.presentation.core.model.NavigationPanelCellModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.DividerCellViewModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.HeaderCellViewModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.NavigationPanelCellViewModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.SpaceCellViewModel
import com.ivanovsky.passnotes.presentation.groups.GroupsScreenArgs
import com.ivanovsky.passnotes.presentation.history.HistoryScreenArgs
import com.ivanovsky.passnotes.presentation.note.cells.viewmodel.AttachmentCellViewModel
import com.ivanovsky.passnotes.presentation.note.cells.viewmodel.NotePropertyCellViewModel
import com.ivanovsky.passnotes.presentation.note.cells.viewmodel.OtpPropertyCellViewModel
import com.ivanovsky.passnotes.presentation.note.factory.NoteCellModelFactory
import com.ivanovsky.passnotes.presentation.note.factory.NoteCellViewModelFactory
import com.ivanovsky.passnotes.presentation.noteEditor.NoteEditorArgs
import com.ivanovsky.passnotes.presentation.noteEditor.NoteEditorMode
import com.ivanovsky.passnotes.presentation.unlock.UnlockScreenArgs
import com.ivanovsky.passnotes.util.TimeUtils.toTimestamp
import com.ivanovsky.passnotes.util.UrlUtils
import java.io.File
import java.util.UUID
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf

class NoteViewModel(
    private val interactor: NoteInteractor,
    private val errorInteractor: ErrorInteractor,
    lockInteractor: DatabaseLockInteractor,
    private val resourceProvider: ResourceProvider,
    private val dateFormatter: DateFormatter,
    private val observerBus: ObserverBus,
    private val cellModelFactory: NoteCellModelFactory,
    private val cellViewModelFactory: NoteCellViewModelFactory,
    private val router: Router,
    private val args: NoteScreenArgs
) : BaseScreenViewModel(),
    ObserverBus.NoteContentObserver {

    val viewTypes = ViewModelTypes()
        .add(NotePropertyCellViewModel::class, R.layout.cell_note_property)
        .add(OtpPropertyCellViewModel::class, R.layout.cell_otp_property)
        .add(DividerCellViewModel::class, R.layout.cell_divider)
        .add(SpaceCellViewModel::class, R.layout.cell_space)
        .add(HeaderCellViewModel::class, R.layout.cell_header)
        .add(AttachmentCellViewModel::class, R.layout.cell_attachment)

    val screenStateHandler = DefaultScreenStateHandler()
    val screenState = MutableLiveData(ScreenState.notInitialized())

    val navigationPanelViewModel = NavigationPanelCellViewModel(
        initModel = NavigationPanelCellModel(
            items = emptyList(),
            isVisible = false
        ),
        eventProvider = eventProvider
    )

    val actionBarTitle = MutableLiveData<String>()
    val shortModifiedText = MutableLiveData<String>()
    val modifiedText = MutableLiveData<String>()
    val createdText = MutableLiveData<String>()
    val isTimeCardExpanded = MutableLiveData(false)
    val visibleMenuItems = MutableLiveData<List<NoteMenuItem>>(emptyList())
    val isFabButtonVisible = MutableLiveData(getFabButtonVisibility())
    val showSnackbarMessageEvent = SingleLiveEvent<String>()
    val finishActivityEvent = SingleLiveEvent<Unit>()
    val sendAutofillResponseEvent = SingleLiveEvent<Pair<Note?, AutofillStructure>>()
    val showAddAutofillDataDialog = SingleLiveEvent<Note>()
    val showPropertyActionDialog = SingleLiveEvent<Property>()
    val showAttachmentActionDialog = SingleLiveEvent<List<AttachmentAction>>()
    val lockScreenEvent = LockScreenLiveEvent(observerBus, lockInteractor)
    val openUrlEvent = SingleLiveEvent<String>()
    val shareFileEvent = SingleLiveEvent<File>()
    val openFileEvent = SingleLiveEvent<File>()

    private var cellIdToPropertyMap: Map<String, Property>? = null
    private var cellIdToAttachmentMap: Map<String, Attachment>? = null
    private var note: Note? = null
    private var history: List<Note>? = null
    private var noteUid: UUID? = args.noteSource.getNoteUid()
    private var navigationPanelGroups: List<Group> = emptyList()
    private var isShowHiddenProperties = false

    private val visiblePropertiesFilter = PropertyFilter.Builder()
        .visible()
        .notEmpty()
        .sortedByType()
        .build()

    init {
        observerBus.register(this)
        subscribeToCellEvents()
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
                finishActivityEvent.call(Unit)
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
            GroupsScreen(
                GroupsScreenArgs(
                    appMode = args.appMode,
                    groupUid = null,
                    isCloseDatabaseOnExit = false,
                    isSearchModeEnabled = true,
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

    fun loadData() {
        val noteUid = this.noteUid

        setScreenState(ScreenState.loading())

        viewModelScope.launch {
            val note = when (args.noteSource) {
                is NoteSource.ByUid -> {
                    val getNoteResult = interactor.getNoteByUid(noteUid ?: args.noteSource.uid)
                    if (getNoteResult.isFailed) {
                        setScreenState(
                            ScreenState.error(
                                errorText = errorInteractor.processAndGetMessage(
                                    getNoteResult.error
                                )
                            )
                        )
                        return@launch
                    }

                    getNoteResult.getOrThrow()
                }

                is NoteSource.ByNote -> {
                    args.noteSource.note
                }
            }

            val getGroupResult = interactor.getGroup(note.groupUid)
            if (getGroupResult.isFailed) {
                setScreenState(
                    ScreenState.error(
                        errorText = errorInteractor.processAndGetMessage(getGroupResult.error)
                    )
                )
                return@launch
            }

            val group = getGroupResult.getOrThrow()
            val getParentsResult = interactor.getAllParents(group.uid)
            if (getParentsResult.isFailed) {
                setScreenState(
                    ScreenState.error(
                        errorText = errorInteractor.processAndGetMessage(getParentsResult.error)
                    )
                )
                return@launch
            }

            val history = if (!args.isViewOnly && noteUid != null) {
                val getHistoryResult = interactor.getHistory(noteUid)
                if (getHistoryResult.isFailed) {
                    setScreenState(
                        ScreenState.error(
                            errorText = errorInteractor.processAndGetMessage(getHistoryResult.error)
                        )
                    )
                    return@launch
                }
                getHistoryResult.getOrThrow()
            } else {
                emptyList()
            }

            navigationPanelGroups = getParentsResult.getOrThrow()
            val parentItems = navigationPanelGroups.map { parent -> parent.title }
            navigationPanelViewModel.setModel(
                NavigationPanelCellModel(
                    items = parentItems,
                    isVisible = !args.isViewOnly
                )
            )

            onDataLoaded(note, history)
        }
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

    fun onToggleHiddenClicked() {
        val note = note ?: return
        val history = history ?: emptyList()

        isShowHiddenProperties = !isShowHiddenProperties

        onDataLoaded(note, history)
    }

    fun onToggleTimeCard() {
        isTimeCardExpanded.value = !(isTimeCardExpanded.value ?: false)
    }

    private fun onDataLoaded(
        note: Note,
        history: List<Note>
    ) {
        this.note = note
        this.history = history

        actionBarTitle.value = note.title
        shortModifiedText.value = resourceProvider.getString(
            R.string.modified_with_str,
            dateFormatter.formatShortDate(note.modified)
        )
        modifiedText.value = dateFormatter.formatDateAndTime(note.modified)
        createdText.value = dateFormatter.formatDateAndTime(note.modified)

        val propertiesWithIds = pairPropertiesWithIds(
            note.properties,
            VISIBLE_PROPERTY_CELL_ID_PREFIX
        )

        val attachmentsWithIds = pairAttachmentsWithIds(
            note.attachments
        )

        cellIdToPropertyMap = propertiesWithIds.associate { (property, id) ->
            id.value to property
        }

        cellIdToAttachmentMap = attachmentsWithIds.associate { (attachment, id) ->
            id.value to attachment
        }

        val expiration = note.expiration?.time?.toTimestamp()

        val models = cellModelFactory.createCellModels(
            propertiesWithIds = propertiesWithIds,
            attachmentsWithIds = attachmentsWithIds,
            expiration = expiration,
            isShowHiddenProperties = isShowHiddenProperties,
            isShowHistoryButton = history.isNotEmpty()
        )

        setCellElements(cellViewModelFactory.createCellViewModels(models, eventProvider))

        setScreenState(ScreenState.data())
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

                event.containsKey(AttachmentCellViewModel.CLICK_EVENT) -> {
                    val id = event.getString(AttachmentCellViewModel.CLICK_EVENT)
                        ?: return@subscribe

                    onOpenAttachmentClicked(id)
                }

                event.containsKey(AttachmentCellViewModel.LONG_CLICK_EVENT) -> {
                    val id = event.getString(AttachmentCellViewModel.LONG_CLICK_EVENT)
                        ?: return@subscribe

                    onAttachmentLongClicked(id)
                }

                event.containsKey(AttachmentCellViewModel.SHARE_ICON_CLICK_EVENT) -> {
                    val id = event.getString(AttachmentCellViewModel.SHARE_ICON_CLICK_EVENT)
                        ?: return@subscribe

                    onShareAttachmentClicked(id)
                }

                event.containsKey(NavigationPanelCellViewModel.ITEM_CLICK_EVENT) -> {
                    val index = event.getInt(NavigationPanelCellViewModel.ITEM_CLICK_EVENT)
                        ?: return@subscribe

                    onNavigationPanelItemClicked(index)
                }

                event.containsKey(OtpPropertyCellViewModel.CLICK_EVENT) -> {
                    val otpCode = event.getString(OtpPropertyCellViewModel.CLICK_EVENT)
                        ?: return@subscribe

                    onOtpCodeClicked(otpCode)
                }

                event.containsKey(HeaderCellViewModel.ITEM_CLICK_EVENT) -> {
                    onHistoryClicked()
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
        interactor.copyToClipboard(text, isProtected = false)
        if (Build.VERSION.SDK_INT < 33) {
            showSnackbarMessageEvent.call(resourceProvider.getString(R.string.copied))
        }
    }

    private fun copyProtectedText(text: String) {
        interactor.copyToClipboardWithTimeout(text, isProtected = true)

        val delayInSeconds = interactor.getTimeoutValueInMillis() / 1000L

        val message = resourceProvider.getString(
            R.string.copied_clipboard_will_be_cleared_in_seconds,
            delayInSeconds
        )
        if (Build.VERSION.SDK_INT < 33) {
            showSnackbarMessageEvent.call(message)
        }
    }

    private fun onNotePropertyLongClicked(cellId: String) {
        val property = cellIdToPropertyMap?.get(cellId) ?: return
        val viewModel = findCellViewModel(cellId, NotePropertyCellViewModel::class) ?: return

        val isValueProtected = viewModel.model.isValueProtected

        val currentProperty = property.copy(
            isProtected = isValueProtected
        )

        showPropertyActionDialog.call(currentProperty)
    }

    fun onOpenAttachmentClicked(cellId: String) {
        val attachment = cellIdToAttachmentMap?.get(cellId) ?: return

        viewModelScope.launch {
            val file = saveAttachmentAndGetFile(attachment) ?: return@launch

            openFileEvent.call(file)
            setScreenState(ScreenState.data())
        }
    }

    fun onShareAttachmentClicked(cellId: String) {
        val attachment = cellIdToAttachmentMap?.get(cellId) ?: return

        viewModelScope.launch {
            val file = saveAttachmentAndGetFile(attachment) ?: return@launch

            shareFileEvent.call(file)
            setScreenState(ScreenState.data())
        }
    }

    private fun onAttachmentLongClicked(cellId: String) {
        val attachment = cellIdToAttachmentMap?.get(cellId) ?: return

        viewModelScope.launch {
            val file = saveAttachmentAndGetFile(attachment) ?: return@launch

            showAttachmentActionDialog.call(
                listOf(
                    AttachmentAction.OpenFile(file),
                    AttachmentAction.OpenAsText(file),
                    AttachmentAction.ShareFile(file)
                )
            )
            setScreenState(ScreenState.data())
        }
    }

    private suspend fun saveAttachmentAndGetFile(attachment: Attachment): File? {
        setScreenState(ScreenState.loading())

        val saveResult = interactor.saveAttachmentToStorage(attachment)
        if (saveResult.isFailed) {
            setScreenState(
                ScreenState.dataWithError(
                    errorText = errorInteractor.processAndGetMessage(saveResult.error)
                )
            )
            return null
        }

        return saveResult.getOrThrow()
    }

    private fun onNavigationPanelItemClicked(index: Int) {
        val groupUid = navigationPanelGroups.getOrNull(index)?.uid ?: return

        router.exit()
        router.newChain(
            GroupsScreen(
                GroupsScreenArgs(
                    appMode = args.appMode,
                    groupUid = groupUid,
                    isCloseDatabaseOnExit = true,
                    isSearchModeEnabled = false,
                    autofillStructure = args.autofillStructure
                )
            )
        )
    }

    private fun onOtpCodeClicked(otpCode: String) {
        copyProtectedText(otpCode)
    }

    private fun onHistoryClicked() {
        val noteUid = this.noteUid ?: return

        router.navigateTo(
            Screens.HistoryScreen(
                HistoryScreenArgs(
                    appMode = args.appMode,
                    noteUid = noteUid,
                    autofillStructure = args.autofillStructure
                )
            )
        )
    }

    private fun getVisibleMenuItems(): List<NoteMenuItem> {
        val screenState = this.screenState.value ?: return emptyList()
        val note = this.note

        val hasHiddenProperties = if (note != null) {
            hasHiddenProperties(note)
        } else {
            false
        }

        return if (screenState.isDisplayingData) {
            mutableListOf<NoteMenuItem>()
                .apply {
                    if (args.appMode == ApplicationLaunchMode.AUTOFILL_SELECTION) {
                        add(NoteMenuItem.SELECT)
                    }

                    add(NoteMenuItem.LOCK)

                    if (!args.isViewOnly) {
                        add(NoteMenuItem.SEARCH)
                    }

                    if (args.appMode == ApplicationLaunchMode.NORMAL) {
                        add(NoteMenuItem.SETTINGS)
                    }

                    if (hasHiddenProperties) {
                        add(NoteMenuItem.TOGGLE_HIDDEN)
                    }
                }
        } else {
            emptyList()
        }
    }

    private fun hasHiddenProperties(note: Note): Boolean {
        val visibleProperties = visiblePropertiesFilter.apply(note.properties)
        val otherProperties = note.properties.subtract(visibleProperties.toSet())
        return otherProperties.isNotEmpty()
    }

    private fun getFabButtonVisibility(): Boolean {
        val screenState = this.screenState.value ?: return false

        return screenState.isDisplayingData &&
            args.appMode == ApplicationLaunchMode.NORMAL &&
            !args.isViewOnly
    }

    private fun pairPropertiesWithIds(
        properties: List<Property>,
        idPrefix: String
    ): List<Pair<Property, CellId>> {
        return properties.mapIndexed { idx, property ->
            property to CellId("$idPrefix-$idx")
        }
    }

    private fun pairAttachmentsWithIds(
        attachments: List<Attachment>
    ): List<Pair<Attachment, CellId>> {
        return attachments.mapIndexed { idx, attachment ->
            attachment to CellId("$ATTACHMENT_CELL_ID_PREFIX-$idx")
        }
    }

    private fun setScreenState(state: ScreenState) {
        screenState.value = state
        isFabButtonVisible.value = getFabButtonVisibility()
        visibleMenuItems.value = getVisibleMenuItems()
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

    sealed class AttachmentAction {

        data class OpenFile(
            val file: File
        ) : AttachmentAction()

        data class OpenAsText(
            val file: File
        ) : AttachmentAction()

        data class ShareFile(
            val file: File
        ) : AttachmentAction()
    }

    companion object {
        private const val ATTACHMENT_CELL_ID_PREFIX = "attachment"
        private const val VISIBLE_PROPERTY_CELL_ID_PREFIX = "visible"
    }
}