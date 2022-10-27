package com.ivanovsky.passnotes.data.repository.keepass.keepass_java;

import androidx.annotation.NonNull;

import com.annimon.stream.Stream;
import com.ivanovsky.passnotes.data.entity.GroupEntity;
import com.ivanovsky.passnotes.data.entity.InheritableBooleanOption;
import com.ivanovsky.passnotes.data.entity.OperationResult;
import com.ivanovsky.passnotes.data.repository.encdb.ContentWatcher;
import com.ivanovsky.passnotes.data.repository.encdb.dao.GroupDao;
import com.ivanovsky.passnotes.data.entity.Group;
import com.ivanovsky.passnotes.extensions.GroupExtKt;

import java.util.LinkedList;

import org.linguafranca.pwdb.kdbx.simple.SimpleDatabase;
import org.linguafranca.pwdb.kdbx.simple.SimpleGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_COMPLETE_OPERATION;
import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_FIND_GROUP;
import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_FIND_NEW_PARENT_GROUP;
import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_FIND_PARENT_GROUP;
import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_FIND_ROOT_GROUP;
import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_MOVE_GROUP_INSIDE_ITS_OWN_TREE;
import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_PARENT_UID_IS_NULL;
import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_UID_IS_NULL;
import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_UNKNOWN_ERROR;
import static com.ivanovsky.passnotes.data.entity.OperationError.newDbError;

public class KeepassJavaGroupDao implements GroupDao {

    private final KeepassJavaDatabase db;
    private final ContentWatcher<Group> contentWatcher;

    public KeepassJavaGroupDao(KeepassJavaDatabase db) {
        this.db = db;
        this.contentWatcher = new ContentWatcher<>();
    }

    @NonNull
    @Override
    public ContentWatcher<Group> getContentWatcher() {
        return contentWatcher;
    }

    @NonNull
    @Override
    public OperationResult<Group> getRootGroup() {
        SimpleDatabase keepassDb = db.getKeepassDatabase();

        SimpleGroup rootGroup;
        db.getLock().lock();
        try {
            rootGroup = keepassDb.getRootGroup();
            if (rootGroup == null) {
                return OperationResult.error(newDbError(MESSAGE_FAILED_TO_FIND_ROOT_GROUP));
            }
        } finally {
                db.getLock().unlock();
        }

        return OperationResult.success(createGroupFromKeepassGroup(rootGroup));
    }

    @NonNull
    @Override
    public OperationResult<List<Group>> getChildGroups(UUID parentGroupUid) {
        List<Group> groups = new ArrayList<>();

        db.getLock().lock();
        try {
            SimpleGroup parentGroup = db.findGroupByUid(parentGroupUid);

            if (parentGroup == null) {
                return OperationResult.error(newDbError(MESSAGE_FAILED_TO_FIND_GROUP));
            }

            List<SimpleGroup> childGroups = parentGroup.getGroups();
            if (childGroups != null) {
                groups.addAll(createGroupsFromKeepassGroups(childGroups));
            }
        } finally {
            db.getLock().unlock();
        }

        return OperationResult.success(groups);
    }

    @NonNull
    @Override
    public OperationResult<List<Group>> getAll() {
        List<Group> groups = new ArrayList<>();

        db.getLock().lock();
        try {
            SimpleDatabase keepassDb = db.getKeepassDatabase();

            putAllGroupsIntoListRecursively(groups, keepassDb.getRootGroup());
        } finally {
            db.getLock().unlock();
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
        UUID parentGroupUid = null;
        if (keepassGroup.getParent() != null) {
            parentGroupUid = keepassGroup.getParent().getUuid();
        }


        return new Group(keepassGroup.getUuid(),
                parentGroupUid,
                keepassGroup.getName(),
                keepassGroup.getGroupsCount(),
                keepassGroup.getEntriesCount(),
                // Property 'enableAutotype' is not accessible, let it be always true
                new InheritableBooleanOption(true, false)
        );
    }

    @NonNull
    @Override
    public OperationResult<UUID> insert(GroupEntity group) {
        return insert(group, true);
    }

    @NonNull
    @Override
    public OperationResult<UUID> insert(@NonNull GroupEntity group, boolean doCommit) {
        SimpleDatabase keepassDb = db.getKeepassDatabase();

        SimpleGroup newGroup;
        db.getLock().lock();
        try {
            if (group.getParentUid() == null) {
                return OperationResult.error(newDbError(MESSAGE_PARENT_UID_IS_NULL));
            }

            SimpleGroup parentGroup = db.findGroupByUid(group.getParentUid());
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
        } finally {
            db.getLock().unlock();
        }

        contentWatcher.notifyEntryInserted(createGroupFromKeepassGroup(newGroup));

        return OperationResult.success(newGroup.getUuid());
    }

    @NonNull
    @Override
    public OperationResult<Boolean> remove(UUID groupUid) {
        return remove(groupUid, true);
    }

    @NonNull
    private OperationResult<Boolean> remove(UUID groupUid, boolean doCommit) {
        db.getLock().lock();
        SimpleGroup group;
        try {
            group = db.findGroupByUid(groupUid);
            if (group == null) {
                return OperationResult.error(newDbError(MESSAGE_FAILED_TO_FIND_GROUP));
            }

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
        } finally {
            db.getLock().unlock();
        }

        contentWatcher.notifyEntryRemoved(createGroupFromKeepassGroup(group));

        return OperationResult.success(true);
    }

    @NonNull
    @Override
    public OperationResult<Group> getGroupByUid(@NonNull UUID groupUid) {
        SimpleGroup group;

        db.getLock().lock();
        try {
            group = db.findGroupByUid(groupUid);
            if (group == null) {
                return OperationResult.error(newDbError(MESSAGE_FAILED_TO_FIND_GROUP));
            }
        } finally {
            db.getLock().unlock();
        }

        return OperationResult.success(createGroupFromKeepassGroup(group));
    }

    @NonNull
    @Override
    public OperationResult<Boolean> update(@NonNull GroupEntity group) {
        UUID groupUid = group.getUid();
        Group oldGroup;
        Group newGroup;

        db.getLock().lock();
        try {
            if (group.getParentUid() == null) {
                if (group.getUid() == null) {
                    return OperationResult.error(newDbError(MESSAGE_UID_IS_NULL));
                }

                SimpleGroup dbGroup = db.findGroupByUid(group.getUid());
                if (dbGroup == null) {
                    return OperationResult.error(newDbError(MESSAGE_FAILED_TO_FIND_GROUP));
                }

                oldGroup = createGroupFromKeepassGroup(dbGroup);
                dbGroup.setName(group.getTitle());
                newGroup = createGroupFromKeepassGroup(dbGroup);
            } else {
                OperationResult<Boolean> isInsideItselfResult = isGroupInsideGroupTree(group.getParentUid(), groupUid);
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

                LinkedList<SimpleGroup> nextGroups = new LinkedList<>();
                nextGroups.add(rootGroup);

                if (rootGroup.getUuid().equals(groupUid)) {
                    oldParentGroup = rootGroup;
                }
                if (rootGroup.getUuid().equals(group.getParentUid())) {
                    newParentGroup = rootGroup;
                }

                SimpleGroup currentGroup;
                SimpleGroup dbGroup = null;
                while ((currentGroup = nextGroups.pollFirst()) != null) {
                    List<SimpleGroup> innerGroups = currentGroup.getGroups();

                    if (oldParentGroup == null
                            && containsGroupWithUid(innerGroups, groupUid)) {
                        oldParentGroup = currentGroup;
                    }

                    if (newParentGroup == null
                            && currentGroup.getUuid().equals(group.getParentUid())) {
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

                oldGroup = createGroupFromKeepassGroup(dbGroup);
                dbGroup.setName(group.getTitle());

                oldParentGroup.getGroups().remove(oldParentGroup);
                newParentGroup.addGroup(dbGroup);
                newGroup = createGroupFromKeepassGroup(dbGroup);
            }

            OperationResult<Boolean> commitResult = db.commit();
            if (commitResult.isFailed()) {
                return commitResult.takeError();
            }
        } finally {
            db.getLock().unlock();
        }

        contentWatcher.notifyEntryChanged(oldGroup, newGroup);

        return OperationResult.success(true);
    }

    @NonNull
    @Override
    public OperationResult<List<Group>> find(@NonNull String query) {
        OperationResult<List<Group>> allGroupsResult = getAll();
        if (allGroupsResult.isFailed()) {
            return allGroupsResult.takeError();
        }

        List<Group> allGroups = allGroupsResult.getObj();

        List<Group> matchedGroups = new ArrayList<>();
        for (Group group : allGroups) {
            if (GroupExtKt.matches(group, query)) {
                matchedGroups.add(group);
            }
        }

        return OperationResult.success(matchedGroups);
    }

    private boolean containsGroupWithUid(List<SimpleGroup> groups, UUID groupUid) {
        for (SimpleGroup group : groups) {
            if (group.getUuid().equals(groupUid)) {
                return true;
            }
        }

        return false;
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
