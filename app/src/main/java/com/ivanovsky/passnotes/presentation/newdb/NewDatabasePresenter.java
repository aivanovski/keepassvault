package com.ivanovsky.passnotes.presentation.newdb;

import android.content.Context;

import com.ivanovsky.passnotes.R;
import com.ivanovsky.passnotes.data.entity.FileDescriptor;
import com.ivanovsky.passnotes.data.entity.OperationResult;
import com.ivanovsky.passnotes.data.repository.file.FSType;
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
	private FileDescriptor selectedStorageDir;

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
		if (selectedStorageDir != null) {
			view.hideKeyboard();
			view.setDoneButtonVisible(false);
			view.setState(FragmentState.LOADING);

			KeepassDatabaseKey key = new KeepassDatabaseKey(password);
			FileDescriptor dbFile = FileDescriptor.fromParent(selectedStorageDir, filename + ".kdbx");

			Disposable disposable = interactor.createNewDatabaseAndOpen(key, dbFile)
					.subscribe(this::onCreateDatabaseResult);

			disposables.add(disposable);

		} else {
			view.showError(context.getString(R.string.storage_is_not_selected));
		}
	}

	private void onCreateDatabaseResult(OperationResult<Boolean> result) {
		if (result.getResult() != null) {
			boolean created = result.getResult();

			if (created) {
				view.showGroupsScreen();
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

	@Override
	public void onStorageSelected(FileDescriptor selectedFile) {
		selectedStorageDir = selectedFile;

		if (selectedFile.getFsType() == FSType.REGULAR_FS) {
			File file = new File(selectedFile.getPath());

			if (FileUtils.isLocatedInPrivateStorage(file, context)) {
				view.setStorage(context.getString(R.string.private_storage), selectedFile.getPath());
			} else {
				view.setStorage(context.getString(R.string.public_storage), selectedFile.getPath());
			}
		} else if (selectedFile.getFsType() == FSType.DROPBOX) {
			view.setStorage(context.getString(R.string.dropbox), selectedFile.getPath());
		}
	}
}
