package com.ivanovsky.passnotes.ui.recentlyused;

import android.content.Context;

import com.ivanovsky.passnotes.App;
import com.ivanovsky.passnotes.R;
import com.ivanovsky.passnotes.data.db.model.UsedFile;
import com.ivanovsky.passnotes.data.keepass.KeepassDatabaseKey;
import com.ivanovsky.passnotes.data.repository.UsedFileRepository;
import com.ivanovsky.passnotes.data.safedb.EncryptedDatabaseProvider;
import com.ivanovsky.passnotes.event.UsedFileDataSetChangedEvent;
import com.ivanovsky.passnotes.ui.core.FragmentState;

import java.io.File;
import java.util.List;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class RecentlyUsedPresenter implements RecentlyUsedContract.Presenter {

	private static final String TAG = RecentlyUsedPresenter.class.getSimpleName();

	@Inject
	UsedFileRepository repository;

	@Inject
	EncryptedDatabaseProvider dbProvider;

	private Context context;
	private RecentlyUsedContract.View view;
	private CompositeDisposable disposables;

	RecentlyUsedPresenter(Context context, RecentlyUsedContract.View view) {
		App.getDaggerComponent().inject(this);

		this.context = context;
		this.view = view;
		this.disposables = new CompositeDisposable();
	}

	@Override
	public void start() {
		view.setState(FragmentState.LOADING);
		EventBus.getDefault().register(this);
		loadData();
	}

	@Override
	public void stop() {
		EventBus.getDefault().unregister(this);
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

		dbProvider.openAsync(key, dbFile)
				.subscribeOn(Schedulers.newThread())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(db -> onDbOpened(),
						this::onFailedToOpenDb);

//		view.showNotepadsScreen(new DbDescriptor(password, dbFile));
	}

	private void onFailedToOpenDb(Throwable exception) {
		view.showError(context.getString(R.string.error_was_occurred));
	}

	private void onDbOpened() {
		view.showNotepadsScreen();
	}

	public void onEvent(UsedFileDataSetChangedEvent event) {
		loadData();
	}
}
