package com.ivanovsky.passnotes.ui.newdb;

import android.content.Context;

import com.ivanovsky.passnotes.ui.core.FragmentState;
import com.ivanovsky.passnotes.ui.newdb.NewDatabaseContract.Presenter;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class NewDatabasePresenter implements Presenter {

	private final NewDatabaseContract.View view;
	private final Context context;

	public NewDatabasePresenter(NewDatabaseContract.View view, Context context) {
		this.view = view;
		this.context = context;
	}

	@Override
	public void start() {
		view.setState(FragmentState.DISPLAYING_DATA);
	}

	private Boolean createNewDatabaseFile() {
		return null;
	}

	private void onNewDatabaseCreated(Boolean created) {
	}

	@Override
	public void onPermissionResult(boolean granted) {
	}

	@Override
	public void createNewDatabaseFile(String filename, String password) {
		Observable.fromCallable(this::createNewDatabaseFile)
				.subscribeOn(Schedulers.newThread())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(this::onNewDatabaseCreated);

		view.showHomeActivity();
	}
}
