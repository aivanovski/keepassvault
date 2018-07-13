package com.ivanovsky.passnotes.data.keepass;

import com.ivanovsky.passnotes.data.keepass.dao.KeepassGroupDao;
import com.ivanovsky.passnotes.data.safedb.EncryptedDatabase;
import com.ivanovsky.passnotes.data.safedb.GroupRepository;
import com.ivanovsky.passnotes.data.safedb.dao.GroupDao;

import org.linguafranca.pwdb.Database;

public class KeepassDatabase implements EncryptedDatabase {

	private Database keepassDb;
	private final KeepassGroupDao groupDao;
	private final KeepassGroupRepository groupRepository;

	public KeepassDatabase(Database keepassDb) {
		this.keepassDb = keepassDb;
		this.groupDao = new KeepassGroupDao(keepassDb);
		this.groupRepository = new KeepassGroupRepository(groupDao);
	}

	@Override
	public GroupDao getGroupDao() {
		return groupDao;
	}

	@Override
	public GroupRepository getGroupRepository() {
		return groupRepository;
	}

	Database getKeepassDatabase() {
		return keepassDb;
	}
}
