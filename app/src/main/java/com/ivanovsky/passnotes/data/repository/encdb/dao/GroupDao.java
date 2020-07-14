package com.ivanovsky.passnotes.data.repository.encdb.dao;

import com.ivanovsky.passnotes.data.entity.Group;
import com.ivanovsky.passnotes.data.entity.OperationResult;

import java.util.List;
import java.util.UUID;

public interface GroupDao {

	OperationResult<List<Group>> getAll();
	OperationResult<Group> getRootGroup();
	OperationResult<List<Group>> getChildGroups(UUID parentGroupUid);
	OperationResult<UUID> insert(Group group, UUID parentGroupUid);
	OperationResult<Boolean> remove(UUID groupUid);
}
