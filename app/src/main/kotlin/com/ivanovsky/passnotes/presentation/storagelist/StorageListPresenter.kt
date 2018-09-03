package com.ivanovsky.passnotes.presentation.storagelist

import android.arch.lifecycle.MutableLiveData
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.domain.entity.StorageOption
import com.ivanovsky.passnotes.domain.entity.StorageOptionType
import com.ivanovsky.passnotes.domain.interactor.storagelist.StorageListInteractor
import com.ivanovsky.passnotes.injection.Injector
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.livedata.SingleLiveAction
import javax.inject.Inject

class StorageListPresenter(private val mode: Mode) :
		StorageListContract.Presenter {

	@Inject
	lateinit var interactor: StorageListInteractor

	override val storageOptions = MutableLiveData<List<StorageOption>>()
	override val screenState = MutableLiveData<ScreenState>()
	override val showFilePickerScreenAction = SingleLiveAction<Pair<FileDescriptor, Mode>>()
	override val fileSelectedAction = SingleLiveAction<FileDescriptor>()

	init {
		Injector.getInstance().appComponent.inject(this)
	}

	override fun start() {
		storageOptions.value = interactor.getAvailableStorageOptions()
		screenState.value = ScreenState.data()
	}

	override fun stop() {
	}

	override fun onStorageOptionClicked(option: StorageOption) {
		when (option.type) {
			StorageOptionType.PRIVATE_STORAGE -> onPrivateStorageSelected(option.root!!)
			StorageOptionType.EXTERNAL_STORAGE -> onExternalStorageSelected(option.root!!)
			StorageOptionType.DROPBOX -> onDropboxStorageSelected()
		}
	}

	private fun onPrivateStorageSelected(root: FileDescriptor) {
		fileSelectedAction.call(root)
	}

	private fun onExternalStorageSelected(root: FileDescriptor) {
		showFilePickerScreenAction.call(Pair(root, mode))
	}

	private fun onDropboxStorageSelected() {
		//TODO: implement
		throw RuntimeException("Not implemented")
	}

	override fun onFilePicked(file: FileDescriptor) {
		fileSelectedAction.call(file)
	}
}