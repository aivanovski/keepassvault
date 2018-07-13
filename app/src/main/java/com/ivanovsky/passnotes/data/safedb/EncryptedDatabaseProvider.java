package com.ivanovsky.passnotes.data.safedb;

import java.io.File;

import io.reactivex.Single;

public interface EncryptedDatabaseProvider {

	boolean isOpened();
	EncryptedDatabase getDatabase();
	EncryptedDatabase open(EncryptedDatabaseKey key, File file) throws EncryptedDatabaseOperationException;
	Single<EncryptedDatabase> openAsync(EncryptedDatabaseKey key, File file);
	boolean createNew(EncryptedDatabaseKey key, File file);
	String getOpenedDatabasePath();
	boolean commit();
	void close();
}
