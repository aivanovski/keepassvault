package com.ivanovsky.passnotes.data.repository;

import com.ivanovsky.passnotes.data.entity.FileDescriptor;
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabase;
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseKey;
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseOperationException;

import java.io.File;

import io.reactivex.Single;

public interface EncryptedDatabaseRepository {

	boolean isOpened();
	EncryptedDatabase getDatabase();
	Single<EncryptedDatabase> openAsync(EncryptedDatabaseKey key, FileDescriptor file);
	boolean createNew(EncryptedDatabaseKey key, FileDescriptor file);
	void close();
}
