package com.ivanovsky.passnotes.data.safedb;

import com.ivanovsky.passnotes.data.safedb.dao.NotepadDao;

public interface EncryptedDatabase {

	NotepadDao getNotepadDao();
	NotepadRepository getNotepadRepository();
}
