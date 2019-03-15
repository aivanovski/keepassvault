package com.ivanovsky.passnotes.data.repository.encdb;

import com.ivanovsky.passnotes.data.entity.OperationResult;
import com.ivanovsky.passnotes.data.repository.GroupRepository;
import com.ivanovsky.passnotes.data.repository.NoteRepository;
import com.ivanovsky.passnotes.data.repository.encdb.exception.EncryptedDatabaseException;

public interface EncryptedDatabase {

	GroupRepository getGroupRepository();
	NoteRepository getNoteRepository();
	OperationResult<Boolean> commit();
}
