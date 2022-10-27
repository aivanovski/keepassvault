package com.ivanovsky.passnotes.data.repository.keepass.kotpass

import app.keemobile.kotpass.constants.GroupOverride
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.InheritableBooleanOption
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.data.entity.PropertyType
import com.ivanovsky.passnotes.data.repository.keepass.kotpass.model.InheritableOptions
import com.ivanovsky.passnotes.domain.entity.PropertyFilter
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import app.keemobile.kotpass.cryptography.EncryptedValue
import app.keemobile.kotpass.database.KeePassDatabase
import app.keemobile.kotpass.models.EntryValue
import app.keemobile.kotpass.models.TimeData
import java.time.Instant
import java.util.Date
import java.util.LinkedList
import java.util.UUID
import app.keemobile.kotpass.models.Entry as RawEntry
import app.keemobile.kotpass.models.Group as RawGroup

fun GroupOverride.convertToInheritableOption(parentValue: Boolean): InheritableBooleanOption {
    return when (this) {
        GroupOverride.Enabled -> InheritableBooleanOption(
            isEnabled = true,
            isInheritValue = false
        )
        GroupOverride.Disabled -> InheritableBooleanOption(
            isEnabled = false,
            isInheritValue = false
        )
        GroupOverride.Inherit -> InheritableBooleanOption(
            isEnabled = parentValue,
            isInheritValue = true
        )
    }
}

fun KeePassDatabase.getAllGroups(): List<RawGroup> {
    val root = content.group
    val nextGroups = LinkedList<RawGroup>()
        .apply {
            add(root)
        }

    val allGroups = mutableListOf(root)

    while (nextGroups.size > 0) {
        val currentGroup = nextGroups.removeFirst()

        nextGroups.addAll(currentGroup.groups)
        allGroups.addAll(currentGroup.groups)
    }

    return allGroups
}

fun RawGroup.convertToGroup(
    parentGroupUid: UUID?,
    options: InheritableOptions
): Group {
    return Group(
        uid = uuid,
        parentUid = parentGroupUid,
        title = name,
        groupCount = groups.size,
        noteCount = entries.size,
        autotypeEnabled = options.autotypeEnabled,
        searchEnabled = options.searchEnabled
    )
}

fun RawEntry.convertToNote(groupUid: UUID): Note {
    val properties = mutableListOf<Property>()

    for (field in fields.entries) {
        val type = PropertyType.getByName(field.key)

        properties.add(
            Property(
                type = type,
                name = type?.propertyName ?: field.key,
                value = field.value.content,
                isProtected = field.value is EntryValue.Encrypted
            )
        )
    }

    val title = PropertyFilter.filterTitle(properties)?.value ?: EMPTY

    return Note(
        uid = uuid,
        groupUid = groupUid,
        created = Date(getCreationTime()),
        modified = Date(getModificationTime()),
        title = title,
        properties = properties
    )
}

private fun RawEntry.getCreationTime(): Long {
    val created = times?.creationTime
    val modified = times?.lastModificationTime
    return when {
        created != null -> created.toEpochMilli()
        modified != null -> modified.toEpochMilli()
        else -> System.currentTimeMillis()
    }
}

private fun RawEntry.getModificationTime(): Long {
    val created = times?.creationTime
    val modified = times?.lastModificationTime
    return when {
        modified != null -> modified.toEpochMilli()
        created != null -> created.toEpochMilli()
        else -> System.currentTimeMillis()
    }
}

fun List<RawEntry>.convertToNotes(groupUid: UUID): List<Note> {
    return map { it.convertToNote(groupUid) }
}

fun Property.convertToEntryValue(): EntryValue {
    return if (isProtected) {
        val bytes = value?.toByteArray() ?: byteArrayOf()
        val salt = ByteArray(bytes.size, init = { 0 })

        EntryValue.Encrypted(
            EncryptedValue(
                value = bytes,
                salt = salt
            )
        )
    } else {
        EntryValue.Plain(content = value ?: EMPTY)
    }
}

fun Note.convertToEntry(): RawEntry {
    val fields = properties.associate { property ->
        val name = if (property.type != null) {
            property.type.propertyName
        } else {
            property.name
        }
            ?: EMPTY

        Pair(name, property.convertToEntryValue())
    }

    return RawEntry(
        uuid = uid ?: throw IllegalStateException(),
        fields = fields,
        times = TimeData(
            creationTime = Instant.ofEpochMilli(created.time),
            lastModificationTime = Instant.ofEpochMilli(modified.time),
            lastAccessTime = null,
            locationChanged = null,
            expiryTime = null
        )
    )
}