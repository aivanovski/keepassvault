package com.ivanovsky.passnotes.presentation.newdb

import androidx.lifecycle.MutableLiveData
import android.content.Context
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.file.FSType
import com.ivanovsky.passnotes.data.repository.keepass.KeepassDatabaseKey
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.newdb.NewDatabaseInteractor
import com.ivanovsky.passnotes.injection.Injector
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.livedata.SingleLiveAction
import com.ivanovsky.passnotes.util.FileUtils
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

	override val screenState = MutableLiveData<ScreenState>()
	override val storageTypeAndPath = MutableLiveData<Pair<String, String>>()
	override val doneButtonVisibility = MutableLiveData<Boolean>()
	override val showGroupsScreenAction = SingleLiveAction<Void>()
	override val showStorageScreenAction = SingleLiveAction<Void>()
	override val hideKeyboardAction = SingleLiveAction<Void>()
	var selectedStorageDir: FileDescriptor? = null
	private val scope = CoroutineScope(Dispatchers.IO)

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

			scope.launch(Dispatchers.IO) {
				val result = interactor.createNewDatabaseAndOpen(dbKey, dbFile)

				withContext(Dispatchers.Main) {
					onCreateDatabaseResult(result)
				}
			}

		} else {
			screenState.value = ScreenState.dataWithError(context.getString(R.string.storage_is_not_selected))
		}
	}

	fun onCreateDatabaseResult(result: OperationResult<Boolean>) {
		if (result.isSuccessful) {
			val created = result.result

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

	override fun selectStorage() {
		showStorageScreenAction.call()
	}

	override fun onStorageSelected(selectedFile: FileDescriptor) {
		selectedStorageDir = selectedFile

		if (selectedFile.fsType == FSType.REGULAR_FS) {
			val file = File(selectedFile.path)

			if (FileUtils.isLocatedInPrivateStorage(file, context)) {
				storageTypeAndPath.value = Pair(context.getString(R.string.private_storage), selectedFile.path)
			} else {
				storageTypeAndPath.value = Pair(context.getString(R.string.public_storage), selectedFile.path)
			}
		} else if (selectedFile.fsType == FSType.DROPBOX) {
			storageTypeAndPath.value = Pair(context.getString(R.string.dropbox), selectedFile.path)
		}
	}
}