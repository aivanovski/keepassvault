package com.ivanovsky.passnotes.data.repository;

import com.ivanovsky.passnotes.data.entity.Group;
import com.ivanovsky.passnotes.data.entity.OperationResult;

import java.util.List;
import java.util.UUID;

public interface GroupRepository {

	OperationResult<List<Group>> getAllGroup();
	OperationResult<Group> getRootGroup();
	OperationResult<List<Group>> getChildGroups(UUID parentGroupUid);
	OperationResult<Integer> getChildGroupsCount(UUID parentGroupUid);
	OperationResult<Boolean> insert(Group group, UUID parentGroupUid);
}
