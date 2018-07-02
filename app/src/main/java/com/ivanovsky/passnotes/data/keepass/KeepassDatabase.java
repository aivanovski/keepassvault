package com.ivanovsky.passnotes.data.keepass;

import com.ivanovsky.passnotes.data.keepass.dao.KeepassNotepadDao;
import com.ivanovsky.passnotes.data.safedb.EncryptedDatabase;
import com.ivanovsky.passnotes.data.safedb.NotepadRepository;
import com.ivanovsky.passnotes.data.safedb.dao.NotepadDao;

import org.linguafranca.pwdb.Database;

public class KeepassDatabase implements EncryptedDatabase {

	private Database keepassDb;
	private final KeepassNotepadDao notepadDao;
	private final KeepassNotepadRepository notepadRepository;

	public KeepassDatabase(Database keepassDb) {
		this.keepassDb = keepassDb;
		this.notepadDao = new KeepassNotepadDao(keepassDb);
		this.notepadRepository = new KeepassNotepadRepository(notepadDao);
	}

	@Override
	public NotepadDao getNotepadDao() {
		return notepadDao;
	}

	@Override
	public NotepadRepository getNotepadRepository() {
		return notepadRepository;
	}
}
