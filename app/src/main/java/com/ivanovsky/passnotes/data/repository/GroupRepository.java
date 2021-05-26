package com.ivanovsky.passnotes.data.repository;

import androidx.annotation.NonNull;

import com.ivanovsky.passnotes.data.entity.Group;
import com.ivanovsky.passnotes.data.entity.OperationResult;

import java.util.List;
import java.util.UUID;

public interface GroupRepository {

    @NonNull
    OperationResult<Group> getGroupByUid(@NonNull UUID groupUid);

    @NonNull
    OperationResult<List<Group>> getAllGroup();

    @NonNull
    OperationResult<Group> getRootGroup();

    @NonNull
    OperationResult<List<Group>> getChildGroups(UUID parentGroupUid);

    @NonNull
    OperationResult<Integer> getChildGroupsCount(UUID parentGroupUid);

    @NonNull
    OperationResult<Boolean> insert(Group group, UUID parentGroupUid);

    @NonNull
    OperationResult<Boolean> remove(UUID groupUid);
}
