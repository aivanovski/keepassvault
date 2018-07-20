package com.ivanovsky.passnotes.ui.groups

import android.content.Context
import com.ivanovsky.passnotes.App
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.repository.GroupRepository
import com.ivanovsky.passnotes.injection.Injector
import com.ivanovsky.passnotes.presentation.core.FragmentState
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class GroupsPresenter(val context: Context, val view: GroupsContract.View) :
		GroupsContract.Presenter,
		ObserverBus.GroupDataSetObserver {

	@Inject
	lateinit var groupRepository: GroupRepository

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
		val disposable = groupRepository.allGroup
				.subscribeOn(Schedulers.newThread())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(Consumer { onGroupsLoaded(it) })

		disposables.add(disposable)
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