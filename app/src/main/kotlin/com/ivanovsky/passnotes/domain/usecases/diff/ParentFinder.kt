package com.ivanovsky.passnotes.domain.usecases.diff

import com.ivanovsky.passnotes.data.repository.keepass.kotpass.KotpassDatabase
import app.keemobile.kotpass.models.Group as KotpassGroup
import app.keemobile.kotpass.models.Entry as KotpassEntry
import app.keemobile.kotpass.models.DatabaseElement as KotpassDatabaseElement
import app.keemobile.kotpass.models.Group
import com.ivanovsky.passnotes.data.entity.InheritableBooleanOption
import com.ivanovsky.passnotes.data.repository.keepass.kotpass.convertToGroup
import com.ivanovsky.passnotes.data.repository.keepass.kotpass.convertToNote
import com.ivanovsky.passnotes.data.repository.keepass.kotpass.model.InheritableOptions
import com.ivanovsky.passnotes.domain.usecases.diff.entity.DiffOriginType
import com.ivanovsky.passnotes.domain.usecases.diff.entity.Parent
import java.util.LinkedList
import java.util.UUID

class ParentFinder(
    lhs: KotpassDatabase,
    rhs: KotpassDatabase
) {

    private val lhsGroupMap = lhs.buildAllGroupsMap()
    private val rhsGroupMap = rhs.buildAllGroupsMap()

    private val lhsEntryMap = lhs.buildAllEntriesMap()
    private val rhsEntryMap = rhs.buildAllEntriesMap()

    private val lhsUuidToParentMap = lhs.buildUuidToParentMap()
    private val rhsUuidToParentMap = rhs.buildUuidToParentMap()

    fun findAllParentsToRoot(
        firstParentUuid: UUID,
        originType: DiffOriginType
    ): List<Parent> {
        val parents = mutableListOf<Parent>()

        val groupMap = when (originType) {
            DiffOriginType.LEFT -> lhsGroupMap
            DiffOriginType.RIGHT -> rhsGroupMap
        }

        val entryMap = when (originType) {
            DiffOriginType.LEFT -> lhsEntryMap
            DiffOriginType.RIGHT -> rhsEntryMap
        }

        val uuidToParentMap = when (originType) {
            DiffOriginType.LEFT -> lhsUuidToParentMap
            DiffOriginType.RIGHT -> rhsUuidToParentMap
        }

        var firstGroupUuid: UUID? = firstParentUuid
        if (entryMap.containsKey(firstParentUuid)) {
            val entry = entryMap[firstParentUuid]

            firstGroupUuid = if (entry != null) {
                uuidToParentMap[entry.uuid]
            } else {
                null
            }

            if (entry != null && firstGroupUuid != null) {
                parents.add(
                    Parent(
                        firstParentUuid,
                        entry.convertToNote(
                            groupUid = firstGroupUuid,
                            allBinaries = emptyMap()
                        )
                    )
                )
            }
        }

        val parentUuids = if (firstGroupUuid != null) {
            findAllParentsToRoot(
                uuid = firstGroupUuid,
                uuidToParentMap = uuidToParentMap
            )
        } else {
            emptyList()
        }

        for (parentUuid in parentUuids) {
            val group = groupMap[parentUuid]?.let { group ->
                group.convertToGroup(
                    parentGroupUid = uuidToParentMap[group.uuid],
                    options = InheritableOptions(
                        autotypeEnabled = InheritableBooleanOption.ENABLED,
                        searchEnabled = InheritableBooleanOption.ENABLED,
                    )
                )
            }

            parents.add(
                Parent(
                    uuid = parentUuid,
                    entity = group
                )
            )
        }

        return parents
    }

    private fun findAllParentsToRoot(
        uuid: UUID,
        uuidToParentMap: Map<UUID, UUID>
    ): List<UUID> {
        val result = mutableListOf(uuid)

        var current: UUID? = uuid
        while (current != null && uuidToParentMap.containsKey(uuid)) {
            val parent = uuidToParentMap[current]

            if (parent != null) {
                result.add(parent)
            }

            current = parent
        }

        return result
    }

    private fun KotpassDatabase.buildAllGroupsMap(): Map<UUID, Group> {
        return this.traverse { element -> element is Group }
            .map { element -> element as Group }
            .associateBy { group -> group.uuid }
    }

    private fun KotpassDatabase.buildAllEntriesMap(): Map<UUID, KotpassEntry> {
        return this.traverse { element -> element is KotpassEntry }
            .map { element -> element as KotpassEntry }
            .associateBy { entry -> entry.uuid }
    }

    private fun KotpassDatabase.buildUuidToParentMap(): Map<UUID, UUID> {
        val result = HashMap<UUID, UUID>()

        val parentToNodePairs = this.traverseWithParents()
        for ((parent, node) in parentToNodePairs) {
            if (parent == null) continue

            result[node.uuid] = parent.uuid
        }

        return result
    }

    private fun KotpassDatabase.traverse(
        predicate: (element: KotpassDatabaseElement) -> Boolean
    ): List<KotpassDatabaseElement> {
        val stack = LinkedList<KotpassGroup>()
        stack.push(this.getRawRootGroup())

        val result = mutableListOf<KotpassDatabaseElement>()
        while (stack.isNotEmpty()) {
            val group = stack.pop()

            if (predicate.invoke(group)) {
                result.add(group)
            }

            for (entry in group.entries) {
                if (predicate(entry)) {
                    result.add(entry)
                }
            }

            for (childGroup in group.groups) {
                stack.push(childGroup)
            }
        }

        return result
    }

    private fun KotpassDatabase.traverseWithParents():
        List<Pair<KotpassGroup?, KotpassDatabaseElement>> {
        val nodes = LinkedList<Pair<KotpassGroup?, KotpassDatabaseElement>>()
        nodes.add(Pair(null, getRawRootGroup()))

        val result = mutableListOf<Pair<KotpassGroup?, KotpassDatabaseElement>>()
        while (nodes.isNotEmpty()) {
            repeat(nodes.size) {
                val (parent, node) = nodes.removeFirst()
                result.add(Pair(parent, node))

                if (node is KotpassGroup) {
                    for (childNode in node.groups) {
                        nodes.add(Pair(node, childNode))
                    }

                    for (childNode in node.entries) {
                        nodes.add(Pair(node, childNode))
                    }
                }
            }
        }

        return result
    }
}