package com.ivanovsky.passnotes.data.repository.keepass.keepassrs

import com.google.protobuf.ByteString
import com.ivanovsky.passnotes.data.entity.Attachment
import com.ivanovsky.passnotes.data.entity.GroupEntity
import com.ivanovsky.passnotes.data.entity.InheritableBooleanOption
import com.ivanovsky.passnotes.data.entity.Note
import java.nio.ByteBuffer
import java.util.UUID
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.Attachment as ProtoAttachment
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.Entry as ProtoEntry
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.EntryAttachment as ProtoEntryAttachment
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.Field as ProtoField
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.Group as ProtoGroup
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.Times as ProtoTimes

internal fun UUID.toByteString(): ByteString {
    val buffer = ByteBuffer.allocate(UUID_SIZE_BYTES)
    buffer.putLong(mostSignificantBits)
    buffer.putLong(leastSignificantBits)
    return ByteString.copyFrom(buffer.array())
}

internal fun Note.toProtoEntry(attachmentIdProvider: (Attachment) -> Int): ProtoEntry {
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
            attachments.map { attachment ->
                ProtoEntryAttachment.newBuilder()
                    .setName(attachment.name)
                    .setAttachmentId(attachmentIdProvider(attachment))
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
        .build()
}

internal fun Attachment.toProtoAttachment(id: Int): ProtoAttachment {
    return ProtoAttachment.newBuilder()
        .setId(id)
        .setData(ByteString.copyFrom(data))
        .build()
}

internal fun GroupEntity.toProtoGroup(uid: UUID): ProtoGroup {
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
