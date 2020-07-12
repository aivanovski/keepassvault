package com.ivanovsky.passnotes.presentation.groups

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.domain.ResourceHelper
import com.ivanovsky.passnotes.domain.globalsnackbar.GlobalSnackbarBus
import com.ivanovsky.passnotes.domain.globalsnackbar.GlobalSnackbarMessageLiveAction
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.groups.GroupsInteractor
import com.ivanovsky.passnotes.injection.Injector
import com.ivanovsky.passnotes.presentation.core.ScreenState
import kotlinx.coroutines.*
import java.util.*
import javax.inject.Inject

class GroupsPresenter(
    private val view: GroupsContract.View,
    private val groupUid: UUID?
) : GroupsContract.Presenter,
    ObserverBus.GroupDataSetObserver,
    ObserverBus.NoteDataSetChanged,
    ObserverBus.NoteContentChangedObserver {

    @Inject
    lateinit var interactor: GroupsInteractor

    @Inject
    lateinit var errorInteractor: ErrorInteractor

    @Inject
    lateinit var observerBus: ObserverBus

    @Inject
    lateinit var globalSnackbarBus: GlobalSnackbarBus

    @Inject
    lateinit var resourceHelper: ResourceHelper

    override val globalSnackbarMessageAction: GlobalSnackbarMessageLiveAction

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private var currentDataItems: List<GroupsInteractor.Item>? = null
    private var rootGroupUid: UUID? = null

    init {
        Injector.getInstance().appComponent.inject(this)
        globalSnackbarMessageAction = globalSnackbarBus.messageAction
    }

    override fun start() {
        if (view.screenState.isNotInitialized) {
            view.screenState = ScreenState.loading()

            loadData()
            observerBus.register(this)
        }
    }

    override fun destroy() {
        observerBus.unregister(this)
        job.cancel()
    }

    override fun loadData() {
        scope.launch {
            val data = withContext(Dispatchers.Default) {
                if (groupUid == null) {
                    interactor.getRootGroupData()
                } else {
                    interactor.getGroupData(groupUid)
                }
            }

            if (groupUid == null && rootGroupUid == null) {
                rootGroupUid = withContext(Dispatchers.Default) {
                    interactor.getRootUid()
                }
            }

            if (data.isSucceededOrDeferred) {
                val dataItems = data.obj
                currentDataItems = dataItems


                if (dataItems.isNotEmpty()) {
                    view.setItems(createAdapterItems(dataItems))
                    view.screenState = ScreenState.data()
                } else {
                    val emptyText = resourceHelper.getString(R.string.no_items)
                    view.screenState = ScreenState.empty(emptyText)
                }
            } else {
                val message = errorInteractor.processAndGetMessage(data.error)
                view.screenState = ScreenState.error(message)
            }
        }
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

    override fun onListItemClicked(position: Int) {
        val dataItems = currentDataItems ?: return

        if (position == dataItems.size) {
            val currentGroupUid = getCurrentGroupUid() ?: return

            view.showNewGroupScreen(currentGroupUid)
        } else {
            val dataItem = dataItems[position]

            if (dataItem is GroupsInteractor.GroupItem) {
                view.showNoteListScreen(dataItem.group)
            } else if (dataItem is GroupsInteractor.NoteItem) {
                view.showNoteScreen(dataItem.note)
            }
        }
    }

    private fun createAdapterItems(dataItems: List<GroupsInteractor.Item>): List<GroupsAdapter.ListItem> {
        val result = mutableListOf<GroupsAdapter.ListItem>()

        for (dataItem in dataItems) {
            if (dataItem is GroupsInteractor.GroupItem) {
                result.add(
                    GroupsAdapter.GroupListItem(
                        dataItem.group.title, dataItem.noteCount, dataItem.childGroupCount
                    )
                )
            } else if (dataItem is GroupsInteractor.NoteItem) {
                result.add(GroupsAdapter.NoteListItem(dataItem.note.title))
            }
        }

        return result
    }

    override fun onAddButtonClicked() {
        view.showNewEntryDialog()
    }

    private fun getCurrentGroupUid(): UUID? {
        return when {
            groupUid != null -> groupUid
            rootGroupUid != null -> rootGroupUid
            else -> null
        }
    }

    override fun onCreateNewGroupClicked() {
        val currentGroupUid = getCurrentGroupUid() ?: return

        view.showNewGroupScreen(currentGroupUid)

    }

    override fun onCreateNewNoteClicked() {
        val currentGroupUid = getCurrentGroupUid() ?: return

        view.showNewNoteScreen(currentGroupUid)
    }
}