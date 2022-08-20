package com.ivanovsky.passnotes.data.repository.keepass.kotpass

import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.GroupEntity
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_MOVE_GROUP_INSIDE_ITS_OWN_TREE
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_PARENT_UID_IS_NULL
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_UID_IS_NULL
import com.ivanovsky.passnotes.data.entity.OperationError.newDbError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.encdb.dao.GroupDao
import com.ivanovsky.passnotes.data.repository.encdb.ContentWatcher
import com.ivanovsky.passnotes.extensions.map
import com.ivanovsky.passnotes.extensions.mapError
import com.ivanovsky.passnotes.extensions.mapWithObject
import io.github.anvell.kotpass.database.modifiers.modifyGroup
import io.github.anvell.kotpass.database.modifiers.moveGroup
import io.github.anvell.kotpass.database.modifiers.removeGroup
import java.util.UUID
import kotlin.concurrent.withLock
import io.github.anvell.kotpass.models.Group as RawGroup

class KotpassGroupDao(
    private val db: KotpassDatabase
) : GroupDao {

    private val watcher = ContentWatcher<Group>()

    override fun getContentWatcher(): ContentWatcher<Group> = watcher

    override fun getAll(): OperationResult<List<Group>> {
        return db.lock.withLock {
            val root = db.getRawRootGroup()
            val getGroupsResult = db.getRawChildGroups(root)
            if (getGroupsResult.isFailed) {
                return@withLock getGroupsResult.mapError()
            }

            getGroupsResult.map { rootChildren ->
                val allGroups = mutableListOf<RawGroup>()
                    .apply {
                        add(root)
                        addAll(rootChildren)
                    }
                    .map { it.convertToGroup() }

                allGroups
            }
        }
    }

    override fun getRootGroup(): OperationResult<Group> {
        val rootGroup = db.getRawDatabase().content.group.convertToGroup()
        return OperationResult.success(rootGroup)
    }

    override fun getChildGroups(parentGroupUid: UUID): OperationResult<List<Group>> {
        return db.lock.withLock {
            val getGroupResult = db.getRawGroupByUid(parentGroupUid)
            if (getGroupResult.isFailed) {
                return@withLock getGroupResult.mapError()
            }

            val group = getGroupResult.obj

            OperationResult.success(group.groups.convertToGroups())
        }
    }

    override fun insert(group: GroupEntity): OperationResult<UUID> {
        return insert(group, doCommit = true)
    }

    override fun insert(group: GroupEntity, doCommit: Boolean): OperationResult<UUID> {
        val result = db.lock.withLock {
            if (group.parentUid == null) {
                return@withLock OperationResult.error(newDbError(MESSAGE_PARENT_UID_IS_NULL))
            }

            val getParentGroupResult = db.getRawGroupByUid(group.parentUid)
            if (getParentGroupResult.isFailed) {
                return@withLock getParentGroupResult.mapError()
            }

            val parentRawGroup = getParentGroupResult.obj
            val newRawGroup = RawGroup(
                uuid = group.uid ?: UUID.randomUUID(),
                name = group.title,
                previousParentGroup = group.parentUid
            )

            val newRawDatabase = db.getRawDatabase().modifyGroup(group.parentUid) {
                copy(
                    groups = parentRawGroup.groups.toMutableList()
                        .apply {
                            add(newRawGroup)
                        }
                )
            }

            val newGroup = Group(
                uid = newRawGroup.uuid,
                parentUid = parentRawGroup.uuid,
                title = group.title,
                groupCount = 0,
                noteCount = 0
            )

            db.swapDatabase(newRawDatabase)

            if (doCommit) {
                db.commit().mapWithObject(newGroup)
            } else {
                OperationResult.success(newGroup)
            }
        }

        if (result.isSucceededOrDeferred) {
            contentWatcher.notifyEntryInserted(result.obj)
        }

        return result.map { it.uid }
    }

    override fun remove(groupUid: UUID): OperationResult<Boolean> {
        val result = db.lock.withLock {
            val getGroupResult = getGroupByUid(groupUid)
            if (getGroupResult.isFailed) {
                return@withLock getGroupResult.mapError()
            }

            val group = getGroupResult.obj

            val newRawDatabase = db.getRawDatabase().removeGroup(groupUid)
            db.swapDatabase(newRawDatabase)

            db.commit().mapWithObject(group)
        }

        if (result.isSucceededOrDeferred) {
            contentWatcher.notifyEntryRemoved(result.obj)
        }

        return result.mapWithObject(true)
    }

    override fun getGroupByUid(groupUid: UUID): OperationResult<Group> {
        return db.lock.withLock {
            val getGroupResult = db.getRawGroupByUid(groupUid)
            if (getGroupResult.isFailed) {
                return@withLock getGroupResult.mapError()
            }

            val group = getGroupResult.obj
            OperationResult.success(group.convertToGroup())
        }
    }

    override fun update(group: GroupEntity): OperationResult<Boolean> {
        if (group.uid == null) {
            return OperationResult.error(newDbError(MESSAGE_UID_IS_NULL))
        }

        val getGroupResult = db.lock.withLock { getGroupByUid(group.uid) }
        if (getGroupResult.isFailed) {
            return getGroupResult.mapError()
        }

        val oldGroup = getGroupResult.obj
        val newGroup = oldGroup.copy(
            title = group.title,
            parentUid = group.parentUid
        )

        val result = db.lock.withLock {
            if (group.parentUid == null) {
                val newDb = db.getRawDatabase().modifyGroup(group.uid) {
                    copy(
                        name = group.title
                    )
                }

                db.swapDatabase(newDb)

                return@withLock db.commit().mapWithObject(newGroup)
            }

            val isInsideItself = isGroupInsideGroupTree(group.parentUid, group.uid)
            if (isInsideItself.isFailed) {
                return@withLock isInsideItself.mapError()
            }
            if (isInsideItself.obj) {
                return@withLock OperationResult.error(
                    newDbError(
                        MESSAGE_FAILED_TO_MOVE_GROUP_INSIDE_ITS_OWN_TREE
                    )
                )
            }

            val getOldGroupResult = db.getRawGroupByUid(group.uid)
            if (getOldGroupResult.isFailed) {
                return@withLock getOldGroupResult.mapError()
            }

            val getOldParentResult = db.getRawParentGroup(group.uid)
            if (getOldParentResult.isFailed) {
                return@withLock getOldParentResult.mapError()
            }

            val getNewParentResult = db.getRawGroupByUid(group.parentUid)
            if (getNewParentResult.isFailed) {
                return@withLock getNewParentResult.mapError()
            }

            val oldRawParent = getOldParentResult.obj
            val newRawParent = getNewParentResult.obj

            val newDb = if (oldRawParent.uuid != newRawParent.uuid) {
                db.getRawDatabase()
                    .moveGroup(group.uid, group.parentUid)
                    .modifyGroup(group.uid) {
                        copy(
                            name = group.title
                        )
                    }
            } else {
                db.getRawDatabase()
                    .modifyGroup(group.uid) {
                        copy(
                            name = group.title
                        )
                    }
            }

            db.swapDatabase(newDb)

            db.commit().mapWithObject(newGroup)
        }

        if (result.isSucceededOrDeferred) {
            contentWatcher.notifyEntryChanged(oldGroup, newGroup)
        }

        return result.mapWithObject(true)
    }

    override fun find(query: String): OperationResult<List<Group>> {
        return db.lock.withLock {
            val allGroupsResult = all
            if (allGroupsResult.isFailed) {
                return@withLock allGroupsResult.mapError()
            }

            val allGroups = allGroupsResult.obj

            val matchedGroups = allGroups
                .filter { group -> group.title.contains(query, ignoreCase = true) }

            OperationResult.success(matchedGroups)
        }
    }

    private fun isGroupInsideGroupTree(
        groupUid: UUID,
        groupTreeRootUid: UUID
    ): OperationResult<Boolean> {
        val getTreeRootResult = db.getRawGroupByUid(groupTreeRootUid)
        if (getTreeRootResult.isFailed) {
            return getTreeRootResult.mapError()
        }

        val rawTreeRoot = getTreeRootResult.obj
        val getTreeResult = db.getRawChildGroups(rawTreeRoot)
        if (getTreeResult.isFailed) {
            return getTreeResult.mapError()
        }

        val tree = getTreeResult.obj
        val isGroupInsideTree = tree.any { it.uuid == groupUid }

        return OperationResult.success(isGroupInsideTree)
    }
}