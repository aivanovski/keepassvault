package com.ivanovsky.passnotes.presentation.newdb;

import android.content.Context;

import com.ivanovsky.passnotes.R;
import com.ivanovsky.passnotes.data.entity.DatabaseDescriptor;
import com.ivanovsky.passnotes.data.entity.OperationResult;
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
		File dir = FileUtils.getKeepassDir(context);
		if (dir != null) {
			view.hideKeyboard();
			view.setDoneButtonVisible(false);
			view.setState(FragmentState.LOADING);

			KeepassDatabaseKey key = new KeepassDatabaseKey(password);
			File dbFile = new File(dir, filename + ".kdbx");//TODO: fix db name creation

			Disposable disposable = interactor.createNewDatabaseAndOpen(key, dbFile)
					.subscribe(result -> onCreateDatabaseResult(result, dbFile, password));

			disposables.add(disposable);
		} else {
			view.showError(context.getString(R.string.error_was_occurred));
		}
	}

	private void onCreateDatabaseResult(OperationResult<Boolean> result, File dbFile, String password) {
		if (result.getResult() != null) {
			boolean created = result.getResult();

			if (created) {
				view.showGroupsScreen(new DatabaseDescriptor(password, dbFile));
			} else {
				view.showError(context.getString(R.string.error_was_occurred));
				view.setDoneButtonVisible(true);
			}
		} else {
			view.showError(errorInteractor.processAndGetMessage(result.getError()));
		}
	}

	@Override
	public void selectStorage() {
		view.showStorageScreen();
	}
}
