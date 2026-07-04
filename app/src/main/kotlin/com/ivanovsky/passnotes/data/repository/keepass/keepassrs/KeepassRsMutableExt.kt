package com.ivanovsky.passnotes.data.repository.keepass.keepassrs

import com.google.protobuf.ByteString
import com.ivanovsky.passnotes.data.entity.Attachment
import com.ivanovsky.passnotes.data.entity.GroupEntity
import com.ivanovsky.passnotes.data.entity.Hash
import com.ivanovsky.passnotes.data.entity.InheritableBooleanOption
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.Attachment as ProtoAttachment
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.Database as ProtoDatabase
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.Entry as ProtoEntry
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.EntryAttachment as ProtoEntryAttachment
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.Field as ProtoField
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.Group as ProtoGroup
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.Times as ProtoTimes
import java.nio.ByteBuffer
import java.util.UUID

fun UUID.toByteString(): ByteString {
    val buffer = ByteBuffer.allocate(UUID_SIZE_BYTES)
    buffer.putLong(mostSignificantBits)
    buffer.putLong(leastSignificantBits)
    return ByteString.copyFrom(buffer.array())
}

fun ProtoDatabase.update(block: (source: ProtoDatabase) -> ProtoDatabase): ProtoDatabase {
    return block.invoke(this)
}

fun Note.toProtoEntry(
    attachmentHashToIdMap: Map<Hash, Int>,
    history: List<ProtoEntry> = emptyList()
): ProtoEntry {
    return ProtoEntry.newBuilder()
        .setUuid((uid ?: UUID.randomUUID()).toByteString())
        .setParentGroupUuid(groupUid.toByteString())
        .addAllFields(
            properties.map { property ->
                ProtoField.newBuilder()
                    .setName(property.name ?: property.type?.propertyName.orEmpty())
                    .setValue(property.value.orEmpty())
                    .setIsProtected(property.isProtected)
                    .build()
            }
        )
        .addAllAttachments(
            attachments.mapNotNull { attachment ->
                val attachmentId = attachmentHashToIdMap[attachment.hash]
                    ?: return@mapNotNull null

                ProtoEntryAttachment.newBuilder()
                    .setName(attachment.name)
                    .setAttachmentId(attachmentId)
                    .build()
            }
        )
        .setTimes(
            ProtoTimes.newBuilder()
                .setCreationEpochMs(created.time)
                .setLastModificationEpochMs(modified.time)
                .apply {
                    expiration?.let { expiration ->
                        setExpires(true)
                        setExpiryEpochMs(expiration.time)
                    }
                }
                .build()
        )
        .addAllHistory(history)
        .build()
}

fun Attachment.toProtoAttachment(id: Int): ProtoAttachment {
    return ProtoAttachment.newBuilder()
        .setId(id)
        .setData(ByteString.copyFrom(data))
        .build()
}

fun GroupEntity.toProtoGroup(uid: UUID): ProtoGroup {
    return ProtoGroup.newBuilder()
        .setUuid(uid.toByteString())
        .apply {
            parentUid?.let { parentUid -> setParentUuid(parentUid.toByteString()) }
        }
        .setName(title)
        .applyInheritableOption(autotypeEnabled) { value -> setEnableAutotype(value) }
        .applyInheritableOption(searchEnabled) { value -> setEnableSearching(value) }
        .setTimes(ProtoTimes.newBuilder().build())
        .build()
}

private fun ProtoGroup.Builder.applyInheritableOption(
    option: InheritableBooleanOption,
    setter: ProtoGroup.Builder.(Boolean) -> ProtoGroup.Builder
): ProtoGroup.Builder {
    return if (option.isInheritValue) {
        this
    } else {
        this.setter(option.isEnabled)
    }
}

private const val UUID_SIZE_BYTES = 16