package com.ivanovsky.passnotes.data.repository;

import com.ivanovsky.passnotes.data.db.AppDatabase;
import com.ivanovsky.passnotes.data.db.model.UsedFile;

import java.util.List;
import java.util.concurrent.Callable;

import rx.Observable;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public class UsedFileRepository {

	private final AppDatabase db;
	private volatile PublishSubject<List<UsedFile>> filesSubject;

	public UsedFileRepository(AppDatabase db) {
		this.db = db;
	}

	public PublishSubject<List<UsedFile>> getAllUsedFiles() {
		if (filesSubject == null) {
			filesSubject = PublishSubject.create();
		}

		Callable<List<UsedFile>> task = () -> db.getUsedFileDao().getAll();

		Observable.fromCallable(task)
				.subscribeOn(Schedulers.newThread())
				.subscribe(files -> {
					if (filesSubject != null) {
						filesSubject.onNext(files);
					}
				});

		return filesSubject;
	}
}
