package com.ivanovsky.passnotes.data.safedb;

import com.ivanovsky.passnotes.data.safedb.dao.GroupDao;

public interface EncryptedDatabase {

	GroupDao getGroupDao();
	GroupRepository getGroupRepository();
}
