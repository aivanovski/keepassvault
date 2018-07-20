package com.ivanovsky.passnotes.presentation.newdb;

import android.content.Context;

import com.ivanovsky.passnotes.R;
import com.ivanovsky.passnotes.data.entity.DatabaseDescriptor;
import com.ivanovsky.passnotes.data.repository.keepass.KeepassDatabaseKey;
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor;
import com.ivanovsky.passnotes.domain.interactor.newdb.NewDatabaseInteractor;
import com.ivanovsky.passnotes.injection.Injector;
import com.ivanovsky.passnotes.presentation.core.FragmentState;
import com.ivanovsky.passnotes.presentation.newdb.NewDatabaseContract.Presenter;
import com.ivanovsky.passnotes.util.FileUtils;

import java.io.File;

import javax.inject.Inject;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class NewDatabasePresenter implements Presenter {

	@Inject
	NewDatabaseInteractor interactor;

	@Inject
	ErrorInteractor errorInteractor;

	private final NewDatabaseContract.View view;
	private final Context context;
	private final CompositeDisposable disposables;

	NewDatabasePresenter(NewDatabaseContract.View view, Context context) {
		Injector.getInstance().getAppComponent().inject(this);
		this.view = view;
		this.context = context;
		this.disposables = new CompositeDisposable();
	}

	@Override
	public void start() {
		view.setState(FragmentState.DISPLAYING_DATA);
	}

	@Override
	public void stop() {
		disposables.clear();
	}

	@Override
	public void createNewDatabaseFile(String filename, String password) {
		view.setDoneButtonVisible(false);

		File dir = FileUtils.getKeepassDir(context);
		if (dir != null) {
			KeepassDatabaseKey key = new KeepassDatabaseKey(password);
			File dbFile = new File(dir, filename + ".kdbx");//TODO: fix db name creation

			Disposable disposable = interactor.createNewDatabaseAndOpen(key, dbFile)
					.subscribe(created -> onDatabaseCreated(created, dbFile, password),
							this::onFailedToCreateDatabase);
			disposables.add(disposable);

			view.setState(FragmentState.LOADING);
		} else {
			view.showError(context.getString(R.string.error_was_occurred));
		}
	}

	private void onDatabaseCreated(Boolean result, File dbFile, String password) {
		if (result) {
			view.showGroupsScreen(new DatabaseDescriptor(password, dbFile));
		} else {
			view.showError(context.getString(R.string.error_was_occurred));
			view.setDoneButtonVisible(true);
		}
	}

	private void onFailedToCreateDatabase(Throwable throwable) {
		view.showError(errorInteractor.getErrorMessage(throwable));
	}
}
