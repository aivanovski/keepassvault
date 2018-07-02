package com.ivanovsky.passnotes.data.safedb.dao;

import com.ivanovsky.passnotes.data.safedb.model.Notepad;

import java.util.List;

public interface NotepadDao {

	List<Notepad> getAll();
	String insert(Notepad notepad);
}
