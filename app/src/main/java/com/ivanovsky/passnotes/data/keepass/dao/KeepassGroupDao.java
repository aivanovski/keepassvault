package com.ivanovsky.passnotes.data.keepass.dao;

import com.ivanovsky.passnotes.data.safedb.dao.GroupDao;
import com.ivanovsky.passnotes.data.safedb.model.Group;

import org.linguafranca.pwdb.Database;
import org.linguafranca.pwdb.kdbx.simple.SimpleDatabase;
import org.linguafranca.pwdb.kdbx.simple.SimpleGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class KeepassGroupDao implements GroupDao {

	private final SimpleDatabase keepassDb;

	//TODO: constructor should have SimpleDatabase as argument
	public KeepassGroupDao(Database keepassDb) {
		this.keepassDb = (SimpleDatabase) keepassDb;
	}

	@Override
	public List<Group> getAll() {
		List<Group> groups = new ArrayList<>();

		putAllGroupsIntoListRecursively(groups, keepassDb.getRootGroup());

		return groups;
	}

	private void putAllGroupsIntoListRecursively(List<Group> groups, org.linguafranca.pwdb.Group group) {
		List childGroups = group.getGroups();
		if (childGroups != null && childGroups.size() != 0) {

			for (int i = 0; i < childGroups.size(); i++) {
				org.linguafranca.pwdb.Group childGroup = (org.linguafranca.pwdb.Group) childGroups.get(i);

				groups.add(createGroupFromKeepassGroup(childGroup));

				putAllGroupsIntoListRecursively(groups, childGroup);
			}
		}
	}

	private Group createGroupFromKeepassGroup(org.linguafranca.pwdb.Group keepassGroup) {
		Group result = new Group();

		result.setUid(keepassGroup.getUuid());
		result.setTitle(keepassGroup.getName());

		return result;
	}

	@Override
	public UUID insert(Group group) {
		UUID result = null;

		SimpleGroup rootGroup = keepassDb.getRootGroup();
		if (rootGroup != null) {
			SimpleGroup newGroup = keepassDb.newGroup(group.getTitle());

			rootGroup.addGroup(newGroup);

			result = newGroup.getUuid();
		}

		return result;
	}
}
