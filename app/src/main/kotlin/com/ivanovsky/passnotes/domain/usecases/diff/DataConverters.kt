package com.ivanovsky.passnotes.domain.usecases.diff

import app.keemobile.kotpass.models.Entry
import app.keemobile.kotpass.models.Group as KotpassGroup
import com.github.aivanovski.keepasstreediff.entity.DiffEvent as ExternalDiffEvent
import com.github.aivanovski.keepasstreediff.entity.Entity as DiffEntity
import com.github.aivanovski.keepasstreediff.entity.EntryEntity
import com.github.aivanovski.keepasstreediff.entity.EntryEntity as DiffEntryEntity
import com.github.aivanovski.keepasstreediff.entity.FieldEntity
import com.github.aivanovski.keepasstreediff.entity.FieldEntity as DiffFieldEntity
import com.github.aivanovski.keepasstreediff.entity.GroupEntity as DiffGroupEntity
import com.github.aivanovski.keepasstreediff.entity.MutableNode
import com.github.aivanovski.keepasstreediff.entity.TreeNode
import com.ivanovsky.passnotes.data.entity.EncryptedDatabaseElement
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.InheritableBooleanOption
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.data.entity.PropertyType
import com.ivanovsky.passnotes.data.repository.keepass.kotpass.KotpassDatabase
import com.ivanovsky.passnotes.domain.usecases.diff.entity.DiffEvent
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import java.util.Date
import java.util.LinkedList
import java.util.UUID

fun ExternalDiffEvent<DiffEntity>.toInternalDiffEvent(): DiffEvent<EncryptedDatabaseElement> {
    return when (this) {
        is ExternalDiffEvent.Insert -> {
            DiffEvent.Insert(
                parentUuid = parentUuid,
                entity = entity.toInternalEntity(parentUuid)
            )
        }

        is ExternalDiffEvent.Delete -> {
            DiffEvent.Delete(
                parentUuid = parentUuid,
                entity = entity.toInternalEntity(parentUuid)
            )
        }

        is ExternalDiffEvent.Update -> {
            DiffEvent.Update(
                oldParentUuid = oldParentUuid,
                newParentUuid = newParentUuid,
                oldEntity = oldEntity.toInternalEntity(oldParentUuid),
                newEntity = newEntity.toInternalEntity(newParentUuid)
            )
        }
    }
}

fun DiffEntity.toInternalEntity(parentUid: UUID?): EncryptedDatabaseElement {
    return when (this) {
        is DiffGroupEntity -> this.toGroup(parentUid)
        is DiffEntryEntity -> this.toNote(parentUid!!)
        is DiffFieldEntity -> this.toProperty()
        else -> throw IllegalStateException()
    }
}

fun DiffEntryEntity.toNote(groupUid: UUID): Note {
    val title = fields[PropertyType.TITLE.propertyName]?.value ?: EMPTY

    val properties = fields.values.map { field ->
        val type = PropertyType.getByName(field.name)

        Property(
            type = type,
            name = field.name,
            value = field.value
        )
    }

    return Note(
        uid = uuid,
        title = title,
        groupUid = groupUid,
        created = Date(),
        modified = Date(),
        properties = properties
    )
}

fun DiffGroupEntity.toGroup(parentUid: UUID?): Group {
    val title = fields[PropertyType.TITLE.propertyName]?.value ?: EMPTY

    return Group(
        uid = uuid,
        title = title,
        parentUid = parentUid,
        groupCount = 0,
        noteCount = 0,
        autotypeEnabled = InheritableBooleanOption.ENABLED,
        searchEnabled = InheritableBooleanOption.ENABLED
    )
}

fun DiffFieldEntity.toProperty(): Property {
    val type = PropertyType.getByName(name)

    return Property(
        type = type,
        name = name,
        value = value
    )
}

fun KotpassGroup.toDiffGroupEntity(): DiffGroupEntity {
    return DiffGroupEntity(
        uuid = uuid,
        fields = mapOf(
            PropertyType.TITLE.propertyName to DiffFieldEntity(
                PropertyType.TITLE.propertyName,
                name
            )
        )
    )
}

fun Entry.toDiffEntryEntity(): DiffEntryEntity {
    val fieldsMap = mutableMapOf<String, FieldEntity>()

    for (field in this.fields) {
        fieldsMap[field.key] = FieldEntity(field.key, field.value.content)
    }

    return EntryEntity(
        uuid = uuid,
        fields = fieldsMap
    )
}

fun KotpassDatabase.buildNodeTree(): TreeNode {
    val root = MutableNode(entity = this.getRawRootGroup().toDiffGroupEntity())

    val groups = LinkedList<Pair<MutableNode, KotpassGroup>>()
    groups.add(Pair(root, this.getRawRootGroup()))

    while (groups.isNotEmpty()) {
        val (node, group) = groups.poll()

        for (childGroup in group.groups) {
            val childNode = MutableNode(entity = childGroup.toDiffGroupEntity())

            node.nodes.add(childNode)
            groups.push(Pair(childNode, childGroup))
        }

        for (entry in group.entries) {
            val entryNode = MutableNode(entity = entry.toDiffEntryEntity())
            node.nodes.add(entryNode)
        }
    }

    return root
}