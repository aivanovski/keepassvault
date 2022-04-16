package com.ivanovsky.passnotes.data.repository;

import androidx.annotation.NonNull;

import com.ivanovsky.passnotes.data.entity.Group;
import com.ivanovsky.passnotes.data.entity.GroupEntity;
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
    OperationResult<List<Group>> getChildGroups(@NonNull UUID parentGroupUid);

    @NonNull
    OperationResult<Integer> getChildGroupsCount(@NonNull UUID parentGroupUid);

    @NonNull
    OperationResult<UUID> insert(@NonNull GroupEntity group);

    @NonNull
    OperationResult<Boolean> remove(@NonNull UUID groupUid);

    @NonNull
    OperationResult<List<Group>> find(@NonNull String query);

    @NonNull
    OperationResult<Boolean> update(@NonNull GroupEntity group);
}
