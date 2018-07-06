package com.ivanovsky.passnotes.data.keepass;

import com.ivanovsky.passnotes.data.keepass.dao.KeepassGroupDao;
import com.ivanovsky.passnotes.data.safedb.EncryptedDatabase;
import com.ivanovsky.passnotes.data.safedb.GroupRepository;
import com.ivanovsky.passnotes.data.safedb.dao.GroupDao;

import org.linguafranca.pwdb.Database;

public class KeepassDatabase implements EncryptedDatabase {

	private Database keepassDb;
	private final KeepassGroupDao notepadDao;
	private final KeepassGroupRepository notepadRepository;

	public KeepassDatabase(Database keepassDb) {
		this.keepassDb = keepassDb;
		this.notepadDao = new KeepassGroupDao(keepassDb);
		this.notepadRepository = new KeepassGroupRepository(notepadDao);
	}

	@Override
	public GroupDao getNotepadDao() {
		return notepadDao;
	}

	@Override
	public GroupRepository getNotepadRepository() {
		return notepadRepository;
	}
}
