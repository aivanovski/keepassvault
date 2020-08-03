package com.ivanovsky.passnotes.presentation.group

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.globalsnackbar.GlobalSnackbarBus
import com.ivanovsky.passnotes.domain.globalsnackbar.GlobalSnackbarMessageLiveAction
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.ErrorResolution
import com.ivanovsky.passnotes.domain.interactor.group.GroupInteractor
import com.ivanovsky.passnotes.injection.DaggerInjector
import com.ivanovsky.passnotes.presentation.core.ScreenState
import kotlinx.coroutines.*
import java.util.*
import javax.inject.Inject

class GroupPresenter(
    private val view: GroupContract.View,
    private val parentGroupUid: UUID?
) : GroupContract.Presenter {

    @Inject
    lateinit var interactor: GroupInteractor

    @Inject
    lateinit var errorInteractor: ErrorInteractor

    @Inject
    lateinit var globalSnackbarBus: GlobalSnackbarBus

    @Inject
    lateinit var resourceProvider: ResourceProvider

    override val globalSnackbarMessageAction: GlobalSnackbarMessageLiveAction

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private var rootUid: UUID? = null

    init {
        DaggerInjector.getInstance().appComponent.inject(this)

        globalSnackbarMessageAction = globalSnackbarBus.messageAction
    }

    override fun start() {
        if (view.screenState.isNotInitialized) {
            if (parentGroupUid == null) {
                view.screenState = ScreenState.loading()
                loadData()
            } else {
                view.screenState = ScreenState.data()
            }
        }
    }

    override fun destroy() {
        job.cancel()
    }

    override fun loadData() {
        scope.launch {
            val rootUidResult = withContext(Dispatchers.Default) {
                interactor.getRootGroupUid()
            }

            if (rootUidResult.isSucceededOrDeferred) {
                rootUid = rootUidResult.obj
                view.screenState = ScreenState.data()
            } else {
                val message = errorInteractor.processAndGetMessage(rootUidResult.error)
                view.screenState = ScreenState.error(message)
            }
        }
    }

    override fun createNewGroup(title: String) {
        val parentGroupUid = getParentUid() ?: return

        val trimmedTitle = title.trim()

        if (trimmedTitle.isNotEmpty()) {
            view.hideKeyboard()
            view.setDoneButtonVisibility(false)
            view.setTitleEditTextError(null)

            view.screenState = ScreenState.loading()

            scope.launch {
                val result = withContext(Dispatchers.Default) {
                    interactor.createNewGroup(trimmedTitle, parentGroupUid)
                }

                if (result.isSucceeded) {
                    view.finishScreen()
                } else {
                    view.setDoneButtonVisibility(true)

                    val processedError = errorInteractor.process(result.error)
                    if (processedError.resolution == ErrorResolution.RETRY) {
                        //TODO: implement retry state
                        view.screenState = ScreenState.dataWithError("Test")
                    } else {
                        view.screenState = ScreenState.dataWithError(processedError.message)
                    }
                }
            }
        } else {
            view.setTitleEditTextError(resourceProvider.getString(R.string.empty_field))
        }
    }

    private fun getParentUid(): UUID? {
        return when {
            parentGroupUid != null -> parentGroupUid
            rootUid != null -> rootUid
            else -> null
        }
    }
}