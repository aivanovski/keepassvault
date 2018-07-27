package com.ivanovsky.passnotes.data.repository;

import com.ivanovsky.passnotes.data.entity.Group;
import com.ivanovsky.passnotes.data.entity.OperationResult;

import java.util.List;

public interface GroupRepository {

	OperationResult<List<Group>> getAllGroup();
	OperationResult<Boolean> insert(Group group);
}
