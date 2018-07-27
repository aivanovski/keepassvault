package com.ivanovsky.passnotes.presentation.unlock;

import android.content.Context;

import com.ivanovsky.passnotes.data.ObserverBus;
import com.ivanovsky.passnotes.data.entity.OperationResult;
import com.ivanovsky.passnotes.data.entity.UsedFile;
import com.ivanovsky.passnotes.data.repository.keepass.KeepassDatabaseKey;
import com.ivanovsky.passnotes.domain.interactor.unlock.UnlockInteractor;
import com.ivanovsky.passnotes.injection.Injector;
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor;
import com.ivanovsky.passnotes.presentation.core.FragmentState;

import java.io.File;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class UnlockPresenter implements
		UnlockContract.Presenter,
		ObserverBus.UsedFileDataSetObserver {

	@Inject
	UnlockInteractor interactor;

	@Inject
	ErrorInteractor errorInteractor;

	@Inject
	ObserverBus observerBus;

	private UnlockContract.View view;
	private CompositeDisposable disposables;

	UnlockPresenter(Context context, UnlockContract.View view) {
		Injector.getInstance().getAppComponent().inject(this);
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
		Disposable disposable = interactor.getRecentlyOpenedFiles()
				.subscribe(this::onGetRecentlyOpenedFilesResult);

		disposables.add(disposable);
	}

	private void onGetRecentlyOpenedFilesResult(OperationResult<List<UsedFile>> result) {
		if (result.isSuccessful()) {
			List<UsedFile> files = result.getResult();
			if (files.size() != 0) {
				view.showRecentlyUsedFiles(files);
			} else {
				view.showNoItems();
			}
		} else {
			view.showError(errorInteractor.processAndGetMessage(result.getError()));
		}
	}

	@Override
	public void onUnlockButtonClicked(String password, File dbFile) {
		view.showLoading();

		KeepassDatabaseKey key = new KeepassDatabaseKey(password);

		Disposable disposable = interactor.openDatabase(key, dbFile)
				.subscribe(this::onOpenDatabaseResult);

		disposables.add(disposable);
	}

	private void onOpenDatabaseResult(OperationResult<Boolean> result) {
		if (result.getResult() != null) {
			view.showGroupsScreen();
		} else {
			view.showError(errorInteractor.processAndGetMessage(result.getError()));
		}
	}

	@Override
	public void onUsedFileDataSetChanged() {
		loadData();
	}
}
