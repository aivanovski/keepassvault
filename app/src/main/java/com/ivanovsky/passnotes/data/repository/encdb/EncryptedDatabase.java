package com.ivanovsky.passnotes.data.repository.encdb;

import com.ivanovsky.passnotes.data.repository.GroupRepository;
import com.ivanovsky.passnotes.data.repository.NoteRepository;

public interface EncryptedDatabase {

	GroupRepository getGroupRepository();
	NoteRepository getNoteRepository();
	boolean commit();
}
