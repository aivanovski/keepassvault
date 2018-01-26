package com.ivanovsky.passnotes.data.repository;

import com.ivanovsky.passnotes.data.safedb.SafeDatabase;
import com.ivanovsky.passnotes.data.safedb.model.Notepad;

import java.util.List;
import java.util.concurrent.Callable;

import rx.Observable;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;

public class NotepadRepository {

	private final SafeDatabase db;
	private final Object lock;
	private volatile BehaviorSubject<List<Notepad>> notepadsSubject;

	public NotepadRepository(SafeDatabase db) {
		this.db = db;
		this.lock = new Object();
	}

	public BehaviorSubject<List<Notepad>> getAllNotepads() {
		synchronized (lock) {
			if (notepadsSubject == null) {
				notepadsSubject = BehaviorSubject.create();
			}

			loadAllInBackground()
					.subscribe(notepads -> {
						if (notepadsSubject != null) {
							notepadsSubject.onNext(notepads);
						}
					});
		}

		return notepadsSubject;
	}

	private Observable<List<Notepad>> loadAllInBackground() {
		Callable<List<Notepad>> task = () -> db.getNotepadDao().getAll();
		return Observable.fromCallable(task)
				.subscribeOn(Schedulers.newThread());
	}

	public void notifyDataSetChanged() {
		loadAllInBackground().subscribe(notepads -> {
			if (notepadsSubject != null) {
				notepadsSubject.onNext(notepads);
			}
		});
	}
}
