package com.ivanovsky.passnotes.data.repository;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.ivanovsky.passnotes.data.safedb.SafeDatabase;
import com.ivanovsky.passnotes.data.safedb.dao.NotepadDao;
import com.ivanovsky.passnotes.data.safedb.model.Notepad;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import rx.Observable;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;

public class NotepadRepository {

	private final NotepadDao notepadDao;
	private final Object lock;
	private volatile BehaviorSubject<List<Notepad>> notepadsSubject;

	public NotepadRepository(SafeDatabase db) {
		this.notepadDao = db.getNotepadDao();
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
		Callable<List<Notepad>> task = notepadDao::getAll;
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

	public boolean isTitleFree(String title) {
		return !Stream.of(notepadDao.getAll())
				.anyMatch(notepad -> title.equals(notepad.getTitle()));
	}

	public void saveNotepad(Notepad notepad) {
		long id = notepadDao.insert(notepad);
		notepad.setId((int) id);
	}
}
