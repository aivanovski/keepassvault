package com.ivanovsky.passnotes.presentation.unlock;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;

import com.ivanovsky.passnotes.R;
import com.ivanovsky.passnotes.data.ObserverBus;
import com.ivanovsky.passnotes.data.entity.OperationResult;
import com.ivanovsky.passnotes.data.entity.UsedFile;
import com.ivanovsky.passnotes.data.repository.keepass.KeepassDatabaseKey;
import com.ivanovsky.passnotes.domain.interactor.unlock.UnlockInteractor;
import com.ivanovsky.passnotes.injection.Injector;
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor;
import com.ivanovsky.passnotes.presentation.core.FragmentState;
import com.ivanovsky.passnotes.presentation.core.ScreenState;
import com.ivanovsky.passnotes.presentation.core.livedata.SingleLiveAction;

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

	private MutableLiveData<List<UsedFile>> recentlyUsedFilesData;
	private MutableLiveData<ScreenState> screenStateData;
	private SingleLiveAction<Void> showGroupsScreenAction;
	private SingleLiveAction<Void> showNewDatabaseScreenAction;
	private SingleLiveAction<Void> hideKeyboardAction;
	private Context context;
	private UnlockContract.View view;
	private CompositeDisposable disposables;

	UnlockPresenter(Context context, UnlockContract.View view) {
		Injector.getInstance().getAppComponent().inject(this);
		this.context = context;
		this.view = view;

		disposables = new CompositeDisposable();
		recentlyUsedFilesData = new MutableLiveData<>();
		screenStateData = new MutableLiveData<>();
		showGroupsScreenAction = new SingleLiveAction<>();
		showNewDatabaseScreenAction = new SingleLiveAction<>();
		hideKeyboardAction = new SingleLiveAction<>();
	}

	@Override
	public LiveData<List<UsedFile>> getRecentlyUsedFilesData() {
		return recentlyUsedFilesData;
	}

	@Override
	public LiveData<ScreenState> getScreenStateData() {
		return screenStateData;
	}

	@Override
	public LiveData<Void> getShowGroupsScreenAction() {
		return showGroupsScreenAction;
	}

	@Override
	public LiveData<Void> getShowNewDatabaseScreenAction() {
		return showNewDatabaseScreenAction;
	}

	@Override
	public LiveData<Void> getHideKeyboardAction() {
		return hideKeyboardAction;
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
				recentlyUsedFilesData.setValue(files);
				screenStateData.setValue(ScreenState.data());
			} else {
				screenStateData.setValue(ScreenState.empty(
						context.getString(R.string.no_files_to_open)));
			}
		} else {
			String message = errorInteractor.processAndGetMessage(result.getError());
			screenStateData.setValue(ScreenState.error(message));
		}
	}

	@Override
	public void onUnlockButtonClicked(String password, File dbFile) {
		hideKeyboardAction.call();
		screenStateData.setValue(ScreenState.loading());

		KeepassDatabaseKey key = new KeepassDatabaseKey(password);

		Disposable disposable = interactor.openDatabase(key, dbFile)
				.subscribe(this::onOpenDatabaseResult);

		disposables.add(disposable);
	}

	private void onOpenDatabaseResult(OperationResult<Boolean> result) {
		if (result.getResult() != null) {
			showGroupsScreenAction.call();
		} else {
			String message = errorInteractor.processAndGetMessage(result.getError());
			screenStateData.setValue(ScreenState.error(message));
		}
	}

	@Override
	public void onUsedFileDataSetChanged() {
		loadData();
	}
}
