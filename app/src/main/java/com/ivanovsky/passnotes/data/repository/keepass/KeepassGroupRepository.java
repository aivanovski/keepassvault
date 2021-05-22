package com.ivanovsky.passnotes.data.repository.keepass;

import androidx.annotation.NonNull;

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

    @NonNull
    @Override
    public OperationResult<List<Group>> getAllGroup() {
        return dao.getAll();
    }

    @NonNull
    @Override
    public OperationResult<Group> getRootGroup() {
        return dao.getRootGroup();
    }

    @NonNull
    @Override
    public OperationResult<List<Group>> getChildGroups(UUID parentGroupUid) {
        return dao.getChildGroups(parentGroupUid);
    }

    @NonNull
    @Override
    public OperationResult<Integer> getChildGroupsCount(UUID parentGroupUid) {
        OperationResult<List<Group>> childGroupsResult = getChildGroups(parentGroupUid);
        if (childGroupsResult.isFailed()) {
            return childGroupsResult.takeError();
        }

        List<Group> groups = childGroupsResult.getObj();
        return childGroupsResult.takeStatusWith(groups.size());
    }

    @NonNull
    @Override
    public OperationResult<Boolean> insert(Group group, UUID parentGroupUid) {
        OperationResult<Boolean> result = new OperationResult<>();

        OperationResult<UUID> insertResult = dao.insert(group, parentGroupUid);
        if (insertResult.getObj() != null) {
            group.setUid(insertResult.getObj());
            result.setObj(true);
        } else {
            result.setError(insertResult.getError());
        }

        return result;
    }

    @NonNull
    @Override
    public OperationResult<Boolean> remove(UUID groupUid) {
        return dao.remove(groupUid);
    }
}
