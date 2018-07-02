package com.ivanovsky.passnotes.data.safedb;

import com.ivanovsky.passnotes.data.safedb.model.Notepad;

import java.util.List;

import io.reactivex.Single;

public interface NotepadRepository {

	Single<List<Notepad>> getAllNotepads();
	void insert(Notepad notepad);
	boolean isTitleFree(String title);
}
