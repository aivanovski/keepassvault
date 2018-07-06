package com.ivanovsky.passnotes.data.keepass.dao;

import com.ivanovsky.passnotes.data.safedb.dao.GroupDao;
import com.ivanovsky.passnotes.data.safedb.model.Group;

import org.linguafranca.pwdb.Database;

import java.util.List;

public class KeepassGroupDao implements GroupDao {

	private final Database keepassDb;

	public KeepassGroupDao(Database keepassDb) {
		this.keepassDb = keepassDb;
	}

	@Override
	public List<Group> getAll() {
		return null;
	}

	@Override
	public String insert(Group group) {
		return null;
	}
}
