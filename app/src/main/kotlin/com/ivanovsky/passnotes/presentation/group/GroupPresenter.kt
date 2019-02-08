package com.ivanovsky.passnotes.presentation.group

import android.arch.lifecycle.MutableLiveData
import android.content.Context
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.domain.interactor.group.GroupInteractor
import com.ivanovsky.passnotes.injection.Injector
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.livedata.SingleLiveAction
import kotlinx.coroutines.*
import javax.inject.Inject

class GroupPresenter(private val context: Context) : GroupContract.Presenter {

	@Inject
	lateinit var interactor: GroupInteractor

	override val screenState = MutableLiveData<ScreenState>()
	override val doneButtonVisibility = MutableLiveData<Boolean>()
	override val titleEditTextError = MutableLiveData<String>()
	override val hideKeyboardAction = SingleLiveAction<Void>()
	override val finishScreenAction = SingleLiveAction<Void>()

	private val handler = CoroutineExceptionHandler { ctx, e ->
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
			screenState.value = ScreenState.dataWithError(result.error.message)
		}
	}
}