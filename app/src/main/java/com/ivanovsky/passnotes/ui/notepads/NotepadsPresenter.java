package com.ivanovsky.passnotes.ui.notepads;

import android.content.Context;

import com.ivanovsky.passnotes.App;
import com.ivanovsky.passnotes.data.repository.NotepadRepository;
import com.ivanovsky.passnotes.data.safedb.SafeDatabaseProvider;
import com.ivanovsky.passnotes.data.safedb.model.Notepad;
import com.ivanovsky.passnotes.ui.core.FragmentState;

import java.util.List;

import javax.inject.Inject;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class NotepadsPresenter implements NotepadsContract.Presenter {

	@Inject
	SafeDatabaseProvider safeDbProvider;

	private final String dbName;
	private final Context context;
	private final NotepadsContract.View view;
	private Subscription openDbSubscription;
	private Subscription loadDataSubscription;

	NotepadsPresenter(Context context, NotepadsContract.View view, String dbName) {
		App.getDaggerComponent().inject(this);
		this.context = context;
		this.view = view;
		this.dbName = dbName;
	}

	@Override
	public void start() {
		view.setState(FragmentState.LOADING);

		loadData();
	}

	@Override
	public void stop() {
		if (openDbSubscription != null) {
			openDbSubscription.unsubscribe();
			openDbSubscription = null;
		}

		if (loadDataSubscription != null) {
			loadDataSubscription.unsubscribe();
			loadDataSubscription = null;
		}
	}

	@Override
	public void loadData() {
		if (safeDbProvider.isDatabaseOpened()) {
			NotepadRepository repository = safeDbProvider.getNotepadRepository();

			loadDataSubscription = repository.getAllNotepads()
					.subscribeOn(Schedulers.newThread())
					.observeOn(AndroidSchedulers.mainThread())
					.subscribe(this::onNotepadsLoaded);
		} else {
			openDbSubscription = safeDbProvider.observeDatabase(dbName + ".db")
					.subscribeOn(Schedulers.newThread())
					.observeOn(AndroidSchedulers.mainThread())
					.subscribe(db -> onDbOpened());
		}
	}

	private void onDbOpened() {
		NotepadRepository repository = safeDbProvider.getNotepadRepository();

		loadDataSubscription = repository.getAllNotepads()
				.subscribeOn(Schedulers.newThread())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(this::onNotepadsLoaded);
	}

	private void onNotepadsLoaded(List<Notepad> notepads) {
		if (notepads.size() != 0) {
			view.showNotepads(notepads);
		} else {
			view.showNoItems();
		}
	}
}
