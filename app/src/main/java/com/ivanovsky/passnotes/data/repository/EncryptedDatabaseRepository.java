package com.ivanovsky.passnotes.data.repository;

import com.ivanovsky.passnotes.data.entity.FileDescriptor;
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabase;
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseKey;
import com.ivanovsky.passnotes.data.repository.encdb.exception.EncryptedDatabaseException;

public interface EncryptedDatabaseRepository {

	EncryptedDatabase open(EncryptedDatabaseKey key, FileDescriptor file) throws EncryptedDatabaseException;
	boolean createNew(EncryptedDatabaseKey key, FileDescriptor file);
	void close();
}
