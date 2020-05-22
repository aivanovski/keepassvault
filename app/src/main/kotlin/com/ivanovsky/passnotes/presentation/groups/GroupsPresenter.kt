package com.ivanovsky.passnotes.presentation.groups

import android.content.Context
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.domain.globalsnackbar.GlobalSnackbarBus
import com.ivanovsky.passnotes.domain.globalsnackbar.GlobalSnackbarMessageLiveAction
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.groups.GroupsInteractor
import com.ivanovsky.passnotes.injection.Injector
import com.ivanovsky.passnotes.presentation.core.FragmentState
import kotlinx.coroutines.*
import javax.inject.Inject

class GroupsPresenter(val context: Context, val view: GroupsContract.View) :
        GroupsContract.Presenter,
        ObserverBus.GroupDataSetObserver {

    @Inject
    lateinit var interactor: GroupsInteractor

    @Inject
    lateinit var errorInteractor: ErrorInteractor

    @Inject
    lateinit var observerBus: ObserverBus

    @Inject
    lateinit var globalSnackbarBus: GlobalSnackbarBus

    override val globalSnackbarMessageAction: GlobalSnackbarMessageLiveAction

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    init {
        Injector.getInstance().encryptedDatabaseComponent.inject(this)
        globalSnackbarMessageAction = globalSnackbarBus.messageAction
    }

    override fun start() {
        view.setState(FragmentState.LOADING)
        observerBus.register(this)
        loadData()
    }

    override fun stop() {
        observerBus.unregister(this)
    }

    override fun destroy() {
        job.cancel()
    }

    override fun loadData() {
        scope.launch {
            val result = withContext(Dispatchers.Default) {
                interactor.getAllGroupsWithNoteCount()
            }

            if (result.isSucceededOrDeferred) {
                val groupsAndCounts = result.obj

                if (groupsAndCounts.isNotEmpty()) {
                    view.showGroups(groupsAndCounts)
                } else {
                    view.showNoItems()
                }
            } else {
                view.showError(errorInteractor.processAndGetMessage(result.error))
            }
        }
    }

    override fun onGroupDataSetChanged() {
        loadData()
    }

    override fun onGroupClicked(group: Group) {
        view.showNotesScreen(group)
    }
}