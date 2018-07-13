package com.ivanovsky.passnotes.data.keepass.dao;

import com.ivanovsky.passnotes.data.safedb.dao.NoteDao;
import com.ivanovsky.passnotes.data.safedb.model.Note;

import org.linguafranca.pwdb.kdbx.simple.SimpleDatabase;

import java.util.List;
import java.util.UUID;

public class KeepassNoteDao implements NoteDao {

	private final SimpleDatabase db;

	public KeepassNoteDao(SimpleDatabase db) {
		this.db = db;
	}

	@Override
	public List<Note> getByGroupUid(UUID groupUid) {
		return null;
	}

	@Override
	public UUID insert(Note note) {
		return null;
	}
}
