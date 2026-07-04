package com.ivanovsky.passnotes.data.repository.keepass.keepassrs

import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.Entry as ProtoEntry
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.Group as ProtoGroup
import java.util.UUID

// TODO: all functions should return Either

fun ProtoGroup.replaceGroup(targetUid: UUID, replacement: ProtoGroup): ProtoGroup {
    if (uuid.toUuidOrNull() == targetUid) {
        return replacement
    }

    return toBuilder()
        .clearGroups()
        .addAllGroups(groupsList.map { group -> group.replaceGroup(targetUid, replacement) })
        .build()
}

fun ProtoGroup.updateGroup(targetUid: UUID, transform: (ProtoGroup) -> ProtoGroup): ProtoGroup {
    if (uuid.toUuidOrNull() == targetUid) {
        return transform(this)
    }

    return toBuilder()
        .clearGroups()
        .addAllGroups(groupsList.map { group -> group.updateGroup(targetUid, transform) })
        .build()
}

fun ProtoGroup.removeGroup(targetUid: UUID): ProtoGroup {
    return toBuilder()
        .clearGroups()
        .addAllGroups(
            groupsList
                .filterNot { group -> group.uuid.toUuidOrNull() == targetUid }
                .map { group -> group.removeGroup(targetUid) }
        )
        .build()
}

fun ProtoGroup.updateEntry(targetUid: UUID, transform: (ProtoEntry) -> ProtoEntry): ProtoGroup {
    return toBuilder()
        .clearEntries()
        .addAllEntries(
            entriesList.map { entry ->
                if (entry.uuid.toUuidOrNull() == targetUid) transform(entry) else entry
            }
        )
        .clearGroups()
        .addAllGroups(groupsList.map { group -> group.updateEntry(targetUid, transform) })
        .build()
}

fun ProtoGroup.removeEntry(targetUid: UUID): ProtoGroup {
    return toBuilder()
        .clearEntries()
        .addAllEntries(entriesList.filterNot { entry -> entry.uuid.toUuidOrNull() == targetUid })
        .clearGroups()
        .addAllGroups(groupsList.map { group -> group.removeEntry(targetUid) })
        .build()
}