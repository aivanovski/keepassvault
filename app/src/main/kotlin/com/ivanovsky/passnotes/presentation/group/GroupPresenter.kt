package com.ivanovsky.passnotes.presentation.group

import androidx.lifecycle.MutableLiveData
import android.content.Context
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.domain.globalsnackbar.GlobalSnackbarBus
import com.ivanovsky.passnotes.domain.globalsnackbar.GlobalSnackbarMessageLiveAction
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.ErrorResolution
import com.ivanovsky.passnotes.domain.interactor.group.GroupInteractor
import com.ivanovsky.passnotes.injection.Injector
import com.ivanovsky.passnotes.presentation.core.FragmentState
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.livedata.SingleLiveAction
import java9.util.concurrent.CompletableFuture
import java9.util.function.Supplier
import java.util.concurrent.Executor
import javax.inject.Inject

class GroupPresenter(private val context: Context) : GroupContract.Presenter {

	@Inject
	lateinit var interactor: GroupInteractor

	@Inject
	lateinit var errorInteractor: ErrorInteractor

	@Inject
	lateinit var globalSnackbarBus: GlobalSnackbarBus

	@Inject
	lateinit var executor: Executor

	override val screenState = MutableLiveData<ScreenState>()
	override val doneButtonVisibility = MutableLiveData<Boolean>()
	override val titleEditTextError = MutableLiveData<String>()
	override val hideKeyboardAction = SingleLiveAction<Void>()
	override val finishScreenAction = SingleLiveAction<Void>()
	override val snackbarMessageAction = SingleLiveAction<String>()
	override val globalSnackbarMessageAction: GlobalSnackbarMessageLiveAction

	init {
		Injector.getInstance().encryptedDatabaseComponent.inject(this)

		globalSnackbarMessageAction = globalSnackbarBus.messageAction
	}

	override fun start() {
		val currentState = screenState.value

		if (currentState == null || currentState.state != FragmentState.LOADING) {
			screenState.value = ScreenState.data()
		}
	}

	override fun stop() {
	}

	override fun createNewGroup(title: String) {
		val trimmedTitle = title.trim()

		if (trimmedTitle.isNotEmpty()) {
			hideKeyboardAction.call()
			doneButtonVisibility.value = false
			titleEditTextError.value = null
			screenState.value = ScreenState.loading()

			CompletableFuture.supplyAsync(Supplier {
                interactor.createNewGroup(trimmedTitle)
			}, executor)
					.thenAccept { result -> onCreateNewGroupResult(result) }

		} else {
			titleEditTextError.value = context.getString(R.string.empty_field)
		}
	}

	private fun onCreateNewGroupResult(result: OperationResult<Group>) {
		if (result.isSucceeded) {
			finishScreenAction.postCall()
		} else {
			doneButtonVisibility.postValue(true)

			val processedError = errorInteractor.process(result.error)
			if (processedError.resolution == ErrorResolution.RETRY) {
				//TODO: implement retry state
				screenState.postValue(ScreenState.dataWithError("Test"))
			} else {
                screenState.postValue(ScreenState.dataWithError(processedError.message))
			}
		}
	}
}