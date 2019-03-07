package com.ivanovsky.passnotes.data.repository.keepass;

import com.ivanovsky.passnotes.data.entity.OperationResult;
import com.ivanovsky.passnotes.data.repository.GroupRepository;
import com.ivanovsky.passnotes.data.repository.encdb.dao.GroupDao;
import com.ivanovsky.passnotes.data.entity.Group;

import java.util.List;
import java.util.UUID;

public class KeepassGroupRepository implements GroupRepository {

	private final GroupDao dao;

	KeepassGroupRepository(GroupDao dao) {
		this.dao = dao;
	}

	@Override
	public OperationResult<List<Group>> getAllGroup() {
		return dao.getAll();
	}

	@Override
	public OperationResult<Boolean> insert(Group group) {
		OperationResult<Boolean> result = new OperationResult<>();

		OperationResult<UUID> insertResult = dao.insert(group);
		if (insertResult.getObj() != null) {
			group.setUid(insertResult.getObj());
			result.setObj(true);
		} else {
			result.setError(insertResult.getError());
		}

		return result;
	}
}
