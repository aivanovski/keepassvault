package com.ivanovsky.passnotes.presentation.storagelist

import android.arch.lifecycle.MutableLiveData
import com.ivanovsky.passnotes.domain.entity.StorageOption
import com.ivanovsky.passnotes.domain.interactor.storage.StorageInteractor
import com.ivanovsky.passnotes.injection.Injector
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.livedata.SingleLiveAction
import javax.inject.Inject

class StorageListPresenter(private val view: StorageListContract.View): StorageListContract.Presenter {

	@Inject
	lateinit var interactor: StorageInteractor

	override val storageOptions = MutableLiveData<List<StorageOption>>()
	override val screenState = MutableLiveData<ScreenState>()
	override val showFilePickerScreenAction = SingleLiveAction<StorageOption>()

	init {
		Injector.getInstance().appComponent.inject(this)
	}

	override fun start() {
		storageOptions.postValue(interactor.getAvailableStorageOptions())
		screenState.postValue(ScreenState.data())
	}

	override fun stop() {
	}

	override fun selectStorage(option: StorageOption) {
		showFilePickerScreenAction.call(option)
	}
}