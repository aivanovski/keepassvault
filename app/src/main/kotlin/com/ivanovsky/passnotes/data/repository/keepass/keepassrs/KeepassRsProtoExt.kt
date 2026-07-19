package com.ivanovsky.passnotes.data.repository.keepass.keepassrs

import arrow.core.Either
import arrow.core.raise.either
import com.google.protobuf.ByteString
import com.ivanovsky.passnotes.data.entity.Attachment
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.OperationError.Companion.MESSAGE_FAILED_TO_PARSE_UUID
import com.ivanovsky.passnotes.data.entity.OperationError.Companion.newGenericIOError
import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.data.repository.keepass.determinePropertyType
import com.ivanovsky.passnotes.data.repository.keepass.kotpass.model.InheritableOptions
import com.ivanovsky.passnotes.domain.entity.PropertyFilter
import com.ivanovsky.passnotes.keepassrs.proto.v1.Attachment as RawAttachment
import com.ivanovsky.passnotes.keepassrs.proto.v1.Database as RawDatabase
import com.ivanovsky.passnotes.keepassrs.proto.v1.Entry as RawEntry
import com.ivanovsky.passnotes.keepassrs.proto.v1.EntryAttachment
import com.ivanovsky.passnotes.keepassrs.proto.v1.Group as RawGroup
import com.ivanovsky.passnotes.keepassrs.proto.v1.Times as RawTimes
import com.ivanovsky.passnotes.keepassrs.proto.v1.group
import com.ivanovsky.passnotes.util.Base64Utils
import com.ivanovsky.passnotes.util.ShaUtils
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import java.nio.ByteBuffer
import java.util.Date
import java.util.LinkedList
import java.util.UUID

fun RawDatabase.getAllAttachmentsMap(): Map<Int, RawAttachment> {
    return attachmentsList.associateBy { attachment -> attachment.id }
}

fun <T> RawGroup.collectBFSWithParent(
    transform: (parent: RawGroup, group: RawGroup) -> T
): List<T> {
    val root = this
    val nextGroups = LinkedList<Pair<RawGroup?, RawGroup>>()
        .apply {
            add(null to root)
        }

    val result = mutableListOf<T>()
    while (nextGroups.isNotEmpty()) {
        repeat(nextGroups.size) {
            val (parent, group) = nextGroups.removeFirst()

            if (parent != null) {
                result.add(transform.invoke(parent, group))
            }

            for (childGroup in group.groupsList) {
                nextGroups.add(group to childGroup)
            }
        }
    }

    return result
}

fun <T> RawGroup.collectEntries(
    transform: (group: RawGroup, groupEntries: List<RawEntry>) -> List<T>
): List<T> {
    val root = this

    val result = mutableListOf<T>()
    val nextGroups = LinkedList<RawGroup>()
        .apply {
            add(root)
        }

    while (nextGroups.isNotEmpty()) {
        val currentGroup = nextGroups.removeFirst()
        nextGroups.addAll(currentGroup.groupsList)
        result.addAll(transform.invoke(currentGroup, currentGroup.entriesList))
    }

    return result
}

fun RawGroup.getGroup(
    predicate: (group: RawGroup) -> Boolean
): RawGroup? {
    val root = this

    if (predicate.invoke(root)) {
        return root
    }

    for (childGroup in groupsList) {
        val match = childGroup.getGroup(predicate = predicate)
        if (match != null) {
            return match
        }
    }

    return null
}

fun RawGroup.getEntry(
    predicate: (group: RawGroup, entry: RawEntry) -> Boolean
): RawEntry? {
    val root = this

    val matchedEntry = entriesList.firstOrNull { entry ->
        predicate.invoke(root, entry)
    }
    if (matchedEntry != null) {
        return matchedEntry
    }

    for (childGroup in groupsList) {
        val match = childGroup.getEntry(predicate = predicate)
        if (match != null) {
            return match
        }
    }

    return null
}

fun RawGroup.getEntryAndGroup(
    predicate: (RawEntry) -> Boolean
): Pair<RawGroup, RawEntry>? {
    val matchedEntry = entriesList.firstOrNull(predicate)
    if (matchedEntry != null) {
        return (this to matchedEntry)
    }

    for (childGroup in groupsList) {
        val match = childGroup.getEntryAndGroup(predicate)
        if (match != null) {
            return match
        }
    }

    return null
}

fun RawGroup.convertToGroup(
    parentUid: UUID?,
    options: InheritableOptions
): Either<OperationError, Group> =
    either {
        Group(
            uid = uuid.toUuid().bind(),
            parentUid = parentUid,
            title = name.orEmpty(),
            groupCount = groupsCount,
            noteCount = entriesCount,
            autotypeEnabled = options.autotypeEnabled,
            searchEnabled = options.searchEnabled
        )
    }

fun List<RawEntry>.convertToNotes(
    groupUid: UUID,
    allAttachments: Map<Int, RawAttachment>
): List<Note> {
    return this.map { entry ->
        entry.convertToNote(
            groupUid = groupUid,
            allAttachments = allAttachments
        )
    }
}

fun RawEntry.convertToNote(
    groupUid: UUID,
    allAttachments: Map<Int, RawAttachment>
): Note {
    val properties = fieldsList
        .map { field ->
            val type = determinePropertyType(field.name.orEmpty(), field.value.orEmpty())

            Property(
                type = type,
                name = type?.propertyName ?: field.name,
                value = field.value.orEmpty(),
                isProtected = field.isProtected
            )
        }
        .toMutableList()

    val propertyTypes = properties.map { property -> property.type }
    for (property in Property.DEFAULT_PROPERTIES) {
        if (property.type !in propertyTypes) {
            properties.add(property)
        }
    }

    val entryAttachments = attachmentsList.toAttachments(
        allAttachments = allAttachments
    )

    val title = PropertyFilter.filterTitle(properties)?.value ?: EMPTY
    val expirationTime = times.getExpirationTime()

    return Note(
        uid = uuid.toUuidOrThrow(),
        groupUid = groupUid,
        created = Date(times.getCreationTime()),
        modified = Date(times.getModificationTime()),
        expiration = if (expirationTime != null) Date(expirationTime) else null,
        title = title,
        properties = properties,
        attachments = entryAttachments
    )
}

fun List<EntryAttachment>.toAttachments(
    allAttachments: Map<Int, RawAttachment>
): List<Attachment> {
    return this.mapNotNull { entryAttachment ->
        val attachment = allAttachments[entryAttachment.attachmentId] ?: return@mapNotNull null

        val data = attachment.data.toByteArray()
        val hash = ShaUtils.sha256(data)

        val attachmentId = entryAttachment.attachmentId
        val hashBase64 = Base64Utils.toBase64String(hash.data)
        val uid = "$attachmentId#$hashBase64"

        Attachment(
            uid = uid,
            name = entryAttachment.name,
            hash = hash,
            data = data
        )
    }
}

fun ByteString.toUuidOrNull(): UUID? {
    return if (size() == UUID_SIZE_BYTES) toUuidOrThrow() else null
}

fun ByteString.toUuidOrThrow(): UUID {
    require(size() == UUID_SIZE_BYTES)

    val buffer = ByteBuffer.wrap(toByteArray())
    return UUID(
        buffer.long,
        buffer.long
    )
}

fun ByteString.toUuid(): Either<OperationError, UUID> {
    return Either.catch { toUuidOrThrow() }
        .mapLeft { error -> newGenericIOError(MESSAGE_FAILED_TO_PARSE_UUID, error) }
}

private fun RawTimes.getCreationTime(): Long {
    return when {
        hasCreationEpochMs() -> creationEpochMs
        hasLastModificationEpochMs() -> lastModificationEpochMs
        else -> System.currentTimeMillis()
    }
}

private fun RawTimes.getModificationTime(): Long {
    return when {
        hasLastModificationEpochMs() -> lastModificationEpochMs
        hasCreationEpochMs() -> creationEpochMs
        else -> System.currentTimeMillis()
    }
}

private fun RawTimes.getExpirationTime(): Long? {
    return if (hasExpires() && expires && hasExpiryEpochMs()) {
        expiryEpochMs
    } else {
        null
    }
}

private const val UUID_SIZE_BYTES = 16