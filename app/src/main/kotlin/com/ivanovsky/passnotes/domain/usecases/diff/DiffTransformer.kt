package com.ivanovsky.passnotes.domain.usecases.diff

import app.keemobile.kotpass.models.DatabaseElement as KotpassDatabaseElement
import app.keemobile.kotpass.models.Group as KotpassGroup
import com.ivanovsky.passnotes.data.entity.EncryptedDatabaseElement
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.data.repository.keepass.kotpass.KotpassDatabase
import com.ivanovsky.passnotes.domain.usecases.diff.entity.DiffEvent
import com.ivanovsky.passnotes.domain.usecases.diff.entity.DiffListItem
import com.ivanovsky.passnotes.domain.usecases.diff.entity.DiffOriginType
import java.util.LinkedList
import java.util.UUID

class DiffTransformer {

    fun transform(
        lhs: KotpassDatabase,
        rhs: KotpassDatabase,
        diff: List<DiffEvent<EncryptedDatabaseElement>>
    ): List<DiffListItem> {
        val sortedDiff = DiffSorter().sort(diff)

        val lhsDepthMap = lhs.getRawRootGroup().buildDepthMap()
        val rhsDepthMap = rhs.getRawRootGroup().buildDepthMap()

        val eventsByParentMap = groupEventsByParent(sortedDiff)
        val eventsByDepthMap = groupEventsByDepth(
            lhsDepthMap = lhsDepthMap,
            rhsDepthMap = rhsDepthMap,
            eventsByParentMap = eventsByParentMap
        )

        val parentFinder = ParentFinder(lhs, rhs)
        val result = mutableListOf<DiffListItem>()
        val depths = eventsByDepthMap.keys.sorted()

        for (depth in depths) {
            val eventsByDepth = eventsByDepthMap[depth] ?: continue

            for ((parentUuid, eventsByParent) in eventsByDepth) {
                val parents = if (parentUuid != null) {
                    parentFinder.findAllParentsToRoot(
                        firstParentUuid = parentUuid,
                        originType = getOriginTypeForEvents(eventsByParent)
                    )
                        .reversed()
                } else {
                    emptyList()
                }

                val isPropertyUpdate = (eventsByParent.firstOrNull()?.getEntity() is Property)

                if (isPropertyUpdate) {
                    val parentGroups = parents.mapNotNull { parent ->
                        parent.entity as? Group
                    }

                    val note = parents
                        .lastOrNull { parent -> parent.entity is Note }
                        ?.let { parent -> parent.entity as? Note }

                    if (note != null) {
                        @Suppress("UNCHECKED_CAST")
                        val item = DiffListItem.PropertiesItem(
                            parentGroups = parentGroups,
                            note = note,
                            events = eventsByParent as List<DiffEvent.Update<Property>>
                        )

                        result.add(item)
                    }
                } else {
                    for (event in eventsByParent) {
                        when (event.getEntity()) {
                            is Group -> {
                                @Suppress("UNCHECKED_CAST")
                                val item = DiffListItem.GroupItem(
                                    parents = parents.mapNotNull { parent ->
                                        parent.entity as? Group
                                    },
                                    event = event as DiffEvent<Group>
                                )

                                result.add(item)
                            }

                            is Note -> {
                                @Suppress("UNCHECKED_CAST")
                                val item = DiffListItem.NoteItem(
                                    parents = parents.mapNotNull { parent ->
                                        parent.entity as? Group
                                    },
                                    event = event as DiffEvent<Note>
                                )

                                result.add(item)
                            }
                        }
                    }
                }
            }
        }

        return result
    }

    private fun getOriginTypeForEvents(
        events: List<DiffEvent<EncryptedDatabaseElement>>
    ): DiffOriginType {
        return when (events.first()) {
            is DiffEvent.Insert -> DiffOriginType.RIGHT
            is DiffEvent.Delete -> DiffOriginType.LEFT
            is DiffEvent.Update -> DiffOriginType.RIGHT
        }
    }

    private fun groupEventsByParent(
        allEvents: List<DiffEvent<EncryptedDatabaseElement>>
    ): Map<UUID?, List<DiffEvent<EncryptedDatabaseElement>>> {
        val eventsByParentUuidMap =
            HashMap<UUID?, MutableList<DiffEvent<EncryptedDatabaseElement>>>()

        for (event in allEvents) {
            val parentUuid = event.getParentUuid()

            val eventsByParent = eventsByParentUuidMap.getOrDefault(
                parentUuid,
                mutableListOf()
            )

            eventsByParent.add(event)

            eventsByParentUuidMap[parentUuid] = eventsByParent
        }

        return eventsByParentUuidMap
    }

    private fun groupEventsByDepth(
        lhsDepthMap: Map<UUID, Int>,
        rhsDepthMap: Map<UUID, Int>,
        eventsByParentMap: Map<UUID?, List<DiffEvent<EncryptedDatabaseElement>>>
    ): Map<Int, Map<UUID?, List<DiffEvent<EncryptedDatabaseElement>>>> {
        val allEventsByDepth =
            HashMap<Int, MutableMap<UUID?, List<DiffEvent<EncryptedDatabaseElement>>>>()

        for ((parentUuid, eventsByParent) in eventsByParentMap) {
            val depth = getDepth(
                lhsDepthMap = lhsDepthMap,
                rhsDepthMap = rhsDepthMap,
                event = eventsByParent.first()
            )

            val eventsByDepth = allEventsByDepth.getOrDefault(
                depth,
                HashMap()
            )

            eventsByDepth[parentUuid] = eventsByParent
            allEventsByDepth[depth] = eventsByDepth
        }

        return allEventsByDepth
    }

    private fun KotpassGroup.buildDepthMap(): Map<UUID, Int> {
        val depthMap = HashMap<UUID, Int>()

        val nodes = LinkedList<KotpassDatabaseElement>()
        nodes.add(this)

        var level = 0
        while (nodes.isNotEmpty()) {
            repeat(nodes.size) {
                val node = nodes.removeFirst()

                depthMap[node.uuid] = level

                if (node is KotpassGroup) {
                    for (child in node.groups) {
                        nodes.add(child)
                    }
                    for (child in node.entries) {
                        nodes.add(child)
                    }
                }
            }

            level++
        }

        return depthMap
    }

    private fun getDepth(
        lhsDepthMap: Map<UUID, Int>,
        rhsDepthMap: Map<UUID, Int>,
        event: DiffEvent<EncryptedDatabaseElement>
    ): Int {
        val parentUuid = event.getParentUuid()
        val depthMap = event.chooseSourceByEventType(
            lhs = lhsDepthMap,
            rhs = rhsDepthMap
        )

        return depthMap[parentUuid]?.let { depth ->
            depth + 1
        } ?: 0
    }

    private fun <T> DiffEvent<EncryptedDatabaseElement>.chooseSourceByEventType(
        lhs: T,
        rhs: T
    ): T {
        return when (this) {
            is DiffEvent.Delete -> lhs
            is DiffEvent.Insert -> rhs
            is DiffEvent.Update -> rhs
        }
    }

    private fun DiffEvent<*>.getParentUuid(): UUID? {
        return when (this) {
            is DiffEvent.Insert -> parentUuid
            is DiffEvent.Delete -> parentUuid

            // Only for fields, newParentUuid should always match oldParentUuid
            is DiffEvent.Update -> newParentUuid
        }
    }
}