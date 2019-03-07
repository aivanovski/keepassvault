package com.ivanovsky.passnotes.presentation.group

import androidx.lifecycle.MutableLiveData
import android.content.Context
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.domain.globalsnackbar.GlobalSnackbarBus
import com.ivanovsky.passnotes.domain.globalsnackbar.GlobalSnackbarMessageLiveAction
import com.ivanovsky.passnotes.domain.globalsnackbar.SnackbarReceiverFilter.Companion.allExceptCurrentScreen
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.group.GroupInteractor
import com.ivanovsky.passnotes.injection.Injector
import com.ivanovsky.passnotes.presentation.Screen
import com.ivanovsky.passnotes.presentation.core.FragmentState
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.SnackbarMessage
import com.ivanovsky.passnotes.presentation.core.livedata.SingleLiveAction
import com.ivanovsky.passnotes.util.COROUTINE_HANDLER
import kotlinx.coroutines.*
import javax.inject.Inject

class GroupPresenter(private val context: Context) : GroupContract.Presenter {

	@Inject
	lateinit var interactor: GroupInteractor

	@Inject
	lateinit var errorInteractor: ErrorInteractor

	@Inject
	lateinit var globalSnackbarBus: GlobalSnackbarBus

	override val screenState = MutableLiveData<ScreenState>()
	override val doneButtonVisibility = MutableLiveData<Boolean>()
	override val titleEditTextError = MutableLiveData<String>()
	override val hideKeyboardAction = SingleLiveAction<Void>()
	override val finishScreenAction = SingleLiveAction<Void>()
	override val snackbarMessageAction = SingleLiveAction<String>()
	override val globalSnackbarMessageAction: GlobalSnackbarMessageLiveAction

	private val scope = CoroutineScope(Dispatchers.Main + COROUTINE_HANDLER)

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

			scope.launch {
				val result = withContext(Dispatchers.IO) { interactor.createNewGroup(trimmedTitle) }
				onCreateGroupResult(result)
			}
		} else {
			titleEditTextError.value = context.getString(R.string.empty_field)
		}
	}

	private fun onCreateGroupResult(result: OperationResult<Group>) {
		val message = SnackbarMessage("Hello from hell!", true)
//		val filterableMessage = FilterableSnackbarMessage(message, allExceptCurrentScreen(Screen.GROUP))
//		globalSnackbarMessageAction.call(filterableMessage)

		globalSnackbarBus.send(message, allExceptCurrentScreen(Screen.GROUP))

		if (result.isSucceeded) {
			finishScreenAction.call()
		} else if (result.isSucceeded) {
			snackbarMessageAction.call(context.getString(R.string.offline_modification_message))
			finishScreenAction.call()
		} else {
			val message = errorInteractor.processAndGetMessage(result.error)
			doneButtonVisibility.value = true
			screenState.value = ScreenState.dataWithError(message)
		}
	}
}