package com.ivanovsky.passnotes.presentation.groups

import android.content.Context
import android.provider.Settings
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.groups.GroupsInteractor
import com.ivanovsky.passnotes.injection.Injector
import com.ivanovsky.passnotes.presentation.core.FragmentState
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import javax.inject.Inject

class GroupsPresenter(val context: Context, val view: GroupsContract.View) :
		GroupsContract.Presenter,
		ObserverBus.GroupDataSetObserver {

	@Inject
	lateinit var interactor: GroupsInteractor

	@Inject
	lateinit var errorInteractor: ErrorInteractor

	@Inject
	lateinit var observerBus: ObserverBus

	override fun start() {
		view.setState(FragmentState.LOADING)
		observerBus.register(this)
		loadData()
	}

	init {
		Injector.getInstance().encryptedDatabaseComponent.inject(this)
	}

	override fun stop() {
		observerBus.unregister(this)
	}

	override fun loadData() {
		GlobalScope.launch(Dispatchers.IO) {
			val groupsWithNoteCount = interactor.getAllGroupsWithNoteCount()

			withContext(Dispatchers.Main) {
				onGroupsLoaded(groupsWithNoteCount)
			}
		}
	}

	private fun onGroupsLoaded(result: OperationResult<List<Pair<Group, Int>>>) {
		if (result.isSuccessful) {
			val groupsAndCounts = result.result

			if (groupsAndCounts.isNotEmpty()) {
				view.showGroups(groupsAndCounts)
			} else {
				view.showNoItems()
			}
		} else {
			view.showError(errorInteractor.processAndGetMessage(result.error))
		}
	}

	override fun onGroupDataSetChanged() {
		loadData()
	}

	override fun onGroupClicked(group: Group) {
		view.showNotesScreen(group)
	}
}