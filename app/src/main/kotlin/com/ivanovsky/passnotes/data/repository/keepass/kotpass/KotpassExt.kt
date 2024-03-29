package com.ivanovsky.passnotes.data.repository.keepass.kotpass

import app.keemobile.kotpass.constants.GroupOverride
import app.keemobile.kotpass.cryptography.EncryptedValue
import app.keemobile.kotpass.database.KeePassDatabase
import app.keemobile.kotpass.models.BinaryData
import app.keemobile.kotpass.models.BinaryReference
import app.keemobile.kotpass.models.Entry as RawEntry
import app.keemobile.kotpass.models.EntryFields
import app.keemobile.kotpass.models.EntryValue
import app.keemobile.kotpass.models.Group as RawGroup
import app.keemobile.kotpass.models.TimeData
import com.ivanovsky.passnotes.data.entity.Attachment
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.Hash
import com.ivanovsky.passnotes.data.entity.HashType
import com.ivanovsky.passnotes.data.entity.InheritableBooleanOption
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.data.entity.PropertyType
import com.ivanovsky.passnotes.data.repository.keepass.kotpass.model.InheritableOptions
import com.ivanovsky.passnotes.domain.entity.PropertyFilter
import com.ivanovsky.passnotes.domain.otp.OtpUriFactory
import com.ivanovsky.passnotes.extensions.toByteString
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import java.time.Instant
import java.util.Date
import java.util.LinkedList
import java.util.UUID
import okio.ByteString

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

fun RawEntry.convertToNote(
    groupUid: UUID,
    allBinaries: Map<ByteString, BinaryData>
): Note {
    val properties = mutableListOf<Property>()
    val attachments = mutableListOf<Attachment>()

    for (field in fields.entries) {
        val type = determinePropertyType(field.key, field.value.content)

        properties.add(
            Property(
                type = type,
                name = type?.propertyName ?: field.key,
                value = field.value.content,
                isProtected = field.value is EntryValue.Encrypted
            )
        )
    }

    for (binary in binaries) {
        val data = allBinaries[binary.hash] ?: continue

        attachments.add(
            Attachment(
                uid = binary.hash.base64(),
                name = binary.name,
                hash = Hash(binary.hash.toByteArray(), HashType.SHA_256),
                data = data.getContent()
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
        properties = properties,
        attachments = attachments
    )
}

private fun determinePropertyType(name: String, value: String): PropertyType? {
    val type = PropertyType.getByName(name) ?: return null

    return if (type == PropertyType.OTP) {
        if (OtpUriFactory.parseUri(value) != null) {
            PropertyType.OTP
        } else {
            null
        }
    } else {
        type
    }
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

fun List<RawEntry>.convertToNotes(
    groupUid: UUID,
    allBinaries: Map<ByteString, BinaryData>
): List<Note> {
    return map { it.convertToNote(groupUid, allBinaries) }
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

    val binaries = attachments.map { attachment -> attachment.convertToBinaryReference() }

    return RawEntry(
        uuid = uid ?: throw IllegalStateException(),
        fields = EntryFields(fields),
        times = TimeData(
            creationTime = Instant.ofEpochMilli(created.time),
            lastModificationTime = Instant.ofEpochMilli(modified.time),
            lastAccessTime = null,
            locationChanged = null,
            expiryTime = null
        ),
        binaries = binaries
    )
}

fun Attachment.convertToBinaryReference(): BinaryReference {
    return BinaryReference(
        hash = hash.toByteString(),
        name = name
    )
}

fun Attachment.convertToBinaryData(): BinaryData {
    return BinaryData.Uncompressed(
        memoryProtection = false,
        rawContent = data
    )
}