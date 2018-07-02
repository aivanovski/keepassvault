package com.ivanovsky.passnotes.data.keepass.dao;

import com.ivanovsky.passnotes.data.safedb.dao.NotepadDao;
import com.ivanovsky.passnotes.data.safedb.model.Notepad;

import org.linguafranca.pwdb.Database;

import java.util.List;

public class KeepassNotepadDao implements NotepadDao {

	private final Database keepassDb;

	public KeepassNotepadDao(Database keepassDb) {
		this.keepassDb = keepassDb;
	}

	@Override
	public List<Notepad> getAll() {
		return null;
	}

	@Override
	public String insert(Notepad notepad) {
		return null;
	}
}
