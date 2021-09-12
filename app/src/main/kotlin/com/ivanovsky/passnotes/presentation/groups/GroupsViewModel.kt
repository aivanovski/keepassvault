package com.ivanovsky.passnotes.presentation.groups

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.Template
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.entity.DatabaseStatus
import com.ivanovsky.passnotes.domain.entity.SelectionItem
import com.ivanovsky.passnotes.domain.entity.SelectionItemType
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.SelectionHolder
import com.ivanovsky.passnotes.domain.interactor.SelectionHolder.ActionType
import com.ivanovsky.passnotes.domain.interactor.groups.GroupsInteractor
import com.ivanovsky.passnotes.presentation.Screens.GroupScreen
import com.ivanovsky.passnotes.presentation.Screens.GroupsScreen
import com.ivanovsky.passnotes.presentation.Screens.MainSettingsScreen
import com.ivanovsky.passnotes.presentation.Screens.NoteEditorScreen
import com.ivanovsky.passnotes.presentation.Screens.NoteScreen
import com.ivanovsky.passnotes.presentation.Screens.SearchScreen
import com.ivanovsky.passnotes.presentation.Screens.UnlockScreen
import com.ivanovsky.passnotes.presentation.core.BaseScreenViewModel
import com.ivanovsky.passnotes.presentation.core.DefaultScreenStateHandler
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.ViewModelTypes
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import com.ivanovsky.passnotes.presentation.core.factory.DatabaseStatusCellModelFactory
import com.ivanovsky.passnotes.presentation.core.viewmodel.DatabaseStatusCellViewModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.GroupCellViewModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.NoteCellViewModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.OptionPanelCellViewModel
import com.ivanovsky.passnotes.presentation.groups.factory.GroupsCellModelFactory
import com.ivanovsky.passnotes.presentation.groups.factory.GroupsCellViewModelFactory
import com.ivanovsky.passnotes.presentation.note_editor.LaunchMode
import com.ivanovsky.passnotes.presentation.note_editor.NoteEditorArgs
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import com.ivanovsky.passnotes.util.toUUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class GroupsViewModel(
    private val interactor: GroupsInteractor,
    private val errorInteractor: ErrorInteractor,
    private val observerBus: ObserverBus,
    private val resourceProvider: ResourceProvider,
    private val cellModelFactory: GroupsCellModelFactory,
    private val statusCellModelFactory: DatabaseStatusCellModelFactory,
    private val cellViewModelFactory: GroupsCellViewModelFactory,
    private val selectionHolder: SelectionHolder,
    private val router: Router
) : BaseScreenViewModel(),
    ObserverBus.GroupDataSetObserver,
    ObserverBus.NoteDataSetChanged,
    ObserverBus.NoteContentObserver,
    ObserverBus.DatabaseCloseObserver,
    ObserverBus.DatabaseStatusObserver {

    val viewTypes = ViewModelTypes()
        .add(NoteCellViewModel::class, R.layout.grid_cell_note)
        .add(GroupCellViewModel::class, R.layout.grid_cell_group)

    val screenStateHandler = DefaultScreenStateHandler()
    val screenState = MutableLiveData(ScreenState.notInitialized())

    val statusViewModel = cellViewModelFactory.createCellViewModel(
        model = statusCellModelFactory.createDefaultStatusCellModel(),
        eventProvider = eventProvider
    ) as DatabaseStatusCellViewModel

    val optionPanelViewModel = cellViewModelFactory.createCellViewModel(
        model = cellModelFactory.createDefaultOptionPanelCellModel(),
        eventProvider = eventProvider
    ) as OptionPanelCellViewModel

    val screenTitle = MutableLiveData(EMPTY)
    val isMenuVisible = MutableLiveData(false)
    val showToastEvent = SingleLiveEvent<String>()
    val showNewEntryDialogEvent = SingleLiveEvent<List<Template>>()
    val showGroupActionsDialogEvent = SingleLiveEvent<Group>()
    val showNoteActionsDialogEvent = SingleLiveEvent<Note>()
    val showRemoveConfirmationDialogEvent = SingleLiveEvent<Pair<Group?, Note?>>()
    val showAddTemplatesDialogEvent = SingleLiveEvent<Unit>()

    private var currentDataItems: List<GroupsInteractor.Item>? = null
    private var rootGroupUid: UUID? = null
    private var groupUid: UUID? = null
    private var templates: List<Template>? = null
    private var args: GroupsArgs? = null

    init {
        observerBus.register(this)
        subscribeToEvents()
    }

    override fun onCleared() {
        super.onCleared()
        observerBus.unregister(this)
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
        router.backTo(UnlockScreen())
    }

    override fun onDatabaseStatusChanged(status: DatabaseStatus) {
        updateStatusViewModel(status)
    }

    fun start(args: GroupsArgs) {
        this.args = args
        this.groupUid = args.groupUid

        if (groupUid == null) {
            screenTitle.value = resourceProvider.getString(R.string.groups)
        }

        loadData()
    }

    fun loadData() {
        showLoading()

        viewModelScope.launch {
            templates = interactor.getTemplates()

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
                    screenTitle.value = group.obj.title ?: ""
                }
            }

            val status = interactor.getDatabaseStatus()

            if (data.isSucceededOrDeferred) {
                val dataItems = data.obj
                currentDataItems = dataItems

                if (dataItems.isNotEmpty()) {
                    val models = cellModelFactory.createCellModels(dataItems)
                    val viewModels =
                        cellViewModelFactory.createCellViewModels(models, eventProvider)
                    setCellElements(viewModels)
                    screenState.value = ScreenState.data()
                } else {
                    val emptyText = resourceProvider.getString(R.string.no_items)
                    screenState.value = ScreenState.empty(emptyText)
                }

                if (status.isSucceededOrDeferred) {
                    updateStatusViewModel(status.obj)
                }

                showMenu()
            } else {
                val message = errorInteractor.processAndGetMessage(data.error)
                screenState.value = ScreenState.error(message)
            }

            updateOptionPanelViewModel(
                isVisible = selectionHolder.hasSelection() && data.isSucceededOrDeferred
            )
        }
    }

    fun onAddButtonClicked() {
        showNewEntryDialogEvent.call(templates ?: emptyList())
    }

    fun onCreateNewGroupClicked() {
        val currentGroupUid = getCurrentGroupUid() ?: return

        router.navigateTo(GroupScreen(currentGroupUid))
    }

    fun onCreateNewNoteClicked() {
        val currentGroupUid = getCurrentGroupUid() ?: return

        router.navigateTo(
            NoteEditorScreen(
                NoteEditorArgs(
                    launchMode = LaunchMode.NEW,
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
                    launchMode = LaunchMode.NEW,
                    groupUid = currentGroupUid,
                    template = template,
                    title = resourceProvider.getString(R.string.new_note)
                )
            )
        )
    }

    fun onEditGroupClicked(group: Group) {
        throw RuntimeException("Not implemented") // TODO: implement
    }

    fun onRemoveGroupClicked(group: Group) {
        showRemoveConfirmationDialogEvent.call(Pair(group, null))
    }

    fun onCutGroupClicked(group: Group) {
        val groupUid = group.uid ?: return
        val currentGroupUid = getCurrentGroupUid() ?: return

        selectionHolder.select(
            action = ActionType.CUT,
            selection = SelectionItem(
                uid = groupUid,
                parentUid = currentGroupUid,
                type = SelectionItemType.GROUP_UID
            )
        )

        updateOptionPanelViewModel(isVisible = true)
    }

    fun onEditNoteClicked(note: Note) {
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

        updateOptionPanelViewModel(isVisible = true)
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
        val isCloseDatabase = args?.isCloseDatabaseOnExit ?: return

        if (isCloseDatabase) {
            interactor.closeDatabase()
        }
        router.exit()
    }

    fun onLockButtonClicked() {
        interactor.closeDatabase()
        router.backTo(UnlockScreen())
    }

    fun onAddTemplatesClicked() {
        showAddTemplatesDialogEvent.call()
    }

    fun onAddTemplatesConfirmed() {
        showLoading()

        viewModelScope.launch {
            val isAdded = interactor.addTemplates()

            if (isAdded.isSucceededOrDeferred) {
                screenState.value = ScreenState.data()
                showToastEvent.call(resourceProvider.getString(R.string.successfully_added))

                showMenu()
            } else {
                val message = errorInteractor.processAndGetMessage(isAdded.error)
                screenState.value = ScreenState.error(message)
            }
        }
    }

    fun onSearchButtonClicked() = router.navigateTo(SearchScreen())

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
                GroupsArgs(
                    groupUid = groupUid,
                    isCloseDatabaseOnExit = false
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
            if (item is GroupsInteractor.GroupItem) {
                item.group.uid == groupUid
            } else {
                false
            }
        } as? GroupsInteractor.GroupItem)?.group
    }

    private fun onNoteClicked(noteUid: UUID) {
        router.navigateTo(NoteScreen(noteUid))
    }

    private fun onNoteLongClicked(noteUid: UUID) {
        val note = findNoteInItems(noteUid) ?: return

        showNoteActionsDialogEvent.call(note)
    }

    private fun findNoteInItems(noteUid: UUID): Note? {
        return (currentDataItems?.firstOrNull { item ->
            if (item is GroupsInteractor.NoteItem) {
                item.note.uid == noteUid
            } else {
                false
            }
        } as? GroupsInteractor.NoteItem)?.note
    }

    private fun getCurrentGroupUid(): UUID? {
        return when {
            groupUid != null -> groupUid
            rootGroupUid != null -> rootGroupUid
            else -> null
        }
    }

    private fun removeGroup(groupUid: UUID) {
        showLoading()

        viewModelScope.launch {
            val removeResult = withContext(Dispatchers.Default) {
                interactor.removeGroup(groupUid)
            }

            if (removeResult.isSucceededOrDeferred) {
                showToastEvent.call(resourceProvider.getString(R.string.successfully_removed))

                showMenu()
            } else {
                val message = errorInteractor.processAndGetMessage(removeResult.error)
                screenState.value = ScreenState.error(message)
            }
        }
    }

    private fun removeNote(groupUid: UUID, noteUid: UUID) {
        showLoading()

        viewModelScope.launch {
            val removeResult = withContext(Dispatchers.Default) {
                interactor.removeNote(groupUid, noteUid)
            }

            if (removeResult.isSucceededOrDeferred) {
                showToastEvent.call(resourceProvider.getString(R.string.successfully_removed))

                showMenu()
            } else {
                val message = errorInteractor.processAndGetMessage(removeResult.error)
                screenState.value = ScreenState.error(message)
            }
        }
    }

    private fun hideStatusCell() {
        updateStatusViewModel(DatabaseStatus.NORMAL)
    }

    private fun updateStatusViewModel(status: DatabaseStatus) {
        statusViewModel.setModel(statusCellModelFactory.createStatusCellModel(status))
    }

    private fun updateOptionPanelViewModel(isVisible: Boolean) {
        val model = if (isVisible) {
            cellModelFactory.createPasteOptionPanelCellModel()
        } else {
            cellModelFactory.createHiddenOptionPanelCellModel()
        }

        optionPanelViewModel.setModel(model)
    }

    private fun onPositiveOptionSelected() {
        val currentGroupUid = getCurrentGroupUid() ?: return
        val selection = selectionHolder.getSelection() ?: return
        val action = selectionHolder.getAction() ?: return

        if (selection.parentUid == currentGroupUid) {
            showToastEvent.value = resourceProvider.getString(R.string.selected_item_is_already_here)
            return
        }

        showLoading()

        viewModelScope.launch {
            val actionResult = interactor.doActionOnSelection(
                selectedGroupUid = currentGroupUid,
                action = action,
                selection = selection
            )
            selectionHolder.clear()

            if (actionResult.isSucceededOrDeferred) {
                showToastEvent.call(resourceProvider.getString(R.string.successfully))
                screenState.value = ScreenState.data()
                showMenu()
            } else {
                val message = errorInteractor.processAndGetMessage(actionResult.error)
                screenState.value = ScreenState.error(message)
            }
        }
    }

    private fun onNegativeOptionSelected() {
        selectionHolder.clear()
        updateOptionPanelViewModel(isVisible = false)
    }

    private fun hideMenu() {
        isMenuVisible.value = false
    }

    private fun showMenu() {
        isMenuVisible.value = true
    }

    private fun showLoading() {
        hideMenu()
        hideStatusCell()
        updateOptionPanelViewModel(isVisible = false)
    }
}