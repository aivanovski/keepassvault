package com.ivanovsky.passnotes.presentation.groups

import androidx.annotation.IdRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.EncryptedDatabaseEntry
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.Template
import com.ivanovsky.passnotes.data.repository.settings.OnSettingsChangeListener
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.entity.DatabaseStatus
import com.ivanovsky.passnotes.domain.entity.SelectionItem
import com.ivanovsky.passnotes.domain.entity.SelectionItemType
import com.ivanovsky.passnotes.domain.entity.SortDirection
import com.ivanovsky.passnotes.domain.entity.SortType
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.SelectionHolder
import com.ivanovsky.passnotes.domain.interactor.SelectionHolder.ActionType
import com.ivanovsky.passnotes.domain.interactor.groups.GroupsInteractor
import com.ivanovsky.passnotes.injection.GlobalInjector
import com.ivanovsky.passnotes.presentation.ApplicationLaunchMode
import com.ivanovsky.passnotes.presentation.Screens.GroupEditorScreen
import com.ivanovsky.passnotes.presentation.Screens.GroupsScreen
import com.ivanovsky.passnotes.presentation.Screens.MainSettingsScreen
import com.ivanovsky.passnotes.presentation.Screens.NoteEditorScreen
import com.ivanovsky.passnotes.presentation.Screens.NoteScreen
import com.ivanovsky.passnotes.presentation.Screens.SearchScreen
import com.ivanovsky.passnotes.presentation.Screens.UnlockScreen
import com.ivanovsky.passnotes.presentation.core.BaseScreenViewModel
import com.ivanovsky.passnotes.presentation.core.DefaultScreenStateHandler
import com.ivanovsky.passnotes.presentation.core.ScreenDisplayingType
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.ViewModelTypes
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import com.ivanovsky.passnotes.presentation.core.factory.DatabaseStatusCellModelFactory
import com.ivanovsky.passnotes.presentation.core.menu.ScreenMenuItem
import com.ivanovsky.passnotes.presentation.core.viewmodel.DatabaseStatusCellViewModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.GroupCellViewModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.NoteCellViewModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.OptionPanelCellViewModel
import com.ivanovsky.passnotes.presentation.group_editor.GroupEditorArgs
import com.ivanovsky.passnotes.presentation.groups.factory.GroupsCellModelFactory
import com.ivanovsky.passnotes.presentation.groups.factory.GroupsCellViewModelFactory
import com.ivanovsky.passnotes.presentation.groups.sorting.SortByDateStrategy
import com.ivanovsky.passnotes.presentation.groups.sorting.SortByDateStrategy.Type
import com.ivanovsky.passnotes.presentation.groups.sorting.SortByDefaultOrderStrategy
import com.ivanovsky.passnotes.presentation.groups.sorting.SortByTitleStrategy
import com.ivanovsky.passnotes.presentation.note.NoteScreenArgs
import com.ivanovsky.passnotes.presentation.note_editor.NoteEditorMode
import com.ivanovsky.passnotes.presentation.note_editor.NoteEditorArgs
import com.ivanovsky.passnotes.presentation.search.SearchScreenArgs
import com.ivanovsky.passnotes.presentation.unlock.UnlockScreenArgs
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import com.ivanovsky.passnotes.util.toUUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.parameter.parametersOf
import java.util.UUID

class GroupsViewModel(
    private val interactor: GroupsInteractor,
    private val errorInteractor: ErrorInteractor,
    private val observerBus: ObserverBus,
    private val settings: Settings,
    private val dispatchers: DispatcherProvider,
    private val resourceProvider: ResourceProvider,
    private val cellModelFactory: GroupsCellModelFactory,
    private val statusCellModelFactory: DatabaseStatusCellModelFactory,
    private val cellViewModelFactory: GroupsCellViewModelFactory,
    private val selectionHolder: SelectionHolder,
    private val router: Router,
    private val args: GroupsScreenArgs
) : BaseScreenViewModel(),
    ObserverBus.GroupDataSetObserver,
    ObserverBus.NoteDataSetChanged,
    ObserverBus.NoteContentObserver,
    ObserverBus.DatabaseCloseObserver,
    ObserverBus.DatabaseStatusObserver,
    OnSettingsChangeListener {

    val viewTypes = ViewModelTypes()
        .add(NoteCellViewModel::class, R.layout.cell_note)
        .add(GroupCellViewModel::class, R.layout.cell_group)

    val screenStateHandler = DefaultScreenStateHandler()
    val screenState = MutableLiveData(ScreenState.notInitialized())

    val statusViewModel = cellViewModelFactory.createCellViewModel(
        model = statusCellModelFactory.createDefaultStatusCellModel(),
        eventProvider = eventProvider
    ) as DatabaseStatusCellViewModel

    val optionPanelViewModel = cellViewModelFactory.createCellViewModel(
        model = cellModelFactory.createOptionPanelCellModel(OptionPanelState.HIDDEN),
        eventProvider = eventProvider
    ) as OptionPanelCellViewModel

    val screenTitle = MutableLiveData(EMPTY)
    val visibleMenuItems = MutableLiveData<List<GroupsMenuItem>>(emptyList())
    val isFabButtonVisible = MutableLiveData(false)
    val showToastEvent = SingleLiveEvent<String>()
    val showNewEntryDialogEvent = SingleLiveEvent<List<Template>>()
    val showGroupActionsDialogEvent = SingleLiveEvent<Group>()
    val showNoteActionsDialogEvent = SingleLiveEvent<Note>()
    val showRemoveConfirmationDialogEvent = SingleLiveEvent<Pair<Group?, Note?>>()
    val showAddTemplatesDialogEvent = SingleLiveEvent<Unit>()
    val finishActivityEvent = SingleLiveEvent<Unit>()
    val showUnlockScreenEvent = SingleLiveEvent<UnlockScreen>()
    val showSortAndViewDialogEvent = SingleLiveEvent<Unit>()

    private var currentDataItems: List<EncryptedDatabaseEntry>? = null
    private var rootGroupUid: UUID? = null
    private var groupUid: UUID? = args.groupUid
    private var templates: List<Template>? = null
    private var isAutofillSavingCancelled = false

    init {
        observerBus.register(this)
        settings.register(this)
        subscribeToEvents()
    }

    override fun onCleared() {
        super.onCleared()
        observerBus.unregister(this)
        settings.register(this)
    }

    override fun onGroupDataSetChanged() {
        loadData()
    }

    override fun onNoteDataSetChanged(groupUid: UUID) {
        if (groupUid == getCurrentGroupUid()) {
            loadData()
        }
    }

    override fun onNoteContentChanged(groupUid: UUID, oldNoteUid: UUID, newNoteUid: UUID) {
        if (groupUid == getCurrentGroupUid()) {
            loadData()
        }
    }

    override fun onDatabaseClosed() {
        showUnlockScreenEvent.call(
            UnlockScreen(
                UnlockScreenArgs(
                    appMode = args.appMode,
                    autofillStructure = args.autofillStructure
                )
            )
        )
    }

    override fun onDatabaseStatusChanged(status: DatabaseStatus) {
        updateStatusViewModel(status)
    }

    override fun onSettingsChanged(pref: SettingsImpl.Pref) {
        if (pref == SettingsImpl.Pref.SORT_TYPE ||
            pref == SettingsImpl.Pref.SORT_DIRECTION ||
            pref == SettingsImpl.Pref.IS_GROUPS_AT_START_ENABLED) {
            loadData()
        }
    }

    fun start() {
        if (groupUid == null) {
            screenTitle.value = resourceProvider.getString(R.string.groups)
        }

        loadData()
    }

    fun loadData() {
        setScreenState(ScreenState.loading())

        viewModelScope.launch {
            templates = interactor.getTemplates().obj

            val data = withContext(Dispatchers.IO) {
                groupUid.let {
                    if (it == null) {
                        interactor.getRootGroupData()
                    } else {
                        interactor.getGroupData(it)
                    }
                }
            }

            if (groupUid == null && rootGroupUid == null) {
                rootGroupUid = withContext(Dispatchers.IO) {
                    interactor.getRootUid()
                }
            }

            groupUid?.let {
                val group = interactor.getGroup(it)
                if (group.isSucceededOrDeferred) {
                    screenTitle.value = group.obj.title
                }
            }

            val status = interactor.getDatabaseStatus()

            if (data.isSucceededOrDeferred) {
                val dataItems = sortDataItems(
                    data.obj,
                    settings.sortType,
                    settings.sortDirection,
                    settings.isGroupsAtStartEnabled
                )
                currentDataItems = dataItems

                if (dataItems.isNotEmpty()) {
                    val models = cellModelFactory.createCellModels(dataItems)
                    val viewModels =
                        cellViewModelFactory.createCellViewModels(models, eventProvider)
                    setCellElements(viewModels)
                    setScreenState(ScreenState.data())
                } else {
                    val emptyText = resourceProvider.getString(R.string.no_items)
                    setScreenState(ScreenState.empty(emptyText))
                }

                if (status.isSucceededOrDeferred) {
                    updateStatusViewModel(status.obj)
                }

                visibleMenuItems.value = getVisibleMenuItems()
            } else {
                val message = errorInteractor.processAndGetMessage(data.error)
                setScreenState(ScreenState.error(message))
            }

            updateOptionPanelState()
        }
    }

    fun onAddButtonClicked() {
        showNewEntryDialogEvent.call(templates ?: emptyList())
    }

    fun onCreateNewGroupClicked() {
        val currentGroupUid = getCurrentGroupUid() ?: return

        router.navigateTo(
            GroupEditorScreen(
                GroupEditorArgs.NewGroup(
                    parentGroupUid = currentGroupUid
                )
            )
        )
    }

    fun onCreateNewNoteClicked() {
        val currentGroupUid = getCurrentGroupUid() ?: return

        router.navigateTo(
            NoteEditorScreen(
                NoteEditorArgs(
                    mode = NoteEditorMode.NEW,
                    groupUid = currentGroupUid,
                    template = null,
                    title = resourceProvider.getString(R.string.new_note)
                )
            )
        )
    }

    fun onCreateNewNoteFromTemplateClicked(template: Template) {
        val currentGroupUid = getCurrentGroupUid() ?: return

        router.navigateTo(
            NoteEditorScreen(
                NoteEditorArgs(
                    mode = NoteEditorMode.NEW,
                    groupUid = currentGroupUid,
                    template = template,
                    title = resourceProvider.getString(R.string.new_note)
                )
            )
        )
    }

    fun onEditGroupClicked(group: Group) {
        router.navigateTo(
            GroupEditorScreen(
                GroupEditorArgs.EditGroup(
                    groupUid = group.uid
                )
            )
        )
    }

    fun onRemoveGroupClicked(group: Group) {
        showRemoveConfirmationDialogEvent.call(Pair(group, null))
    }

    fun onCutGroupClicked(group: Group) {
        val currentGroupUid = getCurrentGroupUid() ?: return

        selectionHolder.select(
            action = ActionType.CUT,
            selection = SelectionItem(
                uid = group.uid,
                parentUid = currentGroupUid,
                type = SelectionItemType.GROUP_UID
            )
        )

        updateOptionPanelState()
    }

    fun onEditNoteClicked(note: Note) {
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

    fun onRemoveNoteClicked(note: Note) {
        showRemoveConfirmationDialogEvent.call(Pair(null, note))
    }

    fun onCutNoteClicked(note: Note) {
        val noteUid = note.uid ?: return
        val currentGroupUid = getCurrentGroupUid() ?: return

        selectionHolder.select(
            action = ActionType.CUT,
            selection = SelectionItem(
                uid = noteUid,
                parentUid = currentGroupUid,
                type = SelectionItemType.NOTE_UID
            )
        )

        updateOptionPanelState()
    }

    fun onRemoveConfirmed(group: Group?, note: Note?) {
        val groupUid = group?.uid
        val noteUid = note?.uid

        if (groupUid != null) {
            removeGroup(groupUid)
        } else if (noteUid != null) {
            removeNote(note.groupUid, noteUid)
        }
    }

    fun onBackClicked() {
        if (args.isCloseDatabaseOnExit) {
            interactor.lockDatabase()
        }

        if (args.appMode == ApplicationLaunchMode.AUTOFILL_SELECTION) {
            finishActivityEvent.call()
        } else {
            router.exit()
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

    fun onAddTemplatesClicked() {
        showAddTemplatesDialogEvent.call()
    }

    fun onAddTemplatesConfirmed() {
        setScreenState(ScreenState.loading())

        viewModelScope.launch {
            val isAdded = interactor.addTemplates()

            if (isAdded.isSucceededOrDeferred) {
                setScreenState(ScreenState.data())
                showToastEvent.call(resourceProvider.getString(R.string.successfully_added))

                visibleMenuItems.value = getVisibleMenuItems()
            } else {
                val message = errorInteractor.processAndGetMessage(isAdded.error)
                setScreenState(ScreenState.error(message))
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

    fun onSortAndViewButtonClicked() {
        showSortAndViewDialogEvent.call()
    }

    fun onSettingsButtonClicked() = router.navigateTo(MainSettingsScreen())

    private fun subscribeToEvents() {
        eventProvider.subscribe(this) { event ->
            when {
                event.containsKey(GroupCellViewModel.CLICK_EVENT) -> {
                    event.getString(GroupCellViewModel.CLICK_EVENT)?.toUUID()?.let {
                        onGroupClicked(it)
                    }
                }
                event.containsKey(GroupCellViewModel.LONG_CLICK_EVENT) -> {
                    event.getString(GroupCellViewModel.LONG_CLICK_EVENT)?.toUUID()?.let {
                        onGroupLongClicked(it)
                    }
                }
                event.containsKey(NoteCellViewModel.CLICK_EVENT) -> {
                    event.getString(NoteCellViewModel.CLICK_EVENT)?.toUUID()?.let {
                        onNoteClicked(it)
                    }
                }
                event.containsKey(NoteCellViewModel.LONG_CLICK_EVENT) -> {
                    event.getString(NoteCellViewModel.LONG_CLICK_EVENT)?.toUUID()?.let {
                        onNoteLongClicked(it)
                    }
                }
                event.containsKey(OptionPanelCellViewModel.POSITIVE_BUTTON_CLICK_EVENT) -> {
                    onPositiveOptionSelected()
                }
                event.containsKey(OptionPanelCellViewModel.NEGATIVE_BUTTON_CLICK_EVENT) -> {
                    onNegativeOptionSelected()
                }
            }
        }
    }

    private fun onGroupClicked(groupUid: UUID) {
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

    private fun onGroupLongClicked(groupUid: UUID) {
        val group = findGroupInItems(groupUid) ?: return

        showGroupActionsDialogEvent.call(group)
    }

    private fun findGroupInItems(groupUid: UUID): Group? {
        return (currentDataItems?.firstOrNull { item ->
            if (item is Group) {
                item.uid == groupUid
            } else {
                false
            }
        } as? Group)
    }

    private fun onNoteClicked(noteUid: UUID) {
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

    private fun onNoteLongClicked(noteUid: UUID) {
        val note = findNoteInItems(noteUid) ?: return

        showNoteActionsDialogEvent.call(note)
    }

    private fun findNoteInItems(noteUid: UUID): Note? {
        return (currentDataItems?.firstOrNull { item ->
            if (item is Note) {
                item.uid == noteUid
            } else {
                false
            }
        } as? Note)
    }

    private fun getCurrentGroupUid(): UUID? {
        return when {
            groupUid != null -> groupUid
            rootGroupUid != null -> rootGroupUid
            else -> null
        }
    }

    private fun removeGroup(groupUid: UUID) {
        setScreenState(ScreenState.loading())

        viewModelScope.launch {
            val removeResult = withContext(Dispatchers.Default) {
                interactor.removeGroup(groupUid)
            }

            if (removeResult.isSucceededOrDeferred) {
                showToastEvent.call(resourceProvider.getString(R.string.successfully_removed))

                visibleMenuItems.value = getVisibleMenuItems()
            } else {
                val message = errorInteractor.processAndGetMessage(removeResult.error)
                setScreenState(ScreenState.error(message))
            }
        }
    }

    private fun removeNote(groupUid: UUID, noteUid: UUID) {
        setScreenState(ScreenState.loading())

        viewModelScope.launch {
            val removeResult = withContext(Dispatchers.Default) {
                interactor.removeNote(groupUid, noteUid)
            }

            if (removeResult.isSucceededOrDeferred) {
                showToastEvent.call(resourceProvider.getString(R.string.successfully_removed))

                visibleMenuItems.value = getVisibleMenuItems()
            } else {
                val message = errorInteractor.processAndGetMessage(removeResult.error)
                setScreenState(ScreenState.error(message))
            }
        }
    }

    private fun hideStatusCell() {
        updateStatusViewModel(DatabaseStatus.NORMAL)
    }

    private fun updateStatusViewModel(status: DatabaseStatus) {
        statusViewModel.setModel(statusCellModelFactory.createStatusCellModel(status))
    }

    private fun updateOptionPanelState() {
        optionPanelViewModel.setModel(
            cellModelFactory.createOptionPanelCellModel(
                state = getCurrentOptionPanelState()
            )
        )
    }

    private fun getCurrentOptionPanelState(): OptionPanelState {
        val screenState = this.screenState.value ?: return OptionPanelState.HIDDEN

        return when {
            !screenState.isDisplayingData -> OptionPanelState.HIDDEN
            args.note != null && !isAutofillSavingCancelled -> OptionPanelState.SAVE_AUTOFILL_DATA
            selectionHolder.hasSelection() -> OptionPanelState.PASTE
            else -> OptionPanelState.HIDDEN
        }
    }

    private fun onPositiveOptionSelected() {
        when (getCurrentOptionPanelState()) {
            OptionPanelState.PASTE -> {
                onPasteButtonClicked()
            }
            OptionPanelState.SAVE_AUTOFILL_DATA -> {
                onSaveAutofillNoteClicked()
            }
            else -> {}
        }
    }

    private fun onPasteButtonClicked() {
        val currentGroupUid = getCurrentGroupUid() ?: return
        val selection = selectionHolder.getSelection() ?: return
        val action = selectionHolder.getAction() ?: return

        if (selection.parentUid == currentGroupUid) {
            showToastEvent.value =
                resourceProvider.getString(R.string.selected_item_is_already_here)
            return
        }

        setScreenState(ScreenState.loading())

        viewModelScope.launch {
            val actionResult = interactor.doActionOnSelection(
                selectedGroupUid = currentGroupUid,
                action = action,
                selection = selection
            )
            selectionHolder.clear()

            if (actionResult.isSucceededOrDeferred) {
                showToastEvent.call(resourceProvider.getString(R.string.successfully))
                setScreenState(ScreenState.data())
                visibleMenuItems.value = getVisibleMenuItems()
            } else {
                val message = errorInteractor.processAndGetMessage(actionResult.error)
                setScreenState(ScreenState.error(message))
            }
        }
    }

    private fun onSaveAutofillNoteClicked() {
        isAutofillSavingCancelled = true
        router.navigateTo(
            NoteEditorScreen(
                NoteEditorArgs(
                    mode = NoteEditorMode.NEW,
                    groupUid = getCurrentGroupUid(),
                    properties = args.note?.properties
                )
            )
        )
    }

    private fun onNegativeOptionSelected() {
        when (getCurrentOptionPanelState()) {
            OptionPanelState.PASTE -> {
                selectionHolder.clear()
            }
            OptionPanelState.SAVE_AUTOFILL_DATA -> {
                isAutofillSavingCancelled = true
            }
            else -> {
            }
        }
        updateOptionPanelState()
    }

    private fun setScreenState(state: ScreenState) {
        screenState.value = state
        isFabButtonVisible.value = getFabButtonVisibility()

        when (state.screenDisplayingType) {
            ScreenDisplayingType.LOADING -> {
                visibleMenuItems.value = getVisibleMenuItems()
                hideStatusCell()
                updateOptionPanelState()
            }
        }
    }

    private fun getFabButtonVisibility(): Boolean {
        val screenState = this.screenState.value ?: return false

        return (screenState.isDisplayingData || screenState.isDisplayingEmptyState) &&
            args.appMode == ApplicationLaunchMode.NORMAL
    }

    private fun getVisibleMenuItems(): List<GroupsMenuItem> {
        val screenState = this.screenState.value ?: return emptyList()

        val isShowMenu = (screenState.isDisplayingData || screenState.isDisplayingEmptyState)

        return when {
            isShowMenu && args.appMode == ApplicationLaunchMode.NORMAL -> {
                val items = mutableListOf(
                    GroupsMenuItem.SEARCH,
                    GroupsMenuItem.LOCK,
                    GroupsMenuItem.VIEW_MODE,
                    GroupsMenuItem.SETTINGS
                )

                if (templates.isNullOrEmpty()) {
                    items.add(GroupsMenuItem.ADD_TEMPLATES)
                }

                items
            }
            isShowMenu && args.appMode == ApplicationLaunchMode.AUTOFILL_SELECTION -> {
                listOf(
                    GroupsMenuItem.SEARCH,
                    GroupsMenuItem.LOCK,
                    GroupsMenuItem.VIEW_MODE,
                    GroupsMenuItem.SETTINGS
                )
            }
            else -> emptyList()
        }
    }

    private suspend fun sortDataItems(
        items: List<EncryptedDatabaseEntry>,
        sortType: SortType,
        direction: SortDirection,
        isGroupsAtStart: Boolean
    ): List<EncryptedDatabaseEntry> =
        withContext(dispatchers.IO) {
            when (sortType) {
                SortType.DEFAULT -> SortByDefaultOrderStrategy().sort(
                    items,
                    direction,
                    isGroupsAtStart = isGroupsAtStart
                )
                SortType.TITLE -> SortByTitleStrategy().sort(
                    items,
                    direction,
                    isGroupsAtStart = isGroupsAtStart
                )
                SortType.CREATION_DATE -> SortByDateStrategy(Type.CREATION_DATE).sort(
                    items,
                    direction,
                    isGroupsAtStart = isGroupsAtStart
                )
                SortType.MODIFICATION_DATE -> SortByDateStrategy(Type.MODIFICATION_DATE).sort(
                    items,
                    direction,
                    isGroupsAtStart = isGroupsAtStart
                )
            }
        }

    enum class OptionPanelState {
        HIDDEN,
        PASTE,
        SAVE_AUTOFILL_DATA
    }

    class Factory(private val args: GroupsScreenArgs) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return GlobalInjector.get<GroupsViewModel>(
                parametersOf(args)
            ) as T
        }
    }

    enum class GroupsMenuItem(@IdRes override val menuId: Int) : ScreenMenuItem {
        SEARCH(R.id.menu_search),
        LOCK(R.id.menu_lock),
        VIEW_MODE(R.id.menu_sort_and_view),
        ADD_TEMPLATES(R.id.menu_add_templates),
        SETTINGS(R.id.menu_settings)
    }
}