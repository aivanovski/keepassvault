package com.ivanovsky.passnotes.presentation.groups

import android.os.Build
import androidx.annotation.IdRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.crypto.biometric.BiometricEncoder
import com.ivanovsky.passnotes.data.entity.EncryptedDatabaseEntry
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.KeyType
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_UID_IS_NULL
import com.ivanovsky.passnotes.data.entity.OperationError.newGenericError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.Template
import com.ivanovsky.passnotes.data.entity.UsedFile
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseKey
import com.ivanovsky.passnotes.data.repository.keepass.PasswordKeepassKey
import com.ivanovsky.passnotes.data.repository.settings.OnSettingsChangeListener
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.data.repository.settings.SettingsImpl
import com.ivanovsky.passnotes.domain.DatabaseLockInteractor
import com.ivanovsky.passnotes.domain.PermissionHelper
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.biometric.BiometricInteractor
import com.ivanovsky.passnotes.domain.entity.SelectionItem
import com.ivanovsky.passnotes.domain.entity.SelectionItemType
import com.ivanovsky.passnotes.domain.entity.SystemPermission
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.SelectionHolder
import com.ivanovsky.passnotes.domain.interactor.SelectionHolder.ActionType
import com.ivanovsky.passnotes.domain.interactor.groups.GroupsInteractor
import com.ivanovsky.passnotes.domain.interactor.syncState.SyncStateInteractor
import com.ivanovsky.passnotes.extensions.getOrThrow
import com.ivanovsky.passnotes.extensions.isRequireSynchronization
import com.ivanovsky.passnotes.extensions.mapError
import com.ivanovsky.passnotes.extensions.mapWithObject
import com.ivanovsky.passnotes.injection.GlobalInjector
import com.ivanovsky.passnotes.presentation.ApplicationLaunchMode
import com.ivanovsky.passnotes.presentation.ApplicationLaunchMode.AUTOFILL_SELECTION
import com.ivanovsky.passnotes.presentation.Screens
import com.ivanovsky.passnotes.presentation.Screens.EnterDbCredentialsScreen
import com.ivanovsky.passnotes.presentation.Screens.GroupEditorScreen
import com.ivanovsky.passnotes.presentation.Screens.MainSettingsScreen
import com.ivanovsky.passnotes.presentation.Screens.NoteEditorScreen
import com.ivanovsky.passnotes.presentation.Screens.NoteScreen
import com.ivanovsky.passnotes.presentation.Screens.StorageListScreen
import com.ivanovsky.passnotes.presentation.Screens.UnlockScreen
import com.ivanovsky.passnotes.presentation.core.BackNavigationIcon
import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.DefaultScreenStateHandler
import com.ivanovsky.passnotes.presentation.core.ScreenDisplayingType
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.ViewModelTypes
import com.ivanovsky.passnotes.presentation.core.dialog.sortAndView.ScreenType
import com.ivanovsky.passnotes.presentation.core.dialog.sortAndView.SortAndViewDialogArgs
import com.ivanovsky.passnotes.presentation.core.event.EventProviderImpl
import com.ivanovsky.passnotes.presentation.core.event.LockScreenLiveEvent
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import com.ivanovsky.passnotes.presentation.core.menu.ScreenMenuItem
import com.ivanovsky.passnotes.presentation.core.model.NavigationPanelCellModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.DividerCellViewModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.GroupCellViewModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.NavigationPanelCellViewModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.NoteCellViewModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.OptionPanelCellViewModel
import com.ivanovsky.passnotes.presentation.core.viewmodel.SpaceCellViewModel
import com.ivanovsky.passnotes.presentation.diffViewer.DiffViewerScreenArgs
import com.ivanovsky.passnotes.presentation.diffViewer.model.DiffEntity
import com.ivanovsky.passnotes.presentation.enterDbCredentials.EnterDbCredentialsScreenArgs
import com.ivanovsky.passnotes.presentation.groupEditor.GroupEditorArgs
import com.ivanovsky.passnotes.presentation.groups.factory.GroupsCellModelFactory
import com.ivanovsky.passnotes.presentation.groups.factory.GroupsCellViewModelFactory
import com.ivanovsky.passnotes.presentation.groups.model.CellsData
import com.ivanovsky.passnotes.presentation.groups.model.NavigationStackItem
import com.ivanovsky.passnotes.presentation.note.NoteScreenArgs
import com.ivanovsky.passnotes.presentation.noteEditor.NoteEditorArgs
import com.ivanovsky.passnotes.presentation.noteEditor.NoteEditorMode
import com.ivanovsky.passnotes.presentation.storagelist.Action
import com.ivanovsky.passnotes.presentation.storagelist.StorageListArgs
import com.ivanovsky.passnotes.presentation.syncState.factory.SyncStateCellModelFactory
import com.ivanovsky.passnotes.presentation.syncState.viewmodel.SyncStateViewModel
import com.ivanovsky.passnotes.presentation.unlock.UnlockScreenArgs
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import com.ivanovsky.passnotes.util.toUUID
import java.util.Deque
import java.util.LinkedList
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.parameter.parametersOf

class GroupsViewModel(
    private val interactor: GroupsInteractor,
    syncStateInteractor: SyncStateInteractor,
    private val biometricInteractor: BiometricInteractor,
    private val errorInteractor: ErrorInteractor,
    lockInteractor: DatabaseLockInteractor,
    private val observerBus: ObserverBus,
    private val settings: Settings,
    private val resourceProvider: ResourceProvider,
    private val cellModelFactory: GroupsCellModelFactory,
    private val cellViewModelFactory: GroupsCellViewModelFactory,
    syncStateModelFactory: SyncStateCellModelFactory,
    private val persmissionHelper: PermissionHelper,
    private val selectionHolder: SelectionHolder,
    private val router: Router,
    private val args: GroupsScreenArgs
) : ViewModel(),
    ObserverBus.GroupDataSetObserver,
    ObserverBus.NoteDataSetChanged,
    ObserverBus.NoteContentObserver,
    ObserverBus.DatabaseDataSetObserver,
    OnSettingsChangeListener {

    val viewTypes = ViewModelTypes()
        .add(NoteCellViewModel::class, R.layout.cell_note)
        .add(GroupCellViewModel::class, R.layout.cell_group)
        .add(SpaceCellViewModel::class, R.layout.cell_space)
        .add(DividerCellViewModel::class, R.layout.cell_divider)

    val screenStateHandler = DefaultScreenStateHandler()
    val screenState = MutableLiveData(ScreenState.notInitialized())
    val eventProvider = EventProviderImpl()

    val navigationPanelViewModel = NavigationPanelCellViewModel(
        initModel = NavigationPanelCellModel(
            items = emptyList()
        ),
        eventProvider = eventProvider
    )

    val syncStateViewModel = SyncStateViewModel(
        interactor = syncStateInteractor,
        modelFactory = syncStateModelFactory,
        resourceProvider = resourceProvider,
        observerBus = observerBus,
        initModel = syncStateInteractor.cache.getValue()
            ?: syncStateModelFactory.createHiddenState()
    )
    val showResolveConflictDialogEvent = syncStateViewModel.showResolveConflictDialogEvent
    val showMessageDialogEvent = syncStateViewModel.showMessageDialogEvent

    val optionPanelViewModel = cellViewModelFactory.createCellViewModel(
        model = cellModelFactory.createOptionPanelCellModel(OptionPanelState.HIDDEN),
        eventProvider = eventProvider
    ) as OptionPanelCellViewModel

    val cellViewModels = MutableLiveData<CellsData>()
    val searchQuery = MutableLiveData(EMPTY)
    val screenTitle = MutableLiveData(EMPTY)
    val visibleMenuItems = MutableLiveData<List<GroupsMenuItem>>(emptyList())
    val isFabButtonVisible = MutableLiveData(false)
    val isSearchQueryVisible = MutableLiveData(false)
    val isNavigationPanelVisible = MutableLiveData(false)
    val showToastEvent = SingleLiveEvent<String>()
    val showNewEntryDialogEvent = SingleLiveEvent<List<Template>>()
    val showGroupActionsDialogEvent = SingleLiveEvent<Group>()
    val showNoteActionsDialogEvent = SingleLiveEvent<Note>()
    val showRemoveConfirmationDialogEvent = SingleLiveEvent<Pair<Group?, Note?>>()
    val showAddTemplatesDialogEvent = SingleLiveEvent<Unit>()
    val showLockNotificationDialogEvent = SingleLiveEvent<Unit>()
    val requestPermissionEvent = SingleLiveEvent<SystemPermission>()
    val finishActivityEvent = SingleLiveEvent<Unit>()
    val showSortAndViewDialogEvent = SingleLiveEvent<SortAndViewDialogArgs>()
    val showBiometricSetupDialog = SingleLiveEvent<BiometricEncoder>()
    val lockScreenEvent = LockScreenLiveEvent(observerBus, lockInteractor)
    val backIcon = MutableLiveData<BackNavigationIcon>(BackNavigationIcon.Arrow)
    val isKeyboardVisibleEvent = SingleLiveEvent<Boolean>()

    private var rootGroup: Group? = null
    private var currentGroup: Group? = null
    private var currentGroupUid: UUID? = null
    private val navigationStack: Deque<NavigationStackItem> = LinkedList()
    private var dbUsedFile: UsedFile? = null
    private var templates: List<Template>? = null
    private var isAutofillSavingCancelled = false
    private var isSearchModeEnabled = false
    private var isFillNavigationStack = false
    private var currentEntries: List<EncryptedDatabaseEntry> = emptyList()
    private var searchableEntries: List<EncryptedDatabaseEntry>? = null
    private var navigationPanelGroups: List<Group> = emptyList()
    private var loadDataJob: Job? = null

    init {
        observerBus.register(this)
        settings.register(this)
        subscribeToEvents()
        syncStateViewModel.onAttach()

        if (args.groupUid == null) {
            navigationStack.push(NavigationStackItem.RootGroup)
        } else {
            navigationStack.push(NavigationStackItem.Group(args.groupUid))
            isFillNavigationStack = true
        }
        currentGroupUid = args.groupUid

        if (args.isSearchModeEnabled) {
            enableSearchMode()
        }

        if (args.groupUid == null) {
            syncStateInteractor.cache.setValue(null)
        }
    }

    override fun onCleared() {
        super.onCleared()
        observerBus.unregister(this)
        settings.register(this)
        syncStateViewModel.onDetach()
    }

    override fun onGroupDataSetChanged() {
        searchableEntries = null
        loadData()
    }

    override fun onNoteDataSetChanged(groupUid: UUID) {
        searchableEntries = null
        if (groupUid == getCurrentGroupUid()) {
            loadData()
        }
    }

    override fun onNoteContentChanged(groupUid: UUID, oldNoteUid: UUID, newNoteUid: UUID) {
        searchableEntries = null
        if (groupUid == getCurrentGroupUid()) {
            loadData()
        }
    }

    override fun onSettingsChanged(pref: SettingsImpl.Pref) {
        if (pref == SettingsImpl.Pref.SORT_TYPE ||
            pref == SettingsImpl.Pref.SORT_DIRECTION ||
            pref == SettingsImpl.Pref.IS_GROUPS_AT_START_ENABLED
        ) {
            loadData()
        }
    }

    override fun onDatabaseDataSetChanged() {
        searchableEntries = null
        templates = null
        loadData()
    }

    fun start() {
        showLockNotificationDialogIfNecessary()

        syncStateViewModel.start()
        loadData()
    }

    fun loadData(
        isResetScroll: Boolean = false
    ) {
        setScreenState(ScreenState.loading())

        loadDataJob?.cancel()
        loadDataJob = viewModelScope.launch {
            if (loadTemplates().isFailed) {
                return@launch
            }
            if (loadRootGroup().isFailed) {
                return@launch
            }

            if (currentGroupUid == null) {
                currentGroupUid = rootGroup?.uid
            }

            if (loadCurrentGroup().isFailed) {
                return@launch
            }
            if (loadUsedFile().isFailed) {
                return@launch
            }
            if (loadNavigationPanelData().isFailed) {
                return@launch
            }

            if (isFillNavigationStack) {
                fillNavigationStack()
                isFillNavigationStack = false
            }

            val getEntriesResult = when {
                isSearchModeEnabled -> loadSearchEntries(searchQuery.value ?: EMPTY)
                currentGroupUid == null -> interactor.getRootEntries()
                else -> interactor.getGroupEntries(currentGroupUid ?: EMPTY_UUID)
            }

            if (getEntriesResult.isSucceededOrDeferred) {
                currentEntries = if (!isSearchModeEnabled) {
                    interactor.sortData(getEntriesResult.getOrThrow())
                } else {
                    getEntriesResult.getOrThrow()
                }

                if (currentEntries.isNotEmpty()) {
                    cellViewModels.value = CellsData(
                        isResetScroll = isResetScroll,
                        viewModels = createCellViewModels(currentEntries)
                    )
                    setScreenState(ScreenState.data())
                } else {
                    val emptyText = if (isSearchModeEnabled) {
                        resourceProvider.getString(R.string.no_search_results)
                    } else {
                        resourceProvider.getString(R.string.no_items)
                    }

                    setScreenState(ScreenState.empty(emptyText))
                }

                visibleMenuItems.value = getVisibleMenuItems()
            } else {
                setErrorState(getEntriesResult.error)
            }

            updateOptionPanelState()

            loadDataJob = null
        }
    }

    fun onAddButtonClicked() {
        showNewEntryDialogEvent.call(templates ?: emptyList())
    }

    fun onCreateNewGroupClicked() {
        val currentGroupUid = getCurrentGroupUid() ?: return

        router.navigateTo(
            GroupEditorScreen(
                GroupEditorArgs.newGroupArgs(
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
                GroupEditorArgs.editGroupArgs(
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

    fun navigateBack() {
        if (isSearchModeEnabled) {
            if (navigationStack.peek() is NavigationStackItem.Search) {
                navigationStack.pop()
            }

            disableSearchMode()
            loadData(isResetScroll = true)
            return
        }

        if (navigationStack.size > 1) {
            navigationStack.pop()

            when (val nextItem = navigationStack.peek()) {
                is NavigationStackItem.RootGroup -> {
                    currentGroupUid = rootGroup?.uid
                }

                is NavigationStackItem.Group -> {
                    currentGroupUid = nextItem.groupUid
                }

                is NavigationStackItem.Search -> {
                    currentGroupUid = nextItem.groupUid

                    searchQuery.value = nextItem.query
                    if (!isSearchModeEnabled) {
                        enableSearchMode()
                    }
                }
            }

            loadData(isResetScroll = true)
        } else {
            if (args.isCloseDatabaseOnExit) {
                interactor.lockDatabase()
            }

            if (args.appMode == AUTOFILL_SELECTION && currentGroupUid == rootGroup?.uid) {
                finishActivityEvent.call(Unit)
            } else {
                router.exit()
            }
        }
    }

    fun onLockButtonClicked() {
        interactor.lockDatabase()
        when (args.appMode) {
            AUTOFILL_SELECTION -> {
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

    fun onAddTemplatesClicked() {
        showAddTemplatesDialogEvent.call(Unit)
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
        searchQuery.value = EMPTY
        enableSearchMode()
    }

    fun onSortAndViewButtonClicked() {
        val dialogArgs = if (isSearchModeEnabled) {
            SortAndViewDialogArgs(ScreenType.SEARCH)
        } else {
            SortAndViewDialogArgs(ScreenType.GROUPS)
        }

        showSortAndViewDialogEvent.call(dialogArgs)
    }

    fun onSettingsButtonClicked() = router.navigateTo(MainSettingsScreen())

    fun onEnableBiometricUnlockButtonClicked() {
        if (isBiometricUnlockAllowedForDatabase()) {
            showBiometricSetupDialog.call(biometricInteractor.getCipherForEncryption())
        }
    }

    fun onBiometricSetupSuccess(encoder: BiometricEncoder) {
        val usedFileId = dbUsedFile?.id ?: return

        setScreenState(ScreenState.loading())

        viewModelScope.launch {
            val getKeyResult = interactor.getDatabaseKey()
            if (getKeyResult.isFailed) {
                setErrorPanelState(getKeyResult.error)
                return@launch
            }

            val password = (getKeyResult.obj as PasswordKeepassKey).password

            val encryptPasswordResult = interactor.encodePasswordAndStoreData(
                encoder,
                password,
                usedFileId
            )
            if (encryptPasswordResult.isFailed) {
                setErrorPanelState(encryptPasswordResult.error)
                return@launch
            }

            loadData()
        }
    }

    fun onDisableBiometricUnlockButtonClicked() {
        val usedFileId = dbUsedFile?.id ?: return

        setScreenState(ScreenState.loading())

        viewModelScope.launch {
            val removeResult = interactor.removeBiometricData(usedFileId)
            if (removeResult.isFailed) {
                setErrorPanelState(removeResult.error)
                return@launch
            }

            loadData()
        }
    }

    fun onSynchronizeButtonClicked() {
        syncStateViewModel.synchronize()
    }

    fun onSearchQueryChanged(query: String) {
        loadData(isResetScroll = true)
    }

    fun onDiffWithButtonClicked() {
        router.setResultListener(StorageListScreen.RESULT_KEY) { file ->
            if (file is FileDescriptor) {
                onDiffFileSelected(file)
            }
        }
        router.navigateTo(
            StorageListScreen(
                args = StorageListArgs(Action.PICK_FILE)
            )
        )
    }

    fun onRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissionEvent.call(SystemPermission.NOTIFICATION)
        }
    }

    fun onNotificationPermissionResult(isGranted: Boolean) {
        interactor.invalidateLockNotification()
    }

    fun onLockNotificationDialogDisabled() {
        settings.isLockNotificationVisible = false
        settings.isLockNotificationDialogEnabled = false
    }

    private fun onDiffFileSelected(file: FileDescriptor) {
        router.setResultListener(EnterDbCredentialsScreen.RESULT_KEY) { key ->
            if (key is EncryptedDatabaseKey) {
                onDiffFileUnlocked(key, file)
            }
        }
        router.navigateTo(
            EnterDbCredentialsScreen(
                EnterDbCredentialsScreenArgs(
                    file = file
                )
            )
        )
    }

    private fun onDiffFileUnlocked(key: EncryptedDatabaseKey, file: FileDescriptor) {
        router.navigateTo(
            Screens.DiffViewerScreen(
                DiffViewerScreenArgs(
                    left = DiffEntity.OpenedDatabase,
                    right = DiffEntity.File(
                        key = key,
                        file = file
                    ),
                    isHoldDatabaseInteraction = true
                )
            )
        )
    }

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

                event.containsKey(NavigationPanelCellViewModel.ITEM_CLICK_EVENT) -> {
                    event.getInt(NavigationPanelCellViewModel.ITEM_CLICK_EVENT)?.let { index ->
                        onNavigationPanelClicked(index)
                    }
                }
            }
        }
    }

    private fun onGroupClicked(groupUid: UUID) {
        if (isSearchModeEnabled) {
            val currentQuery = searchQuery.value ?: EMPTY
            navigationStack.push(NavigationStackItem.Search(currentQuery, currentGroupUid))
            navigationStack.push(NavigationStackItem.Group(groupUid))
            disableSearchMode()
        } else {
            navigationStack.push(NavigationStackItem.Group(groupUid))
        }

        currentGroupUid = groupUid

        loadData(isResetScroll = true)
    }

    private fun onGroupLongClicked(groupUid: UUID) {
        val group = findGroupInItems(groupUid) ?: return

        showGroupActionsDialogEvent.call(group)
    }

    private fun onNavigationPanelClicked(index: Int) {
        val groupUid = navigationPanelGroups.getOrNull(index)?.uid ?: return

        if (currentGroupUid != groupUid) {
            cleanNavigationStackUntil(groupUid)

            val isRootGroup = (groupUid == rootGroup?.uid)
            if (!isRootGroup) {
                navigationStack.push(NavigationStackItem.Group(groupUid))
            }

            currentGroupUid = groupUid

            loadData(isResetScroll = true)
        }
    }

    private fun cleanNavigationStackUntil(groupUid: UUID) {
        if (navigationStack.size == 1) {
            return
        }

        val isRootGroup = (groupUid == rootGroup?.uid)
        if (isRootGroup) {
            while (navigationStack.size > 1) {
                navigationStack.pop()
            }
            return
        }

        val hasGroup = navigationStack.any { item ->
            item is NavigationStackItem.Group && item.groupUid == groupUid
        }
        if (!hasGroup) {
            return
        }

        while (true) {
            val stackItem = navigationStack.peek()
            if (stackItem is NavigationStackItem.Group && stackItem.groupUid == groupUid) {
                navigationStack.pop()
                break
            } else {
                navigationStack.pop()
            }
        }
    }

    private fun fillNavigationStack() {
        val isRootGroup = (currentGroupUid == rootGroup?.uid)
        if (isRootGroup) {
            return
        }

        navigationStack.clear()

        for (parent in navigationPanelGroups) {
            if (parent.uid == rootGroup?.uid) {
                navigationStack.push(NavigationStackItem.RootGroup)
            } else {
                navigationStack.push(NavigationStackItem.Group(parent.uid))
            }
        }
    }

    private fun findGroupInItems(groupUid: UUID): Group? {
        return currentEntries.firstOrNull { item ->
            if (item is Group) {
                item.uid == groupUid
            } else {
                false
            }
        } as? Group
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
        return currentEntries.firstOrNull { item ->
            if (item is Note) {
                item.uid == noteUid
            } else {
                false
            }
        } as? Note
    }

    private fun getCurrentGroupUid(): UUID? {
        return when {
            currentGroupUid != null -> currentGroupUid
            else -> rootGroup?.uid
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
            screenState.isDisplayingLoading || screenState.isDisplayingError -> {
                OptionPanelState.HIDDEN
            }

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

    private fun enableSearchMode() {
        isSearchModeEnabled = true

        isSearchQueryVisible.value = isSearchModeEnabled
        backIcon.value = getBackIconInternal()
        isKeyboardVisibleEvent.value = true

        setScreenState(
            ScreenState.empty(
                emptyText = resourceProvider.getString(R.string.no_search_results)
            )
        )

        if (searchableEntries == null) {
            loadAllSearchableEntries()
        }
    }

    private fun disableSearchMode() {
        isSearchModeEnabled = false
        isSearchQueryVisible.value = isSearchModeEnabled
        backIcon.value = getBackIconInternal()
        isKeyboardVisibleEvent.value = false
        screenTitle.value = currentGroup?.title ?: EMPTY
        visibleMenuItems.value = getVisibleMenuItems()
    }

    private fun loadAllSearchableEntries() {
        viewModelScope.launch {
            val getAllEntries = interactor.getAllSearchableEntries(
                isRespectAutotypeProperty = (args.appMode == AUTOFILL_SELECTION)
            )
            if (getAllEntries.isFailed) {
                setScreenState(
                    ScreenState.error(
                        errorText = errorInteractor.processAndGetMessage(getAllEntries.error)
                    )
                )
                return@launch
            }

            searchableEntries = getAllEntries.getOrThrow()
        }
    }

    private suspend fun loadSearchEntries(
        query: String
    ): OperationResult<List<EncryptedDatabaseEntry>> {
        var allEntries = searchableEntries
        if (allEntries == null) {
            val getAllEntriesResult = interactor.getAllSearchableEntries(
                isRespectAutotypeProperty = (args.appMode == AUTOFILL_SELECTION)
            )
            if (getAllEntriesResult.isFailed) {
                setErrorPanelState(getAllEntriesResult.error)
                return getAllEntriesResult
            }

            searchableEntries = getAllEntriesResult.getOrThrow()
            allEntries = getAllEntriesResult.getOrThrow()
        }

        if (query.isNotEmpty()) {
            delay(SEARCH_DELAY)
        }

        val searchEntries = if (query.isNotEmpty()) {
            interactor.filterEntries(
                entries = allEntries,
                query = query
            )
        } else {
            emptyList()
        }

        return OperationResult.success(searchEntries)
    }

    private suspend fun loadTemplates(): OperationResult<Unit> {
        if (templates == null) {
            val getTemplatesResult = interactor.getTemplates()
            if (getTemplatesResult.isFailed) {
                setErrorPanelState(getTemplatesResult.error)
                return getTemplatesResult.mapError()
            }

            templates = getTemplatesResult.getOrThrow()
        }

        return OperationResult.success(Unit)
    }

    private suspend fun loadRootGroup(): OperationResult<Unit> {
        if (rootGroup == null) {
            val getRootGroupResult = interactor.getRootGroup()
            if (getRootGroupResult.isFailed) {
                setErrorState(getRootGroupResult.error)
                return getRootGroupResult.mapError()
            }

            rootGroup = getRootGroupResult.getOrThrow()
        }

        return OperationResult.success(Unit)
    }

    private suspend fun loadUsedFile(): OperationResult<Unit> {
        val getUsedFileResult = interactor.getDatabaseUsedFile()
        if (getUsedFileResult.isFailed) {
            setErrorState(getUsedFileResult.error)
            return getUsedFileResult.mapError()
        }

        dbUsedFile = getUsedFileResult.obj

        return getUsedFileResult.mapWithObject(Unit)
    }

    private suspend fun loadNavigationPanelData(): OperationResult<Unit> {
        val groupUid = currentGroupUid
        if (groupUid == null) {
            val error = newGenericError(MESSAGE_UID_IS_NULL)
            setErrorState(error)
            return OperationResult.error(error)
        }

        val getParentsResult = interactor.getAllParents(groupUid)
        if (getParentsResult.isFailed) {
            setErrorState(getParentsResult.error)
            return getParentsResult.mapError()
        }

        navigationPanelGroups = getParentsResult.getOrThrow()

        val items = navigationPanelGroups.map { group -> group.title }

        navigationPanelViewModel.setModel(
            NavigationPanelCellModel(
                items = items
            )
        )

        return OperationResult.success(Unit)
    }

    private suspend fun loadCurrentGroup(): OperationResult<Unit> {
        if (currentGroupUid == rootGroup?.uid) {
            currentGroup = rootGroup
            return OperationResult.success(Unit)
        }

        val groupUid = currentGroupUid
        if (groupUid == null) {
            val error = newGenericError(MESSAGE_UID_IS_NULL)
            setErrorState(error)
            return OperationResult.error(error)
        }

        val getGroupResult = interactor.getGroup(groupUid)
        if (getGroupResult.isFailed) {
            setErrorState(getGroupResult.error)
            return getGroupResult.mapError()
        }

        currentGroup = getGroupResult.getOrThrow()
        return OperationResult.success(Unit)
    }

    private fun showLockNotificationDialogIfNecessary() {
        if (settings.isLockNotificationDialogEnabled &&
            !persmissionHelper.isPermissionGranted(SystemPermission.NOTIFICATION)
        ) {
            showLockNotificationDialogEvent.call(Unit)
        }
    }

    private fun createCellViewModels(
        data: List<EncryptedDatabaseEntry>
    ): List<BaseCellViewModel> {
        val models = cellModelFactory.createCellModels(data)
        return cellViewModelFactory.createCellViewModels(models, eventProvider)
    }

    private fun setErrorState(error: OperationError) {
        setScreenState(
            ScreenState.error(
                errorText = errorInteractor.processAndGetMessage(error)
            )
        )
    }

    private fun setErrorPanelState(error: OperationError) {
        setScreenState(
            ScreenState.dataWithError(
                errorText = errorInteractor.processAndGetMessage(error)
            )
        )
    }

    private fun setScreenState(state: ScreenState) {
        screenState.value = state
        isFabButtonVisible.value = getFabButtonVisibility()
        isNavigationPanelVisible.value = getNavigationPanelVisibility()
        screenTitle.value = getScreenTitleInternal()

        when (state.screenDisplayingType) {
            ScreenDisplayingType.LOADING -> {
                visibleMenuItems.value = getVisibleMenuItems()
                updateOptionPanelState()
            }

            else -> {}
        }
    }

    private fun getBackIconInternal(): BackNavigationIcon {
        return if (isSearchModeEnabled) {
            BackNavigationIcon.Icon(R.drawable.ic_close_24dp)
        } else {
            BackNavigationIcon.Arrow
        }
    }

    private fun getScreenTitleInternal(): String {
        return if (isSearchModeEnabled) {
            resourceProvider.getString(R.string.search)
        } else {
            currentGroup?.title ?: EMPTY
        }
    }

    private fun getFabButtonVisibility(): Boolean {
        val screenState = this.screenState.value ?: return false

        return (screenState.isDisplayingData || screenState.isDisplayingEmptyState) &&
            args.appMode == ApplicationLaunchMode.NORMAL
    }

    private fun getNavigationPanelVisibility(): Boolean {
        val screenState = this.screenState.value ?: return false

        return (screenState.isDisplayingData || screenState.isDisplayingEmptyState) &&
            !isSearchModeEnabled
    }

    private fun getVisibleMenuItems(): List<GroupsMenuItem> {
        val screenState = this.screenState.value ?: return emptyList()
        val usedFile = this.dbUsedFile ?: return emptyList()

        val isShowMenu = (screenState.isDisplayingData || screenState.isDisplayingEmptyState)

        return when {
            isShowMenu && args.appMode == ApplicationLaunchMode.NORMAL -> {
                mutableListOf<GroupsMenuItem>()
                    .apply {
                        if (!isSearchModeEnabled) {
                            add(GroupsMenuItem.SEARCH)
                        }
                        add(GroupsMenuItem.LOCK)

                        if (usedFile.fsAuthority.type.isRequireSynchronization()) {
                            add(GroupsMenuItem.SYNCHRONIZE)
                        }

                        add(GroupsMenuItem.VIEW_MODE)
                        add(GroupsMenuItem.SETTINGS)
                        add(GroupsMenuItem.DIFF_WITH)

                        if (templates.isNullOrEmpty()) {
                            add(GroupsMenuItem.ADD_TEMPLATES)
                        }

                        if (isBiometricUnlockAllowedForDatabase()) {
                            if (usedFile.biometricData == null) {
                                add(GroupsMenuItem.ENABLE_BIOMETRIC_UNLOCK)
                            } else {
                                add(GroupsMenuItem.DISABLE_BIOMETRIC_UNLOCK)
                            }
                        }
                    }
            }

            isShowMenu && args.appMode == AUTOFILL_SELECTION -> {
                mutableListOf<GroupsMenuItem>()
                    .apply {
                        if (!isSearchModeEnabled) {
                            add(GroupsMenuItem.SEARCH)
                        }
                        add(GroupsMenuItem.LOCK)
                        add(GroupsMenuItem.VIEW_MODE)
                        add(GroupsMenuItem.SETTINGS)
                    }
            }

            else -> emptyList()
        }
    }

    private fun isBiometricUnlockAllowedForDatabase(): Boolean {
        return biometricInteractor.isBiometricUnlockAvailable() &&
            settings.isBiometricUnlockEnabled &&
            dbUsedFile?.keyType == KeyType.PASSWORD
    }

    enum class OptionPanelState {
        HIDDEN,
        PASTE,
        SAVE_AUTOFILL_DATA
    }

    class Factory(private val args: GroupsScreenArgs) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
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
        SETTINGS(R.id.menu_settings),
        SYNCHRONIZE(R.id.menu_synchronize),
        ENABLE_BIOMETRIC_UNLOCK(R.id.menu_enable_biometric_unlock),
        DISABLE_BIOMETRIC_UNLOCK(R.id.menu_disable_biometric_unlock),
        DIFF_WITH(R.id.menu_diff_with)
    }

    companion object {
        private val EMPTY_UUID = UUID(0, 0)
        private const val SEARCH_DELAY = 300L
    }
}