package com.ivanovsky.passnotes.data.repository.encdb.dao;

import androidx.annotation.NonNull;
import com.ivanovsky.passnotes.data.entity.Group;
import com.ivanovsky.passnotes.data.entity.GroupEntity;
import com.ivanovsky.passnotes.data.entity.OperationResult;
import com.ivanovsky.passnotes.data.repository.encdb.ContentWatcher;
import java.util.List;
import java.util.UUID;

public interface GroupDao {

    @NonNull
    OperationResult<List<Group>> getAll();

    @NonNull
    OperationResult<Group> getRootGroup();

    @NonNull
    OperationResult<List<Group>> getChildGroups(@NonNull UUID parentGroupUid);

    @NonNull
    OperationResult<UUID> insert(@NonNull GroupEntity group);

    // TODO: this method is used only by TemplateDaoImpl
    @NonNull
    OperationResult<UUID> insert(@NonNull GroupEntity group, boolean doCommit);

    @NonNull
    OperationResult<Boolean> remove(@NonNull UUID groupUid);

    @NonNull
    OperationResult<Group> getGroupByUid(@NonNull UUID groupUid);

    @NonNull
    OperationResult<Boolean> update(@NonNull GroupEntity group);

    @NonNull
    OperationResult<List<Group>> find(@NonNull String query);

    @NonNull
    ContentWatcher<Group> getContentWatcher();
}
