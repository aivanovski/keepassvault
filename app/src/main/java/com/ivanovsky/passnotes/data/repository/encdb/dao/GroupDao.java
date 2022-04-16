package com.ivanovsky.passnotes.data.repository.encdb.dao;

import androidx.annotation.NonNull;

import com.ivanovsky.passnotes.data.entity.Group;
import com.ivanovsky.passnotes.data.entity.GroupEntity;
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
    OperationResult<UUID> insert(GroupEntity group);

    @NonNull
    OperationResult<Boolean> remove(UUID groupUid);

    @NonNull
    OperationResult<Group> getGroupByUid(@NonNull UUID groupUid);

    @NonNull
    OperationResult<Boolean> update(@NonNull GroupEntity group);
}
