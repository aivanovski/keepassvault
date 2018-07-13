package com.ivanovsky.passnotes.data.safedb;

public interface EncryptedDatabase {

	GroupRepository getGroupRepository();
	void commit();
}
