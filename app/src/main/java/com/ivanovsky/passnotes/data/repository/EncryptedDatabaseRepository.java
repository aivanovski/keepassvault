package com.ivanovsky.passnotes.data.repository;

import com.ivanovsky.passnotes.data.entity.FileDescriptor;
import com.ivanovsky.passnotes.data.entity.OperationResult;
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabase;
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseKey;

public interface EncryptedDatabaseRepository {

	boolean isOpened();
	EncryptedDatabase getDatabase();
	NoteRepository getNoteRepository();
	GroupRepository getGroupRepository();
	TemplateRepository getTemplateRepository();
	OperationResult<EncryptedDatabase> open(EncryptedDatabaseKey key, FileDescriptor file);
	OperationResult<Boolean> createNew(EncryptedDatabaseKey key, FileDescriptor file);
	OperationResult<Boolean> close();
}
