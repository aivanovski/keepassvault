package com.ivanovsky.passnotes.data.repository.keepass;

import androidx.annotation.NonNull;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.ivanovsky.passnotes.data.entity.GroupEntity;
import com.ivanovsky.passnotes.data.entity.OperationResult;
import com.ivanovsky.passnotes.data.repository.GroupRepository;
import com.ivanovsky.passnotes.data.repository.encdb.dao.GroupDao;
import com.ivanovsky.passnotes.data.entity.Group;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class KeepassGroupRepository implements GroupRepository {

    private final GroupDao dao;

    KeepassGroupRepository(GroupDao dao) {
        this.dao = dao;
    }

    @NonNull
    @Override
    public OperationResult<Group> getGroupByUid(@NonNull UUID groupUid) {
        return dao.getGroupByUid(groupUid);
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
    public OperationResult<List<Group>> getChildGroups(@NonNull UUID parentGroupUid) {
        return dao.getChildGroups(parentGroupUid);
    }

    @NonNull
    @Override
    public OperationResult<UUID> insert(@NonNull GroupEntity group) {
        return dao.insert(group);
    }

    @NonNull
    @Override
    public OperationResult<Boolean> remove(@NonNull UUID groupUid) {
        return dao.remove(groupUid);
    }

    @NonNull
    @Override
    public OperationResult<List<Group>> find(@NonNull String query) {
        OperationResult<List<Group>> allGroupsResult = dao.getAll();
        if (allGroupsResult.isFailed()) {
            return allGroupsResult.takeError();
        }

        String loweredQuery = query.toLowerCase(Locale.getDefault());
        List<Group> allGroups = allGroupsResult.getObj();
        List<Group> matchedGroups = Stream.of(allGroups)
                .filter(group ->
                        group.getTitle()
                                .toLowerCase(Locale.getDefault())
                                .contains(loweredQuery)
                )
                .collect(Collectors.toList());

        return OperationResult.success(matchedGroups);
    }

    @NonNull
    @Override
    public OperationResult<Boolean> update(@NonNull GroupEntity group) {
        return dao.update(group);
    }
}
