package com.ivanovsky.passnotes.presentation.unlock

import android.arch.lifecycle.MutableLiveData
import android.content.Context

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.entity.UsedFile
import com.ivanovsky.passnotes.data.repository.keepass.KeepassDatabaseKey
import com.ivanovsky.passnotes.domain.interactor.unlock.UnlockInteractor
import com.ivanovsky.passnotes.injection.Injector
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.presentation.core.FragmentState
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.livedata.SingleLiveAction

import java.io.File

import javax.inject.Inject

import io.reactivex.disposables.CompositeDisposable

class UnlockPresenter (private val context: Context,
                       private val view: UnlockContract.View) :
		UnlockContract.Presenter,
		ObserverBus.UsedFileDataSetObserver {

	@Inject
	lateinit var interactor: UnlockInteractor

	@Inject
	lateinit var errorInteractor: ErrorInteractor

	@Inject
	lateinit var observerBus: ObserverBus

	override val recentlyUsedFiles = MutableLiveData<List<UsedFile>>()
	override val screenState = MutableLiveData<ScreenState>()
	override val showGroupsScreenAction = SingleLiveAction<Void>()
	override val showNewDatabaseScreenAction = SingleLiveAction<Void>()
	override val hideKeyboardAction = SingleLiveAction<Void>()
	private val disposables = CompositeDisposable()

	init {
		Injector.getInstance().appComponent.inject(this)
	}

	override fun start() {
		view.setState(FragmentState.LOADING)
		observerBus.register(this)
		loadData()
	}

	override fun stop() {
		observerBus.unregister(this)
		disposables.clear()
	}

	override fun loadData() {
		val disposable = interactor.recentlyOpenedFiles
				.subscribe { result -> onGetRecentlyOpenedFilesResult(result) }

		disposables.add(disposable)
	}

	private fun onGetRecentlyOpenedFilesResult(result: OperationResult<List<UsedFile>>) {
		if (result.isSuccessful) {
			val files = result.result
			if (files.isNotEmpty()) {
				recentlyUsedFiles.value = files
				screenState.value = ScreenState.data()
			} else {
				screenState.value = ScreenState.empty(context.getString(R.string.no_files_to_open))
			}
		} else {
			val message = errorInteractor.processAndGetMessage(result.error)
			screenState.setValue(ScreenState.error(message))
		}
	}

	override fun onUnlockButtonClicked(password: String, dbFile: File) {
		hideKeyboardAction.call()
		screenState.value = ScreenState.loading()

		val key = KeepassDatabaseKey(password)

		val disposable = interactor.openDatabase(key, dbFile)
				.subscribe { result -> onOpenDatabaseResult(result) }

		disposables.add(disposable)
	}

	private fun onOpenDatabaseResult(result: OperationResult<Boolean>) {
		if (result.result != null) {
			showGroupsScreenAction.call()
		} else {
			val message = errorInteractor.processAndGetMessage(result.error)
			screenState.value = ScreenState.error(message)
		}
	}

	override fun onUsedFileDataSetChanged() {
		loadData()
	}
}