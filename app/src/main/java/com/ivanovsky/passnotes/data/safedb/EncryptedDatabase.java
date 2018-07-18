package com.ivanovsky.passnotes.data.safedb;

public interface EncryptedDatabase {

	GroupRepository getGroupRepository();
	NoteRepository getNoteRepository();
	boolean commit();
}
