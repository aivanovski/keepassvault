package com.ivanovsky.passnotes.data.repository.keepass.dao;

import androidx.annotation.NonNull;

import com.ivanovsky.passnotes.data.entity.OperationResult;
import com.ivanovsky.passnotes.data.repository.keepass.KeepassDatabase;
import com.ivanovsky.passnotes.data.repository.encdb.dao.GroupDao;
import com.ivanovsky.passnotes.data.entity.Group;

import org.linguafranca.pwdb.kdbx.simple.SimpleDatabase;
import org.linguafranca.pwdb.kdbx.simple.SimpleGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_COMPLETE_OPERATION;
import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_FIND_GROUP;
import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_FIND_ROOT_GROUP;
import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_UNKNOWN_ERROR;
import static com.ivanovsky.passnotes.data.entity.OperationError.newDbError;

public class KeepassGroupDao implements GroupDao {

    private final KeepassDatabase db;
    private final List<OnGroupRemoveLister> removeListeners;

    public interface OnGroupRemoveLister {
        void onGroupRemoved(UUID groupUid);
    }

    public KeepassGroupDao(KeepassDatabase db) {
        this.db = db;
        this.removeListeners = new CopyOnWriteArrayList<>();
    }

    public void addOnGroupRemoveLister(OnGroupRemoveLister removeLister) {
        removeListeners.add(removeLister);
    }

    @NonNull
    @Override
    public OperationResult<Group> getRootGroup() {
        SimpleDatabase keepassDb = db.getKeepassDatabase();

        SimpleGroup rootGroup;
        synchronized (db.getLock()) {
            rootGroup = keepassDb.getRootGroup();
            if (rootGroup == null) {
                return OperationResult.error(newDbError(MESSAGE_FAILED_TO_FIND_ROOT_GROUP));
            }
        }

        return OperationResult.success(createGroupFromKeepassGroup(rootGroup));
    }

    @NonNull
    @Override
    public OperationResult<List<Group>> getChildGroups(UUID parentGroupUid) {
        List<Group> groups = new ArrayList<>();

        synchronized (db.getLock()) {
            SimpleGroup parentGroup = db.findGroupByUid(parentGroupUid);

            if (parentGroup == null) {
                return OperationResult.error(newDbError(MESSAGE_FAILED_TO_FIND_GROUP));
            }

            List<SimpleGroup> childGroups = parentGroup.getGroups();
            if (childGroups != null) {
                groups.addAll(createGroupsFromKeepassGroups(childGroups));
            }
        }

        return OperationResult.success(groups);
    }

    @NonNull
    @Override
    public OperationResult<List<Group>> getAll() {
        List<Group> groups = new ArrayList<>();

        synchronized (db.getLock()) {
            SimpleDatabase keepassDb = db.getKeepassDatabase();

            putAllGroupsIntoListRecursively(groups, keepassDb.getRootGroup());
        }

        return OperationResult.success(groups);
    }

    private void putAllGroupsIntoListRecursively(List<Group> groups, SimpleGroup group) {
        List<SimpleGroup> childGroups = group.getGroups();
        if (childGroups != null && childGroups.size() != 0) {

            for (int i = 0; i < childGroups.size(); i++) {
                SimpleGroup childGroup = childGroups.get(i);

                groups.add(createGroupFromKeepassGroup(childGroup));

                putAllGroupsIntoListRecursively(groups, childGroup);
            }
        }
    }

    private List<Group> createGroupsFromKeepassGroups(List<SimpleGroup> childGroups) {
        List<Group> groups = new ArrayList<>();

        for (int i = 0; i < childGroups.size(); i++) {
            SimpleGroup childGroup = childGroups.get(i);

            groups.add(createGroupFromKeepassGroup(childGroup));
        }

        return groups;
    }

    private Group createGroupFromKeepassGroup(SimpleGroup keepassGroup) {
        Group result = new Group();

        result.setUid(keepassGroup.getUuid());
        result.setTitle(keepassGroup.getName());

        return result;
    }

    @NonNull
    @Override
    public OperationResult<UUID> insert(Group group, UUID parentGroupUid) {
        SimpleDatabase keepassDb = db.getKeepassDatabase();

        SimpleGroup newGroup;
        synchronized (db.getLock()) {
            SimpleGroup parentGroup = db.findGroupByUid(parentGroupUid);
            if (parentGroup == null) {
                return OperationResult.error(newDbError(MESSAGE_FAILED_TO_FIND_GROUP));
            }

            newGroup = keepassDb.newGroup(group.getTitle());

            parentGroup.addGroup(newGroup);
            if (newGroup.getUuid() == null) {
                return OperationResult.error(newDbError(MESSAGE_UNKNOWN_ERROR));
            }

            OperationResult<Boolean> commitResult = db.commit();
            if (commitResult.isFailed()) {
                keepassDb.deleteGroup(newGroup.getUuid());
                return commitResult.takeError();
            }
        }

        return OperationResult.success(newGroup.getUuid());
    }

    @NonNull
    @Override
    public OperationResult<Boolean> remove(UUID groupUid) {
        synchronized (db.getLock()) {
            boolean deleted = db.getKeepassDatabase().deleteGroup(groupUid);
            if (!deleted) {
                return OperationResult.error(newDbError(MESSAGE_FAILED_TO_COMPLETE_OPERATION));
            }

            OperationResult<Boolean> commitResult = db.commit();
            if (commitResult.isFailed()) {
                return commitResult.takeError();
            }
        }

        notifyOnGroupRemoved(groupUid);

        return OperationResult.success(true);
    }

    private void notifyOnGroupRemoved(UUID groupUid) {
        for (OnGroupRemoveLister lister : removeListeners) {
            lister.onGroupRemoved(groupUid);
        }
    }
}
