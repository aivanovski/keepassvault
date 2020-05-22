package com.ivanovsky.passnotes.presentation.group

import androidx.lifecycle.MutableLiveData
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.domain.ResourceHelper
import com.ivanovsky.passnotes.domain.globalsnackbar.GlobalSnackbarBus
import com.ivanovsky.passnotes.domain.globalsnackbar.GlobalSnackbarMessageLiveAction
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.ErrorResolution
import com.ivanovsky.passnotes.domain.interactor.group.GroupInteractor
import com.ivanovsky.passnotes.injection.Injector
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.livedata.SingleLiveEvent
import kotlinx.coroutines.*
import javax.inject.Inject

class GroupPresenter(
    private val view: GroupContract.View
) : GroupContract.Presenter {

    @Inject
    lateinit var interactor: GroupInteractor

    @Inject
    lateinit var errorInteractor: ErrorInteractor

    @Inject
    lateinit var globalSnackbarBus: GlobalSnackbarBus

    @Inject
    lateinit var resourceHelper: ResourceHelper

    override val doneButtonVisibility = MutableLiveData<Boolean>()
    override val titleEditTextError = MutableLiveData<String>()
    override val hideKeyboardEvent = SingleLiveEvent<Void>()
    override val finishScreenEvent = SingleLiveEvent<Void>()
    override val globalSnackbarMessageAction: GlobalSnackbarMessageLiveAction

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    init {
        Injector.getInstance().encryptedDatabaseComponent.inject(this)

        globalSnackbarMessageAction = globalSnackbarBus.messageAction
    }

    override fun start() {
        if (view.screenState.isNotInitialized) {
            view.screenState = ScreenState.data()
        }
    }

    override fun destroy() {
        job.cancel()
    }

    override fun createNewGroup(title: String) {
        val trimmedTitle = title.trim()

        if (trimmedTitle.isNotEmpty()) {
            hideKeyboardEvent.call()
            doneButtonVisibility.value = false
            titleEditTextError.value = null
            view.screenState = ScreenState.loading()

            scope.launch {
                val result = withContext(Dispatchers.Default) {
                    interactor.createNewGroup(trimmedTitle)
                }

                if (result.isSucceeded) {
                    finishScreenEvent.call()
                } else {
                    doneButtonVisibility.value = true

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
            titleEditTextError.value = resourceHelper.getString(R.string.empty_field)
        }
    }
}