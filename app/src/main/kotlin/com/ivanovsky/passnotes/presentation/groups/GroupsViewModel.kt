package com.ivanovsky.passnotes.presentation.groups

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.Template
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.groups.GroupsInteractor
import com.ivanovsky.passnotes.presentation.core_mvvm.BaseScreenViewModel
import com.ivanovsky.passnotes.presentation.core_mvvm.DefaultScreenStateHandler
import com.ivanovsky.passnotes.presentation.core_mvvm.ScreenState
import com.ivanovsky.passnotes.presentation.core_mvvm.ViewModelTypes
import com.ivanovsky.passnotes.presentation.core_mvvm.event.SingleLiveEvent
import com.ivanovsky.passnotes.presentation.core_mvvm.viewmodels.GroupGridCellViewModel
import com.ivanovsky.passnotes.presentation.core_mvvm.viewmodels.NoteGridCellViewModel
import com.ivanovsky.passnotes.presentation.groups.factory.GroupsCellModelFactory
import com.ivanovsky.passnotes.presentation.groups.factory.GroupsCellViewModelFactory
import com.ivanovsky.passnotes.util.toUUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class GroupsViewModel(
    private val interactor: GroupsInteractor,
    private val errorInteractor: ErrorInteractor,
    private val observerBus: ObserverBus,
    private val resourceProvider: ResourceProvider,
    private val cellModelFactory: GroupsCellModelFactory,
    private val cellViewModelFactory: GroupsCellViewModelFactory
) : BaseScreenViewModel(),
    ObserverBus.GroupDataSetObserver,
    ObserverBus.NoteDataSetChanged,
    ObserverBus.NoteContentObserver {

    val viewTypes = ViewModelTypes()
        .add(NoteGridCellViewModel::class, R.layout.grid_cell_note)
        .add(GroupGridCellViewModel::class, R.layout.grid_cell_group)

    val screenStateHandler = DefaultScreenStateHandler()
    val screenState = MutableLiveData<ScreenState>(ScreenState.notInitialized())

    val toastMessage = MutableLiveData<String>()
    val showNoteScreenEvent = SingleLiveEvent<Note>()
    val showNoteListScreenEvent = SingleLiveEvent<Group>()
    val showNewGroupScreenEvent = SingleLiveEvent<UUID>()
    val showNewNoteScreenEvent = SingleLiveEvent<Pair<UUID, Template?>>()
    val showNewEntryDialogEvent = SingleLiveEvent<List<Template>>()
    val showGroupActionsDialogEvent = SingleLiveEvent<Group>()
    val showNoteActionsDialogEvent = SingleLiveEvent<Note>()
    val showRemoveConfirmationDialogEvent = SingleLiveEvent<Pair<Group?, Note?>>()
    val showEditNoteScreenEvent = SingleLiveEvent<Note>()
    val showEditGroupScreenEvent = SingleLiveEvent<Group>()

    private var currentDataItems: List<GroupsInteractor.Item>? = null
    private var rootGroupUid: UUID? = null
    private var groupUid: UUID? = null
    private var templates: List<Template>? = null

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

    fun start(groupUid: UUID?) {
        this.groupUid = groupUid

        loadData()
    }

    fun loadData() {
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
            } else {
                val message = errorInteractor.processAndGetMessage(data.error)
                screenState.value = ScreenState.error(message)
            }
        }
    }

    fun onAddButtonClicked() {
        showNewEntryDialogEvent.call(templates ?: emptyList())
    }

    fun onCreateNewGroupClicked() {
        val currentGroupUid = getCurrentGroupUid() ?: return

        showNewGroupScreenEvent.call(currentGroupUid)
    }

    fun onCreateNewNoteClicked() {
        val currentGroupUid = getCurrentGroupUid() ?: return

        showNewNoteScreenEvent.call(Pair(currentGroupUid, null))
    }

    fun onCreateNewNoteFromTemplateClicked(template: Template) {
        val currentGroupUid = getCurrentGroupUid() ?: return

        showNewNoteScreenEvent.call(Pair(currentGroupUid, template))
    }

    fun onEditGroupClicked(group: Group) {
        showEditGroupScreenEvent.call(group)
    }

    fun onRemoveGroupClicked(group: Group) {
        showRemoveConfirmationDialogEvent.call(Pair(group, null))
    }

    fun onEditNoteClicked(note: Note) {
        showEditNoteScreenEvent.call(note)
    }

    fun onRemoveNoteClicked(note: Note) {
        showRemoveConfirmationDialogEvent.call(Pair(null, note))
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

    private fun subscribeToEvents() {
        eventProvider.subscribe(this) { event ->
            when {
                event.containsKey(GroupGridCellViewModel.CLICK_EVENT) -> {
                    event.getString(GroupGridCellViewModel.CLICK_EVENT)?.toUUID()?.let {
                        onGroupClicked(it)
                    }
                }
                event.containsKey(GroupGridCellViewModel.LONG_CLICK_EVENT) -> {
                    event.getString(GroupGridCellViewModel.LONG_CLICK_EVENT)?.toUUID()?.let {
                        onGroupLongClicked(it)
                    }
                }
                event.containsKey(NoteGridCellViewModel.CLICK_EVENT) -> {
                    event.getString(NoteGridCellViewModel.CLICK_EVENT)?.toUUID()?.let {
                        onNoteClicked(it)
                    }
                }
                event.containsKey(NoteGridCellViewModel.LONG_CLICK_EVENT) -> {
                    event.getString(NoteGridCellViewModel.LONG_CLICK_EVENT)?.toUUID()?.let {
                        onNoteLongClicked(it)
                    }
                }
            }
        }
    }

    private fun onGroupClicked(groupUid: UUID) {
        val group = findGroupInItems(groupUid) ?: return

        showNoteListScreenEvent.call(group)
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
        val note = findNoteInItems(noteUid) ?: return

        showNoteScreenEvent.call(note)
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
        screenState.value = ScreenState.loading()

        viewModelScope.launch {
            val removeResult = withContext(Dispatchers.Default) {
                interactor.removeGroup(groupUid)
            }

            if (removeResult.isSucceededOrDeferred) {
                toastMessage.value = resourceProvider.getString(R.string.successfully_removed)
            } else {
                val message = errorInteractor.processAndGetMessage(removeResult.error)
                screenState.value = ScreenState.error(message)
            }
        }
    }

    private fun removeNote(groupUid: UUID, noteUid: UUID) {
        screenState.value = ScreenState.loading()

        viewModelScope.launch {
            val removeResult = withContext(Dispatchers.Default) {
                interactor.removeNote(groupUid, noteUid)
            }

            if (removeResult.isSucceededOrDeferred) {
                toastMessage.value = resourceProvider.getString(R.string.successfully_removed)
            } else {
                val message = errorInteractor.processAndGetMessage(removeResult.error)
                screenState.value = ScreenState.error(message)
            }
        }
    }
}