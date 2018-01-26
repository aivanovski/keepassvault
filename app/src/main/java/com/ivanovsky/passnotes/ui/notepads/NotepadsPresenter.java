package com.ivanovsky.passnotes.ui.notepads;

import android.content.Context;

import com.ivanovsky.passnotes.App;
import com.ivanovsky.passnotes.data.repository.NotepadRepository;
import com.ivanovsky.passnotes.data.safedb.SafeDatabaseProvider;
import com.ivanovsky.passnotes.data.safedb.model.Notepad;
import com.ivanovsky.passnotes.ui.core.FragmentState;

import java.util.List;

import javax.inject.Inject;

import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class NotepadsPresenter implements NotepadsContract.Presenter {

	@Inject
	SafeDatabaseProvider safeDbProvider;

	private final Context context;
	private final NotepadsContract.View view;
	private final NotepadRepository repository;

	NotepadsPresenter(Context context, NotepadsContract.View view) {
		App.getDaggerComponent().inject(this);
		this.context = context;
		this.view = view;
		this.repository = safeDbProvider.getNotepadRepository();
	}

	@Override
	public void start() {
		view.setState(FragmentState.LOADING);

		loadData();
	}

	@Override
	public void loadData() {
		repository.getAllNotepads()
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

	@Override
	public void showNewNotepadScreen() {
	}
}
