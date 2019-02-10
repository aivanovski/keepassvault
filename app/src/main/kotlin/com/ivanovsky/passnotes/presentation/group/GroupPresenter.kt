package com.ivanovsky.passnotes.presentation.group

import androidx.lifecycle.MutableLiveData
import android.content.Context
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.ErrorResolution
import com.ivanovsky.passnotes.domain.interactor.group.GroupInteractor
import com.ivanovsky.passnotes.injection.Injector
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.livedata.SingleLiveAction
import kotlinx.coroutines.*
import javax.inject.Inject

class GroupPresenter(private val context: Context) : GroupContract.Presenter {

	@Inject
	lateinit var interactor: GroupInteractor

	@Inject
	lateinit var errorInteractor: ErrorInteractor

	override val screenState = MutableLiveData<ScreenState>()
	override val doneButtonVisibility = MutableLiveData<Boolean>()
	override val titleEditTextError = MutableLiveData<String>()
	override val hideKeyboardAction = SingleLiveAction<Void>()
	override val finishScreenAction = SingleLiveAction<Void>()

	private val handler = CoroutineExceptionHandler { _, e ->
		e.printStackTrace()
	}
	private val scope = CoroutineScope(Dispatchers.IO + handler)

	init {
		Injector.getInstance().encryptedDatabaseComponent.inject(this)
	}

	override fun start() {
		screenState.value = ScreenState.data()
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

			scope.launch(Dispatchers.IO) {
				val result = interactor.createNewGroup(trimmedTitle)

				withContext(Dispatchers.Main) {
					onCreateGroupResult(result)
				}
			}
		} else {
			titleEditTextError.value = context.getString(R.string.empty_field)
		}
	}

	private fun onCreateGroupResult(result: OperationResult<Group>) {
		if (result.isSuccessful) {
			finishScreenAction.call()
		} else {
			doneButtonVisibility.value = true

			val processedError = errorInteractor.process(result.error)
			if (processedError.resolution == ErrorResolution.RETRY) {
				//TODO: implement retry state
				screenState.value = ScreenState.dataWithError("Test")
			} else {
				screenState.value = ScreenState.dataWithError(processedError.message)
			}
		}
	}
}