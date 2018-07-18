package com.ivanovsky.passnotes.ui.groups

import android.content.Context
import com.ivanovsky.passnotes.App
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.safedb.EncryptedDatabaseProvider
import com.ivanovsky.passnotes.data.safedb.model.Group
import com.ivanovsky.passnotes.ui.core.FragmentState
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class GroupsPresenter(val context: Context, val view: GroupsContract.View) :
		GroupsContract.Presenter,
		ObserverBus.GroupDataSetObserver {

	@Inject
	lateinit var dbProvider: EncryptedDatabaseProvider

	@Inject
	lateinit var observerBus: ObserverBus

	private val disposables: CompositeDisposable

	override fun start() {
		view.setState(FragmentState.LOADING)
		observerBus.register(this)
		loadData()
	}

	init {
		App.getDaggerComponent().inject(this)
		disposables = CompositeDisposable()
	}

	override fun stop() {
		observerBus.unregister(this)
		disposables.clear()
	}

	override fun loadData() {
		if (dbProvider.isOpened) {
			val repository = dbProvider.database.groupRepository

			val disposable = repository.allGroup
					.subscribeOn(Schedulers.newThread())
					.observeOn(AndroidSchedulers.mainThread())
					.subscribe(Consumer { onGroupsLoaded(it) })

			disposables.add(disposable)
		} else {
			view.showUnlockScreenAndFinish()
		}
	}

	private fun onGroupsLoaded(groups: List<Group>) {
		if (groups.isNotEmpty()) {
			view.showGroups(groups)
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