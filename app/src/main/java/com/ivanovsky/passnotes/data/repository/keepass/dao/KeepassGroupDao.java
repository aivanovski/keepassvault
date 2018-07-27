package com.ivanovsky.passnotes.data.repository.keepass.dao;

import com.ivanovsky.passnotes.data.entity.OperationResult;
import com.ivanovsky.passnotes.data.repository.keepass.KeepassDatabase;
import com.ivanovsky.passnotes.data.repository.encdb.dao.GroupDao;
import com.ivanovsky.passnotes.data.entity.Group;

import org.linguafranca.pwdb.kdbx.simple.SimpleDatabase;
import org.linguafranca.pwdb.kdbx.simple.SimpleGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class KeepassGroupDao implements GroupDao {

	private final KeepassDatabase db;

	public KeepassGroupDao(KeepassDatabase db) {
		this.db = db;
	}

	@Override
	public OperationResult<List<Group>> getAll() {
		List<Group> groups = new ArrayList<>();

		SimpleDatabase keepassDb = db.getKeepassDatabase();

		putAllGroupsIntoListRecursively(groups, keepassDb.getRootGroup());

		return OperationResult.success(groups);
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
	public OperationResult<UUID> insert(Group group) {
		UUID result = null;

		SimpleDatabase keepassDb = db.getKeepassDatabase();

		SimpleGroup rootGroup = keepassDb.getRootGroup();
		if (rootGroup != null) {
			SimpleGroup newGroup = keepassDb.newGroup(group.getTitle());

			rootGroup.addGroup(newGroup);

			if (newGroup.getUuid() != null && db.commit()) {
				result = newGroup.getUuid();
			}
		}

		return OperationResult.success(result);
	}
}
