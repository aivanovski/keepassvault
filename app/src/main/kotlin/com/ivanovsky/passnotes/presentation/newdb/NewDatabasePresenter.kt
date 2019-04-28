package com.ivanovsky.passnotes.presentation.newdb

import androidx.lifecycle.MutableLiveData
import android.content.Context
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.repository.file.FSType
import com.ivanovsky.passnotes.data.repository.keepass.KeepassDatabaseKey
import com.ivanovsky.passnotes.domain.FileHelper
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.newdb.NewDatabaseInteractor
import com.ivanovsky.passnotes.injection.Injector
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.livedata.SingleLiveAction
import com.ivanovsky.passnotes.util.COROUTINE_EXCEPTION_HANDLER
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class NewDatabasePresenter(private val context: Context) : NewDatabaseContract.Presenter {

	@Inject
	lateinit var interactor: NewDatabaseInteractor

	@Inject
	lateinit var errorInteractor: ErrorInteractor

	@Inject
	lateinit var fileHelper: FileHelper

	override val screenState = MutableLiveData<ScreenState>()
	override val storageTypeAndPath = MutableLiveData<Pair<String, String>>()
	override val doneButtonVisibility = MutableLiveData<Boolean>()
	override val showGroupsScreenAction = SingleLiveAction<Void>()
	override val showStorageScreenAction = SingleLiveAction<Void>()
	override val hideKeyboardAction = SingleLiveAction<Void>()
	private var selectedStorageDir: FileDescriptor? = null
	private val scope = CoroutineScope(Dispatchers.Main + COROUTINE_EXCEPTION_HANDLER)

	init {
		Injector.getInstance().appComponent.inject(this)
	}

	override fun start() {
		screenState.value = ScreenState.data()
	}

	override fun stop() {
	}

	override fun createNewDatabaseFile(filename: String, password: String) {
		if (selectedStorageDir != null) {
			hideKeyboardAction.call()
			doneButtonVisibility.value = false
			screenState.value = ScreenState.loading()

			val dbKey = KeepassDatabaseKey(password)
			val dbFile = FileDescriptor.fromParent(selectedStorageDir, "$filename.kdbx")

			scope.launch {
				val result = withContext(Dispatchers.Default) {
					interactor.createNewDatabaseAndOpen(dbKey, dbFile)
				}

				if (result.isSucceededOrDeferred) {
					val created = result.obj

					if (created) {
						showGroupsScreenAction.call()
					} else {
						screenState.value = ScreenState.dataWithError(context.getString(R.string.error_was_occurred))
						doneButtonVisibility.value = true
					}
				} else {
					val message = errorInteractor.processAndGetMessage(result.error)
					screenState.value = ScreenState.dataWithError(message)
					doneButtonVisibility.value = true
				}
			}

		} else {
			screenState.value = ScreenState.dataWithError(context.getString(R.string.storage_is_not_selected))
		}
	}

	override fun selectStorage() {
		showStorageScreenAction.call()
	}

	override fun onStorageSelected(selectedFile: FileDescriptor) {
		selectedStorageDir = selectedFile

		if (selectedFile.fsType == FSType.REGULAR_FS) {
			val file = File(selectedFile.path)

			if (fileHelper.isLocatedInPrivateStorage(file)) {
				storageTypeAndPath.value = Pair(context.getString(R.string.private_storage), selectedFile.path)
			} else {
				storageTypeAndPath.value = Pair(context.getString(R.string.public_storage), selectedFile.path)
			}
		} else if (selectedFile.fsType == FSType.DROPBOX) {
			storageTypeAndPath.value = Pair(context.getString(R.string.dropbox), selectedFile.path)
		}
	}
}