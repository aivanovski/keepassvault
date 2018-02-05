package com.ivanovsky.passnotes.ui.notepads;

import android.content.Context;

import com.ivanovsky.passnotes.App;
import com.ivanovsky.passnotes.data.repository.NotepadRepository;
import com.ivanovsky.passnotes.data.safedb.SafeDatabaseProvider;
import com.ivanovsky.passnotes.data.safedb.model.Notepad;
import com.ivanovsky.passnotes.event.NotepadDataSetChangedEvent;
import com.ivanovsky.passnotes.ui.core.FragmentState;

import java.util.List;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class NotepadsPresenter implements NotepadsContract.Presenter {

	@Inject
	SafeDatabaseProvider safeDbProvider;

	private final String dbName;
	private final Context context;
	private final NotepadsContract.View view;
	private final CompositeDisposable disposables;

	NotepadsPresenter(Context context, NotepadsContract.View view, String dbName) {
		App.getDaggerComponent().inject(this);
		this.context = context;
		this.view = view;
		this.dbName = dbName;
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
		String openedDBName = safeDbProvider.getOpenedDBName();
		if (openedDBName != null && openedDBName.equals(dbName)) {
			NotepadRepository repository = safeDbProvider.getNotepadRepository();

			Disposable disposable = repository.getAllNotepads()
					.subscribeOn(Schedulers.newThread())
					.observeOn(AndroidSchedulers.mainThread())
					.subscribe(this::onNotepadsLoaded);
			disposables.add(disposable);

		} else {
			Disposable disposable = safeDbProvider.openDatabaseAsync(dbName + ".db")
					.subscribeOn(Schedulers.newThread())
					.observeOn(AndroidSchedulers.mainThread())
					.subscribe(db -> onDbOpened());
			disposables.add(disposable);
		}
	}

	private void onDbOpened() {
		NotepadRepository repository = safeDbProvider.getNotepadRepository();

		Disposable disposable = repository.getAllNotepads()
				.subscribeOn(Schedulers.newThread())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(this::onNotepadsLoaded);
		disposables.add(disposable);
	}

	private void onNotepadsLoaded(List<Notepad> notepads) {
		if (notepads.size() != 0) {
			view.showNotepads(notepads);
		} else {
			view.showNoItems();
		}
	}

	public void onEvent(NotepadDataSetChangedEvent event) {
		loadData();
	}
}
