package com.ivanovsky.passnotes.ui.groups

import android.content.Context
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.domain.interactor.groups.GroupsInteractor
import com.ivanovsky.passnotes.injection.Injector
import com.ivanovsky.passnotes.presentation.core.FragmentState
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import javax.inject.Inject

class GroupsPresenter(val context: Context, val view: GroupsContract.View) :
		GroupsContract.Presenter,
		ObserverBus.GroupDataSetObserver {

	@Inject
	lateinit var interactor: GroupsInteractor

	@Inject
	lateinit var observerBus: ObserverBus

	private val disposables: CompositeDisposable

	override fun start() {
		view.setState(FragmentState.LOADING)
		observerBus.register(this)
		loadData()
	}

	init {
		Injector.getInstance().encryptedDatabaseComponent.inject(this)
		disposables = CompositeDisposable()
	}

	override fun stop() {
		observerBus.unregister(this)
		disposables.clear()
	}

	override fun loadData() {
		val disposable = interactor.getAllGroupsWithNoteCount()
				.subscribe(Consumer { onGroupsLoaded(it) })

		disposables.add(disposable)
	}

	private fun onGroupsLoaded(groupsAndCounts: List<Pair<Group, Int>>) {
		if (groupsAndCounts.isNotEmpty()) {
			view.showGroups(groupsAndCounts)
		} else {
			view.showNoItems()
		}
	}

	override fun onGroupDataSetChanged() {
		loadData()
	}

	override fun onGroupClicked(group: Group) {
		view.showNotesScreen(group)
	}
}