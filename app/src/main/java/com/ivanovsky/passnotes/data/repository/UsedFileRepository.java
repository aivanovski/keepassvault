package com.ivanovsky.passnotes.data.repository;

import com.ivanovsky.passnotes.data.db.AppDatabase;
import com.ivanovsky.passnotes.data.db.model.UsedFile;

import java.util.List;
import java.util.concurrent.Callable;

import rx.Observable;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;

public class UsedFileRepository {

	@SuppressWarnings("unused")
	private static final String TAG = UsedFileRepository.class.getSimpleName();

	private final AppDatabase db;
	private volatile BehaviorSubject<List<UsedFile>> filesSubject;

	public UsedFileRepository(AppDatabase db) {
		this.db = db;
	}

	public BehaviorSubject<List<UsedFile>> getAllUsedFiles() {
		if (filesSubject == null) {
			filesSubject = BehaviorSubject.create();
		}

		loadAllInBackground()
				.subscribe(files -> {
					if (filesSubject != null) {
						filesSubject.onNext(files);
					}
				});

		return filesSubject;
	}

	private Observable<List<UsedFile>> loadAllInBackground() {
		Callable<List<UsedFile>> task = () -> db.getUsedFileDao().getAll();
		return Observable.fromCallable(task)
				.subscribeOn(Schedulers.newThread());
	}

	public void notifyDataSetChanged() {
		loadAllInBackground().subscribe(files -> {
			if (filesSubject != null) {
				filesSubject.onNext(files);
			}
		});
	}
}
