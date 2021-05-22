package com.ivanovsky.passnotes.data.repository.encdb.dao;

import androidx.annotation.NonNull;

import com.ivanovsky.passnotes.data.entity.Group;
import com.ivanovsky.passnotes.data.entity.OperationResult;

import java.util.List;
import java.util.UUID;

public interface GroupDao {

    @NonNull
    OperationResult<List<Group>> getAll();

    @NonNull
    OperationResult<Group> getRootGroup();

    @NonNull
    OperationResult<List<Group>> getChildGroups(UUID parentGroupUid);

    @NonNull
    OperationResult<UUID> insert(Group group, UUID parentGroupUid);

    @NonNull
    OperationResult<Boolean> remove(UUID groupUid);
}
