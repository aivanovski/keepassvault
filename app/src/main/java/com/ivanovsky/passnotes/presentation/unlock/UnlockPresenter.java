package com.ivanovsky.passnotes.presentation.unlock;

import android.content.Context;

import com.ivanovsky.passnotes.App;
import com.ivanovsky.passnotes.R;
import com.ivanovsky.passnotes.data.ObserverBus;
import com.ivanovsky.passnotes.data.entity.FileDescriptor;
import com.ivanovsky.passnotes.data.entity.UsedFile;
import com.ivanovsky.passnotes.data.repository.keepass.KeepassDatabaseKey;
import com.ivanovsky.passnotes.data.repository.UsedFileRepository;
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository;
import com.ivanovsky.passnotes.presentation.core.FragmentState;

import java.io.File;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class UnlockPresenter implements
		UnlockContract.Presenter,
		ObserverBus.UsedFileDataSetObserver {

	private static final String TAG = UnlockPresenter.class.getSimpleName();

	@Inject
	UsedFileRepository repository;

	@Inject
	EncryptedDatabaseRepository dbProvider;

	@Inject
	ObserverBus observerBus;

	private Context context;
	private UnlockContract.View view;
	private CompositeDisposable disposables;

	UnlockPresenter(Context context, UnlockContract.View view) {
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
		Disposable disposable = repository.getAllUsedFiles()
				.subscribeOn(Schedulers.newThread())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(this::onFilesLoaded);

		disposables.add(disposable);
	}

	private void onFilesLoaded(List<UsedFile> files) {
		if (files.size() != 0) {
			view.showRecentlyUsedFiles(files);
		} else {
			view.showNoItems();
		}
	}

	@Override
	public void onUnlockButtonClicked(String password, File dbFile) {
		view.showLoading();

		if (dbProvider.isOpened()) {
			dbProvider.close();
		}

		KeepassDatabaseKey key = new KeepassDatabaseKey(password);

		dbProvider.openAsync(key, FileDescriptor.newRegularFile(dbFile))
				.subscribeOn(Schedulers.newThread())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(db -> onDbOpened(),
						this::onFailedToOpenDb);
	}

	private void onFailedToOpenDb(Throwable exception) {
		view.showError(context.getString(R.string.error_was_occurred));
	}

	private void onDbOpened() {
		view.showGroupsScreen();
	}

	@Override
	public void onUsedFileDataSetChanged() {
		loadData();
	}
}
