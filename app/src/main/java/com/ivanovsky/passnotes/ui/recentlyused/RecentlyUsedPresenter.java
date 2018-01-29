package com.ivanovsky.passnotes.ui.recentlyused;

import android.content.Context;

import com.ivanovsky.passnotes.App;
import com.ivanovsky.passnotes.data.db.model.UsedFile;
import com.ivanovsky.passnotes.data.repository.UsedFileRepository;
import com.ivanovsky.passnotes.ui.core.FragmentState;

import java.util.List;

import javax.inject.Inject;

import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class RecentlyUsedPresenter implements RecentlyUsedContract.Presenter {

	@Inject
	UsedFileRepository repository;

	private Context context;
	private RecentlyUsedContract.View view;

	RecentlyUsedPresenter(Context context, RecentlyUsedContract.View view) {
		this.context = context;
		this.view = view;
		App.getDaggerComponent().inject(this);
	}

	@Override
	public void start() {
		view.setState(FragmentState.LOADING);

		loadData();
	}

	@Override
	public void stop() {
		//TODO: implement method
	}

	@Override
	public void loadData() {
		repository.getAllUsedFiles()
				.subscribeOn(Schedulers.newThread())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(this::onFilesLoaded);
	}

	private void onFilesLoaded(List<UsedFile> files) {
		if (files.size() != 0) {
			view.showRecentlyUsedFiles(files);
		} else {
			view.showNoItems();
		}
	}
}
