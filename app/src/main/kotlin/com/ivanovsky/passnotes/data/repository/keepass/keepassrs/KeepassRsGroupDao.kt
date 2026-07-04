package com.ivanovsky.passnotes.data.repository.keepass.keepassrs

import arrow.core.raise.either
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.GroupEntity
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.OperationError.newDbError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.encdb.ContentWatcher
import com.ivanovsky.passnotes.data.repository.encdb.dao.GroupDao
import com.ivanovsky.passnotes.domain.entity.exception.Stacktrace
import com.ivanovsky.passnotes.extensions.getOrNull
import com.ivanovsky.passnotes.extensions.mapError
import com.ivanovsky.passnotes.extensions.matches
import com.ivanovsky.passnotes.util.toOperationResult
import java.util.UUID
import kotlin.concurrent.withLock

class KeepassRsGroupDao(
    private val db: KeepassRsDatabase
) : GroupDao {

    private val watcher = ContentWatcher<Group>()

    override fun getAll(): OperationResult<List<Group>> =
        db.lock.withLock {
            either {
                val allGroups = db.getAllGroups()

                val groups = mutableListOf<Group>()
                for (rawGroup in allGroups) {
                    val groupUid = rawGroup.uuid.toUuid().bind()
                    val options = db.getInheritableOptions(groupUid).bind()
                    val parentUid = db.getParentGroupUid(groupUid).bind()

                    val group = rawGroup.convertToGroup(
                        parentUid = parentUid.getOrNull(),
                        options = options
                    ).bind()

                    groups.add(group)
                }

                groups
            }.toOperationResult()
        }

    override fun getRootGroup(): OperationResult<Group> =
        db.lock.withLock {
            either {
                val root = db.getRawDatabase().rootGroup

                val options = db.getInheritableOptions(
                    groupUid = root.uuid.toUuid().bind()
                ).bind()

                root.convertToGroup(
                    parentUid = null,
                    options = options
                ).bind()
            }.toOperationResult()
        }

    override fun getChildGroups(parentGroupUid: UUID): OperationResult<List<Group>> =
        db.lock.withLock {
            either {
                val parentGroup = db.getRawGroupByUid(parentGroupUid).bind()

                val groups = mutableListOf<Group>()
                for (childGroup in parentGroup.groupsList) {
                    val childGroupUid = childGroup.uuid.toUuid().bind()
                    val options = db.getInheritableOptions(childGroupUid).bind()

                    val group = childGroup.convertToGroup(
                        parentUid = parentGroupUid,
                        options = options
                    ).bind()

                    groups.add(group)
                }

                groups
            }.toOperationResult()
        }

    override fun insert(group: GroupEntity): OperationResult<UUID> =
        insert(group, doCommit = true)

    override fun insert(group: GroupEntity, doCommit: Boolean): OperationResult<UUID> {
        val uid = group.uid ?: UUID.randomUUID()
        val parentUid = group.parentUid ?: return OperationResult.error(
            newDbError(OperationError.MESSAGE_PARENT_UID_IS_NULL, Stacktrace())
        )

        val result = db.lock.withLock {
            either {
                db.getRawGroupByUid(parentUid).bind()

                val rawGroup = group.copy(uid = uid).toProtoGroup(uid)
                db.swapDatabase { db ->
                    db.toBuilder()
                        .setRootGroup(
                            db.rootGroup.updateGroup(parentUid) { parent ->
                                parent.toBuilder()
                                    .addGroups(rawGroup)
                                    .build()
                            }
                        )
                        .build()
                }

                if (doCommit) {
                    db.commit()
                        .map { uid }
                        .bind()
                } else {
                    uid
                }
            }.toOperationResult()
        }

        if (doCommit && result.isSucceededOrDeferred) {
            getGroupByUid(uid).getOrNull()?.let { newGroup ->
                watcher.notifyEntryInserted(newGroup)
            }
        }

        return result
    }

    override fun remove(groupUid: UUID): OperationResult<Boolean> {
        val groupResult = getGroupByUid(groupUid)
        if (groupResult.isFailed) {
            return groupResult.mapError()
        }

        db.lock.withLock {
            db.swapDatabase { db ->
                db.toBuilder()
                    .setRootGroup(db.rootGroup.removeGroup(groupUid))
                    .build()
            }
        }

        val result = db.commit().toOperationResult()
        if (result.isSucceededOrDeferred) {
            watcher.notifyEntryRemoved(groupResult.obj)
        }

        return result
    }

    override fun getGroupByUid(groupUid: UUID): OperationResult<Group> =
        db.lock.withLock {
            either {
                val rawGroup = db.getRawGroupByUid(groupUid).bind()
                val parentUid = db.getParentGroupUid(groupUid).bind()

                rawGroup.convertToGroup(
                    parentUid = parentUid.getOrNull(),
                    options = db.getInheritableOptions(groupUid).bind()
                ).bind()
            }.toOperationResult()
        }

    override fun update(group: GroupEntity, doCommit: Boolean): OperationResult<Boolean> {
        val uid = group.uid ?: return OperationResult.error(
            newDbError(OperationError.MESSAGE_UID_IS_NULL, Stacktrace())
        )
        val oldGroupResult = getGroupByUid(uid)
        if (oldGroupResult.isFailed) {
            return oldGroupResult.mapError()
        }

        db.lock.withLock {
            db.swapDatabase { db ->
                db.toBuilder()
                    .setRootGroup(
                        db.rootGroup.updateGroup(uid) { oldGroup ->
                            group.toProtoGroup(uid).toBuilder()
                                .addAllGroups(oldGroup.groupsList)
                                .addAllEntries(oldGroup.entriesList)
                                .build()
                        }
                    )
                    .build()
            }
        }

        val result =
            if (doCommit) db.commit().toOperationResult() else OperationResult.success(true)
        if (result.isSucceededOrDeferred) {
            val newGroupResult = getGroupByUid(uid)
            if (newGroupResult.isSucceeded) {
                watcher.notifyEntryChanged(oldGroupResult.obj, newGroupResult.obj)
            }
        }

        return result
    }

    override fun find(query: String): OperationResult<List<Group>> {
        return db.lock.withLock {
            val allGroupsResult = all
            if (allGroupsResult.isFailed) {
                return@withLock allGroupsResult.takeError()
            }

            val rootUid = db.getRawDatabase().rootGroup.uuid.toUuidOrNull()
            OperationResult.success(
                allGroupsResult.obj.filter { group ->
                    group.uid != rootUid && group.matches(query)
                }
            )
        }
    }

    override fun getContentWatcher(): ContentWatcher<Group> = watcher
}