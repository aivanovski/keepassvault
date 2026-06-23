package com.ivanovsky.passnotes.data.repository.keepass.keepassrs

import com.google.protobuf.ByteString
import com.ivanovsky.passnotes.data.entity.Attachment
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.data.entity.PropertyType
import com.ivanovsky.passnotes.domain.entity.PropertyFilter
import com.ivanovsky.passnotes.domain.otp.OtpUriFactory
import com.ivanovsky.passnotes.util.Base64Utils
import com.ivanovsky.passnotes.util.ShaUtils
import com.ivanovsky.passnotes.util.StringUtils.EMPTY
import java.nio.ByteBuffer
import java.util.Date
import java.util.UUID
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.Attachment as ProtoAttachment
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.Entry as ProtoEntry
import com.ivanovsky.passnotes.data.repository.keepass.proto.v1.Times as ProtoTimes

internal fun ProtoEntry.toNote(
    groupUid: UUID,
    attachments: List<ProtoAttachment>
): Note {
    val properties = fieldsList.map { field ->
        val type = determinePropertyType(field.name, field.value)

        Property(
            type = type,
            name = type?.propertyName ?: field.name,
            value = field.value,
            isProtected = field.isProtected
        )
    }

    val attachmentById = attachments.associateBy { attachment -> attachment.id }
    val entryAttachments = attachmentsList.mapNotNull { entryAttachment ->
        val attachment = attachmentById[entryAttachment.attachmentId] ?: return@mapNotNull null
        val data = attachment.data.toByteArray()
        val hash = ShaUtils.sha256(data)

        Attachment(
            uid = Base64Utils.toBase64String(hash.data),
            name = entryAttachment.name,
            hash = hash,
            data = data
        )
    }

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

internal fun ByteString.toUuidOrNull(): UUID? {
    return if (size() == UUID_SIZE_BYTES) {
        toUuidOrThrow()
    } else {
        null
    }
}

internal fun ByteString.toUuidOrThrow(): UUID {
    require(size() == UUID_SIZE_BYTES)

    val buffer = ByteBuffer.wrap(toByteArray())
    return UUID(
        buffer.long,
        buffer.long
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

private fun ProtoTimes.getCreationTime(): Long {
    return when {
        hasCreationEpochMs() -> creationEpochMs
        hasLastModificationEpochMs() -> lastModificationEpochMs
        else -> System.currentTimeMillis()
    }
}

private fun ProtoTimes.getModificationTime(): Long {
    return when {
        hasLastModificationEpochMs() -> lastModificationEpochMs
        hasCreationEpochMs() -> creationEpochMs
        else -> System.currentTimeMillis()
    }
}

private fun ProtoTimes.getExpirationTime(): Long? {
    return if (hasExpires() && expires && hasExpiryEpochMs()) {
        expiryEpochMs
    } else {
        null
    }
}

private const val UUID_SIZE_BYTES = 16
