package com.ivanovsky.passnotes.ui.groups;

import android.content.Context;

import com.ivanovsky.passnotes.App;
import com.ivanovsky.passnotes.data.ObserverBus;
import com.ivanovsky.passnotes.data.safedb.EncryptedDatabaseProvider;
import com.ivanovsky.passnotes.data.safedb.GroupRepository;
import com.ivanovsky.passnotes.data.safedb.model.Group;
import com.ivanovsky.passnotes.ui.core.FragmentState;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class GroupsPresenter implements
		GroupsContract.Presenter,
		ObserverBus.GroupDataSetObserver {

	@Inject
	EncryptedDatabaseProvider dbProvider;

	@Inject
	ObserverBus observerBus;

	private final Context context;
	private final GroupsContract.View view;
	private final CompositeDisposable disposables;

	GroupsPresenter(Context context, GroupsContract.View view) {
		App.getDaggerComponent().inject(this);
		this.context = context;
		this.view = view;
		this.disposables = new CompositeDisposable();
	}

	@Override
	public void start() {
		view.setState(FragmentState.LOADING);
		observerBus.register(this);
		loadData();
	}

	@Override
	public void stop() {
		observerBus.unregister(this);
		disposables.clear();
	}

	@Override
	public void loadData() {
		if (dbProvider.isOpened()) {
			GroupRepository repository = dbProvider.getDatabase().getGroupRepository();

			Disposable disposable = repository.getAllGroup()
					.subscribeOn(Schedulers.newThread())
					.observeOn(AndroidSchedulers.mainThread())
					.subscribe(this::onGroupsLoaded);
			disposables.add(disposable);
		} else {
			view.showUnlockScreenAndFinish();
		}
	}

	private void onGroupsLoaded(List<Group> groups) {
		if (groups.size() != 0) {
			view.showGroups(groups);
		} else {
			view.showNoItems();
		}
	}

	@Override
	public void onGroupDataSetChanged() {
		loadData();
	}
}
