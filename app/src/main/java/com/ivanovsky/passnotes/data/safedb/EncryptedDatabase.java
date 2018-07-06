package com.ivanovsky.passnotes.data.safedb;

import com.ivanovsky.passnotes.data.safedb.dao.GroupDao;

public interface EncryptedDatabase {

	GroupDao getNotepadDao();
	GroupRepository getNotepadRepository();
}
