package com.ivanovsky.passnotes.data.repository.keepass.keepassrs

import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.GroupEntity
import com.ivanovsky.passnotes.data.entity.InheritableBooleanOption
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.encdb.ContentWatcher
import com.ivanovsky.passnotes.data.repository.encdb.dao.GroupDao
import com.ivanovsky.passnotes.domain.entity.exception.Stacktrace
import com.ivanovsky.passnotes.extensions.matches
import java.util.LinkedList
import java.util.UUID
import kotlin.concurrent.withLock
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.Group as ProtoGroup

class KeepassRsGroupDao(
    private val db: KeepassRsDatabase
) : GroupDao {

    private val watcher = ContentWatcher<Group>()

    override fun getAll(): OperationResult<List<Group>> {
        return db.lock.withLock {
            OperationResult.success(
                db.getRawDatabase()
                    .rootGroup
                    .flattenGroups()
                    .map { node ->
                        node.group.toGroup(
                            parentUid = node.parentUid,
                            options = node.group.toInheritableOptions(node.parentOptions)
                        )
                    }
            )
        }
    }

    override fun getRootGroup(): OperationResult<Group> {
        return db.lock.withLock {
            val root = db.getRawDatabase().rootGroup
            OperationResult.success(
                root.toGroup(
                    parentUid = null,
                    options = root.toInheritableOptions(
                        ParentOptions(
                            autotypeEnabled = DEFAULT_ROOT_INHERITABLE_VALUE,
                            searchEnabled = DEFAULT_ROOT_INHERITABLE_VALUE
                        )
                    )
                )
            )
        }
    }

    override fun getChildGroups(parentGroupUid: UUID): OperationResult<List<Group>> {
        return db.lock.withLock {
            val parentNode = db.getRawDatabase()
                .rootGroup
                .flattenGroups()
                .firstOrNull { node -> node.group.uuid.toUuidOrNull() == parentGroupUid }
                ?: return@withLock failedToFindGroup()

            val parentOptions = parentNode.group.toInheritableOptions(parentNode.parentOptions)

            OperationResult.success(
                parentNode.group.groupsList.map { group ->
                    group.toGroup(
                        parentUid = parentGroupUid,
                        options = group.toInheritableOptions(parentOptions)
                    )
                }
            )
        }
    }

    override fun insert(group: GroupEntity): OperationResult<UUID> = unsupportedWriteOperation()

    override fun insert(group: GroupEntity, doCommit: Boolean): OperationResult<UUID> =
        unsupportedWriteOperation()

    override fun remove(groupUid: UUID): OperationResult<Boolean> = unsupportedWriteOperation()

    override fun getGroupByUid(groupUid: UUID): OperationResult<Group> {
        return db.lock.withLock {
            val node = db.getRawDatabase()
                .rootGroup
                .flattenGroups()
                .firstOrNull { node -> node.group.uuid.toUuidOrNull() == groupUid }
                ?: return@withLock failedToFindGroup()

            OperationResult.success(
                node.group.toGroup(
                    parentUid = node.parentUid,
                    options = node.group.toInheritableOptions(node.parentOptions)
                )
            )
        }
    }

    override fun update(group: GroupEntity, doCommit: Boolean): OperationResult<Boolean> =
        unsupportedWriteOperation()

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

    private fun ProtoGroup.toGroup(
        parentUid: UUID?,
        options: ParentOptions
    ): Group {
        return Group(
            uid = uuid.toUuidOrThrow(),
            parentUid = parentUid,
            title = name,
            groupCount = groupsCount,
            noteCount = entriesCount,
            autotypeEnabled = InheritableBooleanOption(
                isEnabled = options.autotypeEnabled,
                isInheritValue = !hasEnableAutotype()
            ),
            searchEnabled = InheritableBooleanOption(
                isEnabled = options.searchEnabled,
                isInheritValue = !hasEnableSearching()
            )
        )
    }

    private fun ProtoGroup.toInheritableOptions(parentOptions: ParentOptions): ParentOptions {
        return ParentOptions(
            autotypeEnabled = if (hasEnableAutotype()) {
                enableAutotype
            } else {
                parentOptions.autotypeEnabled
            },
            searchEnabled = if (hasEnableSearching()) {
                enableSearching
            } else {
                parentOptions.searchEnabled
            }
        )
    }

    private fun ProtoGroup.flattenGroups(): List<GroupNode> {
        val nodes = mutableListOf<GroupNode>()
        val rootOptions = ParentOptions(
            autotypeEnabled = DEFAULT_ROOT_INHERITABLE_VALUE,
            searchEnabled = DEFAULT_ROOT_INHERITABLE_VALUE
        )
        val queue = LinkedList<GroupNode>()
            .apply {
                add(
                    GroupNode(
                        group = this@flattenGroups,
                        parentUid = null,
                        parentOptions = rootOptions
                    )
                )
            }

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            nodes.add(node)

            val options = node.group.toInheritableOptions(node.parentOptions)
            val parentUid = node.group.uuid.toUuidOrThrow()

            for (child in node.group.groupsList) {
                queue.add(
                    GroupNode(
                        group = child,
                        parentUid = parentUid,
                        parentOptions = options
                    )
                )
            }
        }

        return nodes
    }

    private data class GroupNode(
        val group: ProtoGroup,
        val parentUid: UUID?,
        val parentOptions: ParentOptions
    )

    private data class ParentOptions(
        val autotypeEnabled: Boolean,
        val searchEnabled: Boolean
    )

    private companion object {
        const val DEFAULT_ROOT_INHERITABLE_VALUE = true
    }
}

private fun <T> failedToFindGroup(): OperationResult<T> =
    OperationResult.error(
        OperationError.newDbError(
            OperationError.MESSAGE_FAILED_TO_FIND_GROUP,
            Stacktrace()
        )
    )
