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

import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_FIND_GROUP;
import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_FIND_ROOT_GROUP;
import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_REMOVE_ROOT_GROUP;
import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_UNKNOWN_ERROR;
import static com.ivanovsky.passnotes.data.entity.OperationError.newDbError;

public class KeepassGroupDao implements GroupDao {

	private final KeepassDatabase db;
	private volatile OnGroupRemoveLister removeLister;

	public interface OnGroupRemoveLister {
		void onGroupRemoved(UUID groupUid);
	}

	public KeepassGroupDao(KeepassDatabase db) {
		this.db = db;
	}

	public void setOnGroupRemoveLister(OnGroupRemoveLister removeLister) {
		this.removeLister = removeLister;
	}

	@Override
	public OperationResult<Group> getRootGroup() {
		SimpleDatabase keepassDb = db.getKeepassDatabase();

		SimpleGroup rootGroup;
		synchronized (db.getLock()) {
			rootGroup = keepassDb.getRootGroup();
			if (rootGroup == null) {
				return OperationResult.error(newDbError(MESSAGE_FAILED_TO_FIND_ROOT_GROUP));
			}
		}

		return OperationResult.success(createGroupFromKeepassGroup(rootGroup));
	}

	@Override
	public OperationResult<List<Group>> getChildGroups(UUID parentGroupUid) {
		List<Group> groups = new ArrayList<>();

		synchronized (db.getLock()) {
			SimpleGroup parentGroup = db.getKeepassDatabase().findGroup(parentGroupUid);
			if (parentGroup == null) {
				return OperationResult.error(newDbError(MESSAGE_FAILED_TO_FIND_GROUP));
			}

			List<SimpleGroup> childGroups = parentGroup.getGroups();
			if (childGroups != null) {
				groups.addAll(createGroupsFromKeepassGroups(childGroups));
			}
		}

		return OperationResult.success(groups);
	}

	@Override
	public OperationResult<List<Group>> getAll() {
		List<Group> groups = new ArrayList<>();

		synchronized (db.getLock()) {
			SimpleDatabase keepassDb = db.getKeepassDatabase();

			putAllGroupsIntoListRecursively(groups, keepassDb.getRootGroup());
		}

		return OperationResult.success(groups);
	}

	private void putAllGroupsIntoListRecursively(List<Group> groups, SimpleGroup group) {
		List<SimpleGroup> childGroups = group.getGroups();
		if (childGroups != null && childGroups.size() != 0) {

			for (int i = 0; i < childGroups.size(); i++) {
				SimpleGroup childGroup = childGroups.get(i);

				groups.add(createGroupFromKeepassGroup(childGroup));

				putAllGroupsIntoListRecursively(groups, childGroup);
			}
		}
	}

	private List<Group> createGroupsFromKeepassGroups(List<SimpleGroup> childGroups) {
		List<Group> groups = new ArrayList<>();

		for (int i = 0; i < childGroups.size(); i++) {
			SimpleGroup childGroup = childGroups.get(i);

			groups.add(createGroupFromKeepassGroup(childGroup));
		}

		return groups;
	}

	private Group createGroupFromKeepassGroup(SimpleGroup keepassGroup) {
		Group result = new Group();

		result.setUid(keepassGroup.getUuid());
		result.setTitle(keepassGroup.getName());

		return result;
	}

	@Override
	public OperationResult<UUID> insert(Group group, UUID parentGroupUid) {
		SimpleDatabase keepassDb = db.getKeepassDatabase();

		SimpleGroup newGroup;
		synchronized (db.getLock()) {
			SimpleGroup parentGroup = keepassDb.findGroup(parentGroupUid);
			if (parentGroup == null) {
				return OperationResult.error(newDbError(MESSAGE_FAILED_TO_FIND_GROUP));
			}

			newGroup = keepassDb.newGroup(group.getTitle());

			parentGroup.addGroup(newGroup);
			if (newGroup.getUuid() == null) {
				return OperationResult.error(newDbError(MESSAGE_UNKNOWN_ERROR));
			}

			OperationResult<Boolean> commitResult = db.commit();
			if (commitResult.isFailed()) {
				keepassDb.deleteGroup(newGroup.getUuid());
				return commitResult.takeError();
			}
		}

		return OperationResult.success(newGroup.getUuid());
	}

	@Override
	public OperationResult<Boolean> remove(UUID groupUid) {
		SimpleDatabase keepassDb = db.getKeepassDatabase();

		synchronized (db.getLock()) {
			SimpleGroup group = keepassDb.findGroup(groupUid);
			if (group == null) {
				return OperationResult.error(newDbError(MESSAGE_FAILED_TO_FIND_GROUP));
			}

			SimpleGroup parentGroup = group.getParent();
			if (parentGroup == null) {
				return OperationResult.error(newDbError(MESSAGE_FAILED_TO_REMOVE_ROOT_GROUP));
			}

			parentGroup.removeGroup(group);

			OperationResult<Boolean> commitResult = db.commit();
			if (commitResult.isFailed()) {
				return commitResult.takeError();
			}
		}

		if (removeLister != null) {
			removeLister.onGroupRemoved(groupUid);
		}

		return OperationResult.success(true);
	}
}
