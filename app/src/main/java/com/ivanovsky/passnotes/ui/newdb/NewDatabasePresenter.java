package com.ivanovsky.passnotes.ui.newdb;

import android.content.Context;

import com.ivanovsky.passnotes.App;
import com.ivanovsky.passnotes.R;
import com.ivanovsky.passnotes.data.db.model.UsedFile;
import com.ivanovsky.passnotes.data.safedb.SafeDatabaseProvider;
import com.ivanovsky.passnotes.data.repository.UsedFileRepository;
import com.ivanovsky.passnotes.ui.core.FragmentState;
import com.ivanovsky.passnotes.ui.newdb.NewDatabaseContract.Presenter;

import java.io.File;

import javax.inject.Inject;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class NewDatabasePresenter implements Presenter {

	@Inject
	SafeDatabaseProvider encryptedDbProvider;

	@Inject
	UsedFileRepository usedFileRepository;

	private final NewDatabaseContract.View view;
	private final Context context;

	NewDatabasePresenter(NewDatabaseContract.View view, Context context) {
		App.getDaggerComponent().inject(this);
		this.view = view;
		this.context = context;
	}

	@Override
	public void start() {
		view.setState(FragmentState.DISPLAYING_DATA);
	}

	@Override
	public void stop() {
		//TODO: implement methods
	}

	@Override
	public void createNewDatabaseFile(String filename, String password) {
		view.setDoneButtonVisible(false);

		String dbName = filename + ".db";

		Observable.fromCallable(() -> createNewDatabase(dbName))
				.subscribeOn(Schedulers.newThread())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(created -> onNewDatabaseCreated(created, dbName));

		view.setState(FragmentState.LOADING);
	}

	private Boolean createNewDatabase(String name) {
		boolean result = false;

		if (!encryptedDbProvider.isDatabaseExist(name)
				&& encryptedDbProvider.openDatabase(name) != null) {

			File dbFile = encryptedDbProvider.getDatabaseFile(name);

			UsedFile usedFile = new UsedFile();
			usedFile.setFilePath(dbFile.getPath());
			usedFile.setLastAccessTime(System.currentTimeMillis());
			usedFileRepository.insert(usedFile);

			result = true;
		}

		return result;
	}

	private void onNewDatabaseCreated(Boolean created, String dbName) {
		if (created) {
			view.showNotepadsScreen(dbName);
		} else {
			view.showError(context.getString(R.string.error_was_occurred));
			view.setDoneButtonVisible(true);
		}
	}
}
