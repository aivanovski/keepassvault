package com.ivanovsky.passnotes.data.repository.keepass.dao;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.annimon.stream.Stream;
import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_MOVE_GROUP_INSIDE_ITS_OWN_TREE;
import com.ivanovsky.passnotes.data.entity.OperationResult;
import com.ivanovsky.passnotes.data.repository.keepass.KeepassDatabase;
import com.ivanovsky.passnotes.data.repository.encdb.dao.GroupDao;
import com.ivanovsky.passnotes.data.entity.Group;

import java.util.LinkedList;

import org.linguafranca.pwdb.kdbx.simple.SimpleDatabase;
import org.linguafranca.pwdb.kdbx.simple.SimpleGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_COMPLETE_OPERATION;
import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_FIND_GROUP;
import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_FIND_NEW_PARENT_GROUP;
import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_FIND_PARENT_GROUP;
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
        return new Group(keepassGroup.getUuid(), keepassGroup.getName());
    }

    @NonNull
    @Override
    public OperationResult<UUID> insert(Group group, UUID parentGroupUid) {
        return insert(group, parentGroupUid, true);
    }

    @NonNull
    private OperationResult<UUID> insert(Group group, UUID parentGroupUid, boolean doCommit) {
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

            if (doCommit) {
                OperationResult<Boolean> commitResult = db.commit();
                if (commitResult.isFailed()) {
                    keepassDb.deleteGroup(newGroup.getUuid());
                    return commitResult.takeError();
                }
            }
        }

        return OperationResult.success(newGroup.getUuid());
    }

    @NonNull
    @Override
    public OperationResult<Boolean> remove(UUID groupUid) {
        return remove(groupUid, true);
    }

    @NonNull
    private OperationResult<Boolean> remove(UUID groupUid, boolean doCommit) {
        synchronized (db.getLock()) {
            boolean deleted = db.getKeepassDatabase().deleteGroup(groupUid);
            if (!deleted) {
                return OperationResult.error(newDbError(MESSAGE_FAILED_TO_COMPLETE_OPERATION));
            }

            if (doCommit) {
                OperationResult<Boolean> commitResult = db.commit();
                if (commitResult.isFailed()) {
                    return commitResult.takeError();
                }
            }
        }

        notifyOnGroupRemoved(groupUid);

        return OperationResult.success(true);
    }

    @NonNull
    @Override
    public OperationResult<Group> getGroupByUid(@NonNull UUID groupUid) {
        SimpleGroup group;

        synchronized (db.getLock()) {
            group = db.findGroupByUid(groupUid);
            if (group == null) {
                return OperationResult.error(newDbError(MESSAGE_FAILED_TO_FIND_GROUP));
            }
        }

        return OperationResult.success(createGroupFromKeepassGroup(group));
    }

    @NonNull
    @Override
    public OperationResult<Boolean> update(@NonNull Group group, @Nullable UUID newParentGroupUid) {
        UUID groupUid = group.getUid();

        synchronized (db.getLock()) {
            if (newParentGroupUid == null) {
                SimpleGroup dbGroup = db.findGroupByUid(group.getUid());
                if (dbGroup == null) {
                    return OperationResult.error(newDbError(MESSAGE_FAILED_TO_FIND_GROUP));
                }

                dbGroup.setName(group.getTitle());
            } else {
                OperationResult<Boolean> isInsideItselfResult = isGroupInsideGroupTree(newParentGroupUid, groupUid);
                if (isInsideItselfResult.isFailed()) {
                    return isInsideItselfResult.takeError();
                }
                if (isInsideItselfResult.getObj()) {
                    return OperationResult.error(newDbError(MESSAGE_FAILED_TO_MOVE_GROUP_INSIDE_ITS_OWN_TREE));
                }

                SimpleGroup rootGroup = db.getKeepassDatabase().getRootGroup();
                if (rootGroup == null) {
                    return OperationResult.error(newDbError(MESSAGE_FAILED_TO_FIND_ROOT_GROUP));
                }

                SimpleGroup oldParentGroup = null;
                SimpleGroup newParentGroup = null;
                SimpleGroup dbGroup = null;

                LinkedList<SimpleGroup> nextGroups = new LinkedList<>();
                nextGroups.add(rootGroup);

                if (rootGroup.getUuid().equals(groupUid)) {
                    oldParentGroup = rootGroup;
                }
                if (rootGroup.getUuid().equals(newParentGroupUid)) {
                    newParentGroup = rootGroup;
                }

                SimpleGroup currentGroup;
                while ((currentGroup = nextGroups.pollFirst()) != null) {
                    List<SimpleGroup> innerGroups = currentGroup.getGroups();

                    if (oldParentGroup == null
                            && containsGroupWithUid(innerGroups, groupUid)) {
                        oldParentGroup = currentGroup;
                    }

                    if (newParentGroup == null
                            && currentGroup.getUuid().equals(newParentGroupUid)) {
                        newParentGroup = currentGroup;
                    }

                    if (dbGroup == null
                            && currentGroup.getUuid().equals(groupUid)) {
                        dbGroup = currentGroup;
                    }

                    if (oldParentGroup != null
                            && newParentGroup != null
                            && dbGroup != null) {
                        break;
                    }

                    nextGroups.addAll(innerGroups);
                }

                if (dbGroup == null) {
                    return OperationResult.error(newDbError(MESSAGE_FAILED_TO_FIND_GROUP));
                } else if (oldParentGroup == null) {
                    return OperationResult.error(newDbError(MESSAGE_FAILED_TO_FIND_PARENT_GROUP));
                } else if (newParentGroup == null) {
                    return OperationResult.error(newDbError(MESSAGE_FAILED_TO_FIND_NEW_PARENT_GROUP));
                }

                dbGroup.setName(group.getTitle());

                oldParentGroup.getGroups().remove(oldParentGroup);
                newParentGroup.addGroup(dbGroup);
            }

            OperationResult<Boolean> commitResult = db.commit();
            if (commitResult.isFailed()) {
                return commitResult.takeError();
            }
        }

        return OperationResult.success(true);
    }

    private boolean containsGroupWithUid(List<SimpleGroup> groups, UUID groupUid) {
        for (SimpleGroup group : groups) {
            if (group.getUuid().equals(groupUid)) {
                return true;
            }
        }

        return false;
    }

    private void notifyOnGroupRemoved(UUID groupUid) {
        for (OnGroupRemoveLister lister : removeListeners) {
            lister.onGroupRemoved(groupUid);
        }
    }

    private OperationResult<Boolean> isGroupInsideGroupTree(UUID groupUid, UUID groupTreeRootUid) {
        SimpleGroup treeRoot = db.findGroupByUid(groupTreeRootUid);
        if (treeRoot == null) {
            return OperationResult.error(newDbError(MESSAGE_FAILED_TO_FIND_GROUP));
        }

        List<SimpleGroup> groupsInsideTree = db.getAllGroupsFromTree(treeRoot);
        boolean isGroupInsideTree = Stream.of(groupsInsideTree)
                .anyMatch(group -> group.getUuid().equals(groupUid));

        return OperationResult.success(isGroupInsideTree);
    }
}
