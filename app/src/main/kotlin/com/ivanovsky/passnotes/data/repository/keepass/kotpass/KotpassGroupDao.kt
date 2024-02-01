package com.ivanovsky.passnotes.data.repository.keepass.kotpass

import app.keemobile.kotpass.constants.GroupOverride
import app.keemobile.kotpass.database.modifiers.modifyGroup
import app.keemobile.kotpass.database.modifiers.moveGroup
import app.keemobile.kotpass.database.modifiers.removeGroup
import app.keemobile.kotpass.models.Group as RawGroup
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.GroupEntity
import com.ivanovsky.passnotes.data.entity.InheritableBooleanOption
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_MOVE_GROUP_INSIDE_ITS_OWN_TREE
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_PARENT_UID_IS_NULL
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_UID_IS_NULL
import com.ivanovsky.passnotes.data.entity.OperationError.newDbError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.encdb.ContentWatcher
import com.ivanovsky.passnotes.data.repository.encdb.dao.GroupDao
import com.ivanovsky.passnotes.extensions.getOrThrow
import com.ivanovsky.passnotes.extensions.map
import com.ivanovsky.passnotes.extensions.mapError
import com.ivanovsky.passnotes.extensions.mapWithObject
import com.ivanovsky.passnotes.extensions.matches
import com.ivanovsky.passnotes.extensions.toEntity
import java.util.UUID
import kotlin.concurrent.withLock

class KotpassGroupDao(
    private val db: KotpassDatabase
) : GroupDao {

    private val watcher = ContentWatcher<Group>()

    override fun getContentWatcher(): ContentWatcher<Group> = watcher

    override fun getAll(): OperationResult<List<Group>> {
        return db.lock.withLock {
            val rootUid = db.getRawRootGroup().uuid

            val allGroups = db.getAllRawGroups()
                .map { group ->
                    val getOptionsResult = db.getInheritableOptions(group.uuid)
                    if (getOptionsResult.isFailed) {
                        return@withLock getOptionsResult.mapError()
                    }

                    val parent = if (group.uuid != rootUid) {
                        val getParentResult = db.getRawParentGroup(group.uuid)
                        if (getParentResult.isFailed) {
                            return@withLock getParentResult.mapError()
                        }

                        getParentResult.obj
                    } else {
                        null
                    }

                    group.convertToGroup(
                        parentGroupUid = parent?.uuid,
                        options = getOptionsResult.obj
                    )
                }

            OperationResult.success(allGroups)
        }
    }

    override fun getRootGroup(): OperationResult<Group> {
        val root = db.getRawRootGroup()
        val result = root.convertToGroup(
            parentGroupUid = null,
            options = db.getRawRootGroupOptions()
        )
        return OperationResult.success(result)
    }

    override fun getChildGroups(parentGroupUid: UUID): OperationResult<List<Group>> {
        return db.lock.withLock {
            val getGroupResult = db.getRawGroupByUid(parentGroupUid)
            if (getGroupResult.isFailed) {
                return@withLock getGroupResult.mapError()
            }

            val group = getGroupResult.obj
            val groups = group.groups
                .map { child ->
                    val getOptionsResult = db.getInheritableOptions(child.uuid)
                    if (getOptionsResult.isFailed) {
                        return@withLock getOptionsResult.mapError()
                    }

                    child.convertToGroup(
                        parentGroupUid = group.uuid,
                        options = getOptionsResult.obj
                    )
                }

            OperationResult.success(groups)
        }
    }

    override fun insert(group: GroupEntity): OperationResult<UUID> {
        return insert(group, doCommit = true)
    }

    override fun insert(entity: GroupEntity, doCommit: Boolean): OperationResult<UUID> {
        val result = db.lock.withLock {
            if (entity.parentUid == null) {
                return@withLock OperationResult.error(newDbError(MESSAGE_PARENT_UID_IS_NULL))
            }

            val getParentGroupResult = db.getRawGroupByUid(entity.parentUid)
            if (getParentGroupResult.isFailed) {
                return@withLock getParentGroupResult.mapError()
            }

            val parentRawGroup = getParentGroupResult.obj
            val uid = entity.uid ?: UUID.randomUUID()
            val newRawGroup = RawGroup(
                uuid = uid,
                name = entity.title,
                previousParentGroup = entity.parentUid,
                enableAutoType = entity.autotypeEnabled.toRawOption(),
                enableSearching = entity.searchEnabled.toRawOption()
            )

            val newRawDatabase = db.getRawDatabase().modifyGroup(entity.parentUid) {
                copy(
                    groups = parentRawGroup.groups.toMutableList()
                        .apply {
                            add(newRawGroup)
                        }
                )
            }

            db.swapDatabase(newRawDatabase)

            val getGroupResult = db.getRawGroupByUid(uid)
            if (getGroupResult.isFailed) {
                return@withLock getGroupResult.mapError()
            }

            val getOptionsResult = db.getInheritableOptions(uid)
            if (getOptionsResult.isFailed) {
                return@withLock getOptionsResult.mapError()
            }

            val newGroup = getGroupResult.obj.convertToGroup(
                parentGroupUid = parentRawGroup.uuid,
                options = getOptionsResult.obj
            )

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

            val getRecycleBinResult = db.getRecycleBinGroup()
            if (getRecycleBinResult.isFailed) {
                return getRecycleBinResult.mapError()
            }

            val group = getGroupResult.getOrThrow()
            val recycleBinGroup = getRecycleBinResult.getOrThrow()

            val isInsideRecycleBin = if (recycleBinGroup != null) {
                val isInsideRecycleBinResult = isGroupInsideGroupTree(
                    groupUid = groupUid,
                    groupTreeRootUid = recycleBinGroup.uuid
                )

                if (isInsideRecycleBinResult.isFailed) {
                    return@withLock isInsideRecycleBinResult.mapError()
                }

                isInsideRecycleBinResult.getOrThrow()
            } else {
                false
            }

            when {
                recycleBinGroup != null && !isInsideRecycleBin -> {
                    // move to recycle bin
                    val newGroup = group.copy(parentUid = recycleBinGroup.uuid)
                    val updateResult = update(newGroup.toEntity(), doCommit = false)
                    if (updateResult.isFailed) {
                        return@withLock updateResult.mapError()
                    }
                }

                else -> {
                    // remove permanently
                    val newDb = db.getRawDatabase().removeGroup(groupUid)
                    db.swapDatabase(newDb)
                }
            }

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

            val getOptionsResult = db.getInheritableOptions(groupUid)
            if (getOptionsResult.isFailed) {
                return@withLock getOptionsResult.mapError()
            }

            val group = getGroupResult.obj
            val options = getOptionsResult.obj

            val rootUid = db.getRawRootGroup().uuid

            val parent = if (group.uuid != rootUid) {
                val getParentResult = db.getRawParentGroup(group.uuid)
                if (getParentResult.isFailed) {
                    return@withLock getParentResult.mapError()
                }

                getParentResult.obj
            } else {
                null
            }

            OperationResult.success(
                group.convertToGroup(
                    parentGroupUid = parent?.uuid,
                    options = options
                )
            )
        }
    }

    override fun update(entity: GroupEntity, doCommit: Boolean): OperationResult<Boolean> {
        if (entity.uid == null) {
            return OperationResult.error(newDbError(MESSAGE_UID_IS_NULL))
        }

        val getGroupResult = db.lock.withLock { getGroupByUid(entity.uid) }
        if (getGroupResult.isFailed) {
            return getGroupResult.mapError()
        }

        val oldGroup = getGroupResult.obj
        val newGroup = oldGroup.copy(
            title = entity.title,
            parentUid = entity.parentUid,
            autotypeEnabled = entity.autotypeEnabled
        )

        val result = db.lock.withLock {
            if (entity.parentUid == null) {
                val newDb = db.getRawDatabase().modifyGroup(entity.uid) {
                    copy(
                        name = entity.title,
                        enableAutoType = entity.autotypeEnabled.toRawOption(),
                        enableSearching = entity.searchEnabled.toRawOption()
                    )
                }

                db.swapDatabase(newDb)

                return@withLock db.commit().mapWithObject(newGroup)
            }

            val isInsideItself = isGroupInsideGroupTree(entity.parentUid, entity.uid)
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

            val getOldGroupResult = db.getRawGroupByUid(entity.uid)
            if (getOldGroupResult.isFailed) {
                return@withLock getOldGroupResult.mapError()
            }

            val getOldParentResult = db.getRawParentGroup(entity.uid)
            if (getOldParentResult.isFailed) {
                return@withLock getOldParentResult.mapError()
            }

            val getNewParentResult = db.getRawGroupByUid(entity.parentUid)
            if (getNewParentResult.isFailed) {
                return@withLock getNewParentResult.mapError()
            }

            val oldRawParent = getOldParentResult.obj
            val newRawParent = getNewParentResult.obj

            val newDb = if (oldRawParent.uuid != newRawParent.uuid) {
                db.getRawDatabase()
                    .moveGroup(entity.uid, entity.parentUid)
                    .modifyGroup(entity.uid) {
                        copy(
                            name = entity.title,
                            enableAutoType = entity.autotypeEnabled.toRawOption(),
                            enableSearching = entity.searchEnabled.toRawOption()
                        )
                    }
            } else {
                db.getRawDatabase()
                    .modifyGroup(entity.uid) {
                        copy(
                            name = entity.title,
                            enableAutoType = entity.autotypeEnabled.toRawOption(),
                            enableSearching = entity.searchEnabled.toRawOption()
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
            val getRootResult = rootGroup
            if (getRootResult.isFailed) {
                return@withLock getRootResult.mapError()
            }

            val allGroupsResult = all
            if (allGroupsResult.isFailed) {
                return@withLock allGroupsResult.mapError()
            }

            val allGroups = allGroupsResult.obj
            val root = getRootResult.obj

            val matchedGroups = allGroups
                .filter { group ->
                    group.uid != root.uid && group.matches(query)
                }

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
        val tree = db.getRawChildGroups(rawTreeRoot)

        val isGroupInsideTree = tree.any { it.uuid == groupUid }

        return OperationResult.success(isGroupInsideTree)
    }


    private fun InheritableBooleanOption.toRawOption(): GroupOverride {
        return when {
            isInheritValue -> GroupOverride.Inherit
            isEnabled -> GroupOverride.Enabled
            else -> GroupOverride.Disabled
        }
    }
}